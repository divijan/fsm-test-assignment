import com.typesafe.sbt.packager.docker.DockerChmodType
import com.typesafe.sbt.packager.docker.DockerPermissionStrategy
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.dockerPermissionStrategy

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, DockerPlugin, AshScriptPlugin)
  .settings(
    name := "fsm-test-assignment",
    version := "1.2",
    scalaVersion := "2.13.3",
    libraryDependencies ++= Seq(
      guice,
      caffeine,
      "com.typesafe.play" %% "play-slick" % "5.0.0",
      "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
      "com.h2database" % "h2" % "1.4.199",
      "org.postgresql" % "postgresql" % "42.2.18",
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test"
    ),
    dependencyOverrides ++= Seq(
      "org.checkerframework" % "checker-qual" % "3.4.0",
      "com.google.guava" % "guava" % "28.2-jre"
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-Xfatal-warnings"
    ),
    javaOptions in Test += "-Dconfig.file=conf/test.conf",

    maintainer in Docker := "Yar Ilich <yar.ilich@gmail.com>",
    dockerChmodType          := DockerChmodType.UserGroupWriteExecute,
    dockerPermissionStrategy := DockerPermissionStrategy.CopyChown,
    dockerExposedPorts       += 9000,
    dockerBaseImage          := "openjdk:jre-alpine",
    javaOptions in Universal += "-Dplay.evolutions.db.default.autoApply=true"
  )
