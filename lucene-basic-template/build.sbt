name := "lucene-basic-template"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.6"

organization := "org.littlewings"

updateOptions := updateOptions.value.withCachedResolution(true)

scalacOptions ++= Seq("-Xlint", "-unchecked", "-deprecation", "-feature")

val luceneVersion = "5.0.0"
libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-queryparser" % luceneVersion,
  "org.apache.lucene" % "lucene-analyzers-kuromoji" % luceneVersion
)
