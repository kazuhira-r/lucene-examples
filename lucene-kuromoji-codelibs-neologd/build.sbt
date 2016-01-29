name := "lucene-kuromoji-codelibs-neologd"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.7"

organization := "org.littlewings"

scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature")

updateOptions := updateOptions.value.withCachedResolution(true)

resolvers += "CodeLibs Repository" at "http://maven.codelibs.org/"

libraryDependencies ++= Seq(
  "org.codelibs" % "lucene-analyzers-kuromoji-ipadic-neologd" % "5.3.1-20151231",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)
