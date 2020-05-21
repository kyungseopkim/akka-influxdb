FROM openjdk:8 as build

ENV SCALA_VERSION=2.12.4
ENV SCALA_HOME=/usr/share/scala
ENV SBT_VERSION 1.2.8

RUN apt-get update && apt-get install -y build-essential

# Install Scala
## Piping curl directly in tar
RUN \
curl -fsL https://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C /root/ && \
echo >> /root/.bashrc && \
echo "export PATH=~/scala-$SCALA_VERSION/bin:$PATH" >> /root/.bashrc

# Install sbt
RUN \
curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
dpkg -i sbt-$SBT_VERSION.deb && \
rm sbt-$SBT_VERSION.deb && \
apt-get update && \
apt-get install sbt && \
sbt sbtVersion && \
mkdir project && \
echo "scalaVersion := \"${SCALA_VERSION}\"" > build.sbt && \
echo "sbt.version=${SBT_VERSION}" > project/build.properties && \
echo "case object Temp" > Temp.scala && \
sbt compile


WORKDIR /root/influx-client
ADD . .
RUN sbt assembly

FROM openjdk:8u151-jre-alpine
WORKDIR /root
COPY --from=build /root/influx-client/target/scala-2.13/akka-influxdb-client-assembly-v0.0.1.jar /root/

ENTRYPOINT ["java", "-jar", "/root/akka-influxdb-client-assembly-v0.0.1.jar"]






