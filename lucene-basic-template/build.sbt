name := "lucene-basic-template"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.3"

organization := "org.littlewings"

scalacOptions ++= Seq("-deprecation")

{
val luceneVersion = "4.6.0"
libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-queryparser" % luceneVersion,
  "org.apache.lucene" % "lucene-analyzers-kuromoji" % luceneVersion
)
}
