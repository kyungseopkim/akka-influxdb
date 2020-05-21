package org.blueskywalker.akka.influxdb

object InfluxdbOptions {
  val url = sys.env.getOrElse("INFLUXDB_URL", "http://localhost:8086")
  val db = sys.env.getOrElse("INFLUXDB_DB","signals")
  val date = sys.env.getOrElse("COLLECT_DATE", "2020-01-01")
  val duration = sys.env.getOrElse("QUERY_SLICE", "10").toInt
  val timeout = sys.env.getOrElse("TIMEOUT", "1").toInt

  override def toString: String = s"Server(${url}),DB($db),DATE($date),DURATION($duration),Timeout($timeout)"
}
