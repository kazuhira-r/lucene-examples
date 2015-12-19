name := "lucene-wildcard-query-suffix-match"

version := "1.0"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature")

updateOptions := updateOptions.value.withCachedResolution(true)

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-core" % "5.4.0",
  "org.apache.lucene" % "lucene-analyzers-common" % "5.4.0",
  "org.apache.lucene" % "lucene-queryparser" % "5.4.0",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
)
