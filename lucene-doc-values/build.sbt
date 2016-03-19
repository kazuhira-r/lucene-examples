name := "lucene-doc-values"

version := "0.0.1-SNAPSHOT"

organization := "org.littlewings"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature")

updateOptions := updateOptions.value.withCachedResolution(true)

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-analyzers-common" % "5.5.0",
  "org.apache.lucene" % "lucene-queryparser" % "5.5.0",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)
