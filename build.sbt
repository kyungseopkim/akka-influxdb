import Dependencies._

ThisBuild / scalaVersion     := "2.13.2"
ThisBuild / version          := "v0.0.1"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val HadoopVersion = "3.2.1"

lazy val root = (project in file("."))
  .settings(
    name := "akka-influxdb-client",
    libraryDependencies ++= Seq(
      "org.influxdb" % "influxdb-java" % "2.19",
      "org.apache.hadoop" % "hadoop-hdfs" % HadoopVersion,
      "org.apache.hadoop" % "hadoop-hdfs-client" % HadoopVersion,
      "org.apache.hadoop" % "hadoop-aws" % HadoopVersion,
      "org.apache.hadoop" % "hadoop-common" % HadoopVersion,
      "org.apache.orc" % "orc-core" % "1.6.3",
      "org.apache.logging.log4j" % "log4j" % "2.13.3",
      scalaTest % Test
    )
  )

mainClass in (Compile, run) := Some("org.blueskywalker.akka.influxdb.InfluxDBQuery")
mainClass in (Compile, packageBin) := Some("org.blueskywalker.akka.influxdb.InfluxDBQuery")
mainClass in assembly := Some("org.blueskywalker.akka.influxdb.InfluxDBQuery")

assemblyMergeStrategy in assembly := {
  case "META-INF/io.netty.versions.properties" => MergeStrategy.last
  case "mozilla/public-suffix-list.txt" => MergeStrategy.last
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)

}
// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
