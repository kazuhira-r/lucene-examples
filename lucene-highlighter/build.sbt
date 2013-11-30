name := "lucene-highlighter"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.3"

organization := "littlewings"

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-analyzers-kuromoji" % "4.4.0",
  "org.apache.lucene" % "lucene-queryparser" % "4.4.0",
  "org.apache.lucene" % "lucene-highlighter" % "4.4.0"
)
