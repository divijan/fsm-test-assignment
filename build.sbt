import com.typesafe.sbt.packager.docker.DockerChmodType
import com.typesafe.sbt.packager.docker.DockerPermissionStrategy
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.dockerPermissionStrategy

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, DockerPlugin)
  .settings(
    name := "fsm-test-assignment",
    version := "1.0.1",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      guice,
      "com.typesafe.play" %% "play-slick" % "5.0.0",
      "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
      "com.h2database" % "h2" % "1.4.199",
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test"
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-Xfatal-warnings"
    ),

    maintainer in Docker := "Yar Ilich <yar.ilich@gmail.com>",
    dockerChmodType := DockerChmodType.UserGroupWriteExecute,
    dockerPermissionStrategy := DockerPermissionStrategy.CopyChown,
    dockerExposedPorts += 9000,
    javaOptions in Universal += "-Dplay.evolutions.db.default.autoApply=true"
  )
