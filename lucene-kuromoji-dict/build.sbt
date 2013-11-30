name := "lucene-kuromoji-dict"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.3"

organization := "littlewings"

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-core" % "4.5.1",
  "org.apache.lucene" % "lucene-analyzers-common" % "4.5.1"
)
