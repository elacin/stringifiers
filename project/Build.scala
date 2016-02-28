import com.typesafe.sbt.pgp.PgpKeys
import com.typesafe.sbt.pgp.PgpKeys._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport._
import scala.xml.Group

object Build extends sbt.Build {
  type PE = Project => Project

  override val settings = super.settings ++ Seq(
    organization       := "com.olvind",
    scalaVersion       := "2.11.7",
    crossScalaVersions := Seq("2.11.7", "2.10.6"),
    homepage           := Some(url("http://github.com/oyvindberg/stringifiers"))
  )

  val doPublish: PE =
    _.settings(
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

  val preventPublication: PE =
    _.settings(
      publishTo := Some(Resolver.file("Unused transient repository", target.value / "fakepublish")),
      publishArtifact := false,
      publishLocalSigned := (),       // doesn't work
      publishSigned := (),            // doesn't work
      packagedArtifacts := Map.empty)

  val hasNoTests: PE =
    _.settings(
      sbt.Keys.test in Test := (),
      testOnly      in Test := (),
      testQuick     in Test := ()
    )

  val sharedDeps = Def.setting(Seq(
    "org.scalatest" %%% "scalatest" % "3.0.0-M15" % Test
  ))

  val stringifiers = crossProject.in(file("stringifiers"))
    .settings(mavenCentralFrouFrou :_*)
    .settings(
      name                 := "stringifiers",
      description          := "A standardized way of moving types to and from strings",
      libraryDependencies ++= sharedDeps.value,
      manifestSetting
    ).jsSettings(
      scalacOptions       <+= sourceMapTransform
  )

  val jvm = stringifiers.jvm
  val js  = stringifiers.js

  val root = project.aggregate(jvm, js).configure(hasNoTests, preventPublication)
}
