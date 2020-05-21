package org.blueskywalker.akka.influxdb

import java.nio.charset.StandardCharsets

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.hive.ql.exec.vector.{BytesColumnVector, DoubleColumnVector, LongColumnVector, VectorizedRowBatch}
import org.apache.orc.{CompressionKind, OrcFile, TypeDescription, Writer}


object OrcFileSinker {

  def sink(filePath:String, signals: Seq[Signal]) = {
    val conf = new Configuration()
    val schemaDef =
      """struct<
        |  vlan:string,
        |  msgId:int,
        |  timestamp: bigint,
        |  epoch:int,
        |  msgName:string,
        |  signalName:string,
        |  value:float,
        |  date:string,
        |  vin:string>
        |""".stripMargin.replaceAll("\\s","")

    val schema = TypeDescription.fromString(schemaDef)

    val writer: Writer = OrcFile.createWriter(new Path(filePath),
      OrcFile.writerOptions(conf)
        .setSchema(schema)
        .compress(CompressionKind.SNAPPY)
        .encodingStrategy(OrcFile.EncodingStrategy.COMPRESSION)
    )

    val batch:VectorizedRowBatch = schema.createRowBatch

    val vlanVector:BytesColumnVector = batch.cols(0).asInstanceOf[BytesColumnVector]
    val msgIdVector:LongColumnVector = batch.cols(1).asInstanceOf[LongColumnVector]
    val tsVector:LongColumnVector = batch.cols(2).asInstanceOf[LongColumnVector]
    val epochVector:LongColumnVector = batch.cols(3).asInstanceOf[LongColumnVector]
    val msgNameVector:BytesColumnVector = batch.cols(4).asInstanceOf[BytesColumnVector]
    val signalNameVector:BytesColumnVector = batch.cols(5).asInstanceOf[BytesColumnVector]
    val valueVector:DoubleColumnVector = batch.cols(6).asInstanceOf[DoubleColumnVector]
    val dateVector:BytesColumnVector = batch.cols(7).asInstanceOf[BytesColumnVector]
    val vinVector:BytesColumnVector = batch.cols(8).asInstanceOf[BytesColumnVector]

    val encoding = StandardCharsets.UTF_8

    signals.foreach { signal:Signal =>
      val row:Int = batch.size
      vlanVector.setVal(row,signal.vlan.getBytes(encoding))
      msgIdVector.vector(row) = signal.msgId
      tsVector.vector(row) = signal.timestamp
      epochVector.vector(row) = signal.epoch
      msgNameVector.setVal(row, signal.msgName.getBytes(encoding))
      signalNameVector.setVal(row, signal.signalName.getBytes(encoding))
      valueVector.vector(row) = signal.value
      dateVector.setVal(row, signal.date.getBytes(encoding))
      vinVector.setVal(row, signal.vin.getBytes(encoding))

      batch.size += 1
      if (batch.size == batch.getMaxSize) {
        writer.addRowBatch(batch)
        batch.reset
      }
    }

    if (batch.size != 0) {
      writer.addRowBatch(batch)
      batch.reset
    }
    writer.close()
  }
}
