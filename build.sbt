import com.typesafe.sbt.packager.docker.Cmd

name := "codacy-detekt-metrics"

scalaVersion := "2.13.3"

lazy val detektVersion = Def.setting("1.14.2")

mappings in Universal ++= {
  (baseDirectory in Compile) map { baseDir: File =>
    val src = baseDir / "docs"
    val dest = "/docs"

    for {
      path <- src.allPaths.get if !path.isDirectory
    } yield path -> path.toString.replaceFirst(src.toString, dest)
  }
}.value

resolvers += Resolver.jcenterRepo

scalacOptions in (Compile, console) ~= {
  _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
}

libraryDependencies ++= {
  Seq(
    "com.codacy" %% "codacy-metrics-scala-seed" % "0.2.2",
    "io.gitlab.arturbosch.detekt" % "detekt-core" % detektVersion.value,
    "io.gitlab.arturbosch.detekt" % "detekt-api" % detektVersion.value,
    "io.gitlab.arturbosch.detekt" % "detekt-rules" % detektVersion.value,
    "io.gitlab.arturbosch.detekt" % "detekt-cli" % detektVersion.value,
    "io.gitlab.arturbosch.detekt" % "detekt-generator" % detektVersion.value
  )
}

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

val dockerUser = "docker"
val dockerGroup = "docker"

daemonUser in Docker := dockerUser

daemonGroup in Docker := dockerGroup

dockerBaseImage := "openjdk:8-jre-alpine"

dockerEntrypoint := Seq(s"/opt/docker/bin/${name.value}")

dockerCommands := dockerCommands.value.flatMap {
  case cmd @ Cmd("WORKDIR", _) => Seq(Cmd("WORKDIR", "/src"))
  case cmd @ Cmd("ADD", _) =>
    Seq(
      Cmd("RUN", s"adduser -u 2004 -D $dockerUser"),
      cmd,
      Cmd("RUN", "mv /opt/docker/docs /docs"),
      Cmd("RUN", "apk --no-cache add bash")
    )
  case other => List(other)
}
