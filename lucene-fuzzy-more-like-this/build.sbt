name := "lucene-fuzzy-more-like-this"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.0"

organization := "org.littlewings"

scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature")

incOptions := incOptions.value.withNameHashing(true)

val luceneVersion = "4.8.0"

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-analyzers-kuromoji" % luceneVersion,
  "org.apache.lucene" % "lucene-queries" % luceneVersion
)
