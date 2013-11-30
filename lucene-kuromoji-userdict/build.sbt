name := "lucene-kuromoji-userdict"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.2"

organization := "littlewings"

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-core" % "4.3.0",
  "org.apache.lucene" % "lucene-analyzers-common" % "4.3.0",
  "org.apache.lucene" % "lucene-analyzers-kuromoji" % "4.3.0"
)
