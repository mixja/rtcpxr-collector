name := "sipstack"

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "io.sipstack" % "sipstack-netty-codec-sip" % "0.1.1",
  "io.reactivex" % "rxscala_2.11" % "0.23.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.1",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.5.1",
  "io.vertx" % "vertx-core" % "2.1.5",
  "io.vertx" % "lang-scala_2.11" % "1.1.0-M1",
  "joda-time" % "joda-time" % "2.7",
  "org.scalaj" %% "scalaj-http" % "1.1.4"
)

