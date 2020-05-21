package org.blueskywalker.akka.influxdb

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.util.Date
import java.util.concurrent.TimeUnit

import okhttp3.OkHttpClient
import org.apache.log4j.LogManager
import org.influxdb.InfluxDBFactory
import org.influxdb.dto.{Query, QueryResult}

import scala.jdk.CollectionConverters._


case class Signal(msgId: Int, timestamp: Long, epoch: Int, vlan: String, vin: String,
                  msgName: String, signalName: String, value: Float, date: String)

object InfluxDBQuery extends App {
  lazy val logger = LogManager.getLogger(getClass)
  logger.info(InfluxdbOptions)
  val timeout = InfluxdbOptions.timeout
  val client = new OkHttpClient.Builder()
    .connectTimeout(timeout, TimeUnit.MINUTES)
    .readTimeout(timeout, TimeUnit.MINUTES)
    .writeTimeout(timeout, TimeUnit.MINUTES)
    .retryOnConnectionFailure(true)

  val influxdb = InfluxDBFactory.connect(InfluxdbOptions.url, client)
  val database = influxdb.setDatabase(InfluxdbOptions.db)

  def format(time: Instant): String = DateTimeFormatter.ISO_INSTANT.format(time)

  def retrieveData(path: String, startTime: Instant, endTime: Instant): Unit = {
    val query = s"select * from oneMonth.signals where time >= '${format(startTime)}' and time < '${format(endTime)}'"
    logger.info(query)
    val response = database.query(new Query(query))
    val lst = response.getResults.asScala

    val signals = lst.flatMap { result: QueryResult.Result =>
      if (result.getSeries == null)
        Seq()
      else
        result.getSeries.asScala.flatMap { series: QueryResult.Series =>
          series.getValues.asScala.map { rec: java.util.List[AnyRef] =>
            val instant: Instant = Instant.parse(rec.get(0).toString)
            val milli: Long = instant.toEpochMilli
            val epoch: Int = (milli / 1000).toInt
            val simple = new SimpleDateFormat("yyyy-MM-dd")
            val date = simple.format(Date.from(Instant.ofEpochMilli(milli)))
            val vlan = if (rec.get(6) == null) "none" else rec.get(6).toString
            Signal(rec.get(1).toString.toInt, milli, epoch, vlan, rec.get(5).toString,
              rec.get(2).toString, rec.get(3).toString, rec.get(4).asInstanceOf[Double].toFloat, date)
          }
        }
    }

    val vinPartition = signals.groupBy(_.vin)

    for ((key, value) <- vinPartition) {
      val outputFileName = s"${path}/vin=${key}/influx-backup-${Instant.now().toEpochMilli}.orc"
      OrcFileSinker.sink(outputFileName, value.toSeq)
    }

    influxdb.close()
  }

  val queryDate = s"${InfluxdbOptions.date}T00:00:00Z"
  val outputFolder = s"${OutputOptions.output}/date=${InfluxdbOptions.date}"
  var startTime = Instant.parse(queryDate)
  val nextDay = startTime.plus(1, ChronoUnit.DAYS)
  do {
    val endTime = startTime.plus(InfluxdbOptions.duration, ChronoUnit.SECONDS)
    retrieveData(outputFolder, startTime, endTime)
    startTime = endTime
  } while (startTime.isBefore(nextDay))
}
