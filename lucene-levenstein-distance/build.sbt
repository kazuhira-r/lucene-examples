name := "lucene-levenstein-distance"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.7"

organization := "org.littlewings"

scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature")

updateOptions := updateOptions.value.withCachedResolution(true)

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-suggest" % "5.3.0",
  "org.scalatest" %% "scalatest" % "2.2.5"
)
