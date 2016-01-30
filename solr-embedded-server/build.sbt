name := "solr-embedded-server"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.7"

organization := "org.littlewings"

scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature")

updateOptions := updateOptions.value.withCachedResolution(true)

libraryDependencies ++= Seq(
  "org.apache.solr" % "solr-core" % "5.4.1" excludeAll(
    ExclusionRule("org.apache.hadoop"),
    ExclusionRule("org.eclipse.jetty")
    ),
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)
