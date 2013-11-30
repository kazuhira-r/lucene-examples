name := "lucene-filter"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.2"

organization := "littlewings"

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-core" % "4.3.1",
  "org.apache.lucene" % "lucene-analyzers-kuromoji" % "4.3.1",
  "org.apache.lucene" % "lucene-queryparser" % "4.3.1"
)
