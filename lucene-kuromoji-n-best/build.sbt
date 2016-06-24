name := "lucene-kuromoji-n-best"

version := "0.0.1-SNAPSHOT"

organization := "org.littlewings"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature")

updateOptions := updateOptions.value.withCachedResolution(true)

libraryDependencies += "org.apache.lucene" % "lucene-analyzers-kuromoji" % "6.1.0"
