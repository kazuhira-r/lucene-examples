name := "lucene-kuromoji-mecab-neologd"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.5"

organization := "org.littlewings"

updateOptions := updateOptions.value.withCachedResolution(true)

scalacOptions ++= Seq("-Xlint", "-unchecked", "-deprecation", "-feature")

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-core" % "5.0.0",
  "org.apache.lucene" % "lucene-analyzers-common" % "5.0.0"
  // "org.apache.lucene" % "lucene-analyzers-kuromoji" % "5.0.0"
)
