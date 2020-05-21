package org.blueskywalker.akka.influxdb

object OutputOptions {
  val output = sys.env.getOrElse("OUTPUT_PATH","/tmp/backup")
}
