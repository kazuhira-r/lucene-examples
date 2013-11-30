name := "lucene-grouping"

version := "0.0.1"

scalaVersion := "2.10.2"

organization := "littlewings"

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-analyzers-kuromoji" % "4.4.0",
  "org.apache.lucene" % "lucene-queryparser" % "4.4.0",
  "org.apache.lucene" % "lucene-grouping" % "4.4.0"
)
