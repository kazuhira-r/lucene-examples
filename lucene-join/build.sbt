name := "lucene-join"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.2"

organization := "littlewings"

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-analyzers-kuromoji" % "4.4.0",
  "org.apache.lucene" % "lucene-join" % "4.4.0"
)
