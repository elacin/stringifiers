import com.typesafe.sbt.pgp.PgpKeys
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport._

import scala.xml.Group

object Build extends sbt.Build {
  type PE = Project => Project

  override val settings = super.settings ++ Seq(
    organization       := "com.olvind",
    crossScalaVersions := Seq("2.12.1", "2.11.8", "2.10.6"),
    scalaVersion       := crossScalaVersions.value.head,
    homepage           := Some(url("http://github.com/oyvindberg/stringifiers"))
  )

  val doPublish =
    Seq(
      releaseCrossBuild             := true,
      releasePublishArtifactsAction := PgpKeys.publishSigned.value,
      pomIncludeRepository          := { x => false },
      publishArtifact in Test       := false,
      publishTo                    <<= isSnapshot {
        case true  ⇒ Some("Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
        case false ⇒ Some("Sonatype Nexus Staging"   at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
      }
    )

  val manifestSetting = packageOptions <+= (name, version, organization) map {
    (title, version, vendor) =>
      Package.ManifestAttributes(
        "Created-By" -> "\"Simple\" Build Tool",
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
    homepage := Some(new URL("http://github.com/oyvindberg/stringifiers")),
    startYear := Some(2016),
    licenses := Seq(("Apache 2", new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))),
    pomExtra <<= (pomExtra, name, description) {(pom, name, desc) => pom ++ Group(
      <scm>
        <url>https://github.com/oyvindberg/stringifiers</url>
        <connection>scm:git:git://github.com/oyvindberg/stringifiers.git</connection>
        <developerConnection>scm:git:git@github.com:oyvindberg/stringifiers.git</developerConnection>
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
      val g = s"https://raw.githubusercontent.com/oyvindberg/stringifiers/$v"
      s"-P:scalajs:mapSourceURI:$a->$g/"
    case (false, _) ⇒ ""
  }

  val sharedDeps = Def.setting(Seq(
    "org.scalatest" %%% "scalatest" % "3.0.0" % Test
  ))

  val stringifiers = crossProject.in(file("stringifiers"))
    .settings(doPublish :_*)
    .settings(mavenCentralFrouFrou :_*)
    .settings(
      name                 := "stringifiers",
      description          := "A standardized way of moving types to and from strings",
      libraryDependencies ++= sharedDeps.value,
      manifestSetting
    ).jsSettings(
      scalacOptions       <+= sourceMapTransform
    ).jvmSettings(
      libraryDependencies ++= Seq("io.argonaut" %% "argonaut" % "6.2-RC2" % Test)
  )

  val jvm = stringifiers.jvm
  val js  = stringifiers.js
}
