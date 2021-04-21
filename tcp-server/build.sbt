name := "tcp-server"

version := "0.1"

scalaVersion := "2.13.5"
val akkaVersion = "2.6.14"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.1.2" % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
)