name := "lucene-document-update"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.4"

organization := "org.littlewings"

scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked")

val luceneVersion = "4.7.1"

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-analyzers-kuromoji" % luceneVersion
)
