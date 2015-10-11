import sbt.Keys._
import sbt._

organization := "com.olvind"
name := "stringifiers"
scalaVersion := "2.11.7"
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0-M7" % Test
)
