name := "lucene-ngram"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.2"

organization := "littlewings"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.2",
  "org.apache.lucene" % "lucene-analyzers-common" % "4.4.0",
  "org.apache.lucene" % "lucene-analyzers-kuromoji" % "4.4.0",
  "org.apache.lucene" % "lucene-queryparser" % "4.4.0"
)
