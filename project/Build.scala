import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

import scala.xml.Group

object Build extends sbt.Build {
  val buildSettings = Defaults.coreDefaultSettings ++ Seq(
    organization         := "com.olvind",
    crossScalaVersions   := Seq("2.10.5", "2.11.7"),
    scalaVersion         := "2.11.7",
    homepage             := Some(url("http://github.com/elacin/stringifiers")),
    publishTo           <<= isSnapshot {
      case true  ⇒ Some("Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
      case false ⇒ Some("Sonatype Nexus Staging"   at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
    },
    pomIncludeRepository    := { x => false },
    publishArtifact in Test := false
  )

  val manifestSetting = packageOptions <+= (name, version, organization) map {
    (title, version, vendor) =>
      Package.ManifestAttributes(
        "Created-By" -> "Simple Build Tool",
        "Built-By" -> System.getProperty("user.name"),
        "Build-Jdk" -> System.getProperty("java.version"),
        "Specification-Title" -> title,
        "Specification-Version" -> version,
        "Specification-Vendor" -> vendor,
        "Implementation-Title" -> title,
        "Implementation-Version" -> version,
        "Implementation-Vendor-Id" -> vendor,
        "Implementation-Vendor" -> vendor
      )
  }

  // Things we care about primarily because Maven Central demands them
  val mavenCentralFrouFrou = Seq(
    homepage := Some(new URL("http://github.com/elacin/stringifiers")),
    startYear := Some(2015),
    licenses := Seq(("Apache 2", new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))),
    pomExtra <<= (pomExtra, name, description) {(pom, name, desc) => pom ++ Group(
      <scm>
        <url>https://github.com/elacin/stringifiers</url>
        <connection>scm:git:git://github.com/elacin/stringifiers.git</connection>
        <developerConnection>scm:git:git@github.com:elacin/stringifiers.git</developerConnection>
      </scm>
      <developers>
        <developer>
          <id>oyvindberg</id>
          <name>Øyvind Raddum Berg</name>
          <url>http://twitter.com/olvindberg</url>
        </developer>
      </developers>
    )}
  )

  val sourceMapTransform = (isSnapshot, version) map {
    case (true, v) ⇒
      val a = new java.io.File("").toURI.toString.replaceFirst("/$", "")
      val g = s"https://raw.githubusercontent.com/elacin/stringifiers/$v"
      s"-P:scalajs:mapSourceURI:$a->$g/"
    case (false, _) ⇒ ""
  }

  val stringifiers = crossProject.in(file("."))
    .settings(buildSettings ++ mavenCentralFrouFrou :_*)
    .settings(
      name                 := "stringifiers",
      description          := "A standardized way of moving types to and from strings",
      libraryDependencies  += "org.scalatest" %%% "scalatest" % "3.0.0-M7" % "test",
      manifestSetting
    ).jsSettings(
    scalacOptions       <+= sourceMapTransform
  )

  val jvm = stringifiers.jvm
  val js  = stringifiers.js

  val root = project.aggregate(jvm, js).settings(publish := {}, publishLocal := {})
}
