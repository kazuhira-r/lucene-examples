name := "lucene-kuromoji-viterbi"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.7"

organization := "org.littlewings"

scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature")

updateOptions := updateOptions.value.withCachedResolution(true)

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-analyzers-common" % "5.2.1",
  "org.apache.lucene" % "lucene-analyzers-kuromoji" % "5.2.1"
)
