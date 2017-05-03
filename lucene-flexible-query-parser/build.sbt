name := "lucene-flexible-query-parser"

version := "1.0"

scalaVersion := "2.12.2"

organization := "org.littlewings"

scalacOptions ++= Seq("-Xlint", "-unchecked", "-deprecation", "-feature")

updateOptions := updateOptions.value.withCachedResolution(true)

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-queryparser" % "6.5.1" % Compile,
  "org.apache.lucene" % "lucene-analyzers-kuromoji" % "6.5.1" % Compile,
  "org.scalatest" %% "scalatest" % "3.0.3" % Test
)
