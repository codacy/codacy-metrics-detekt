import com.typesafe.sbt.packager.docker.Cmd

name := "codacy-metrics-detekt"

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
