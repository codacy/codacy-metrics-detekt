name := "codacy-metrics-detekt"

scalaVersion := "2.13.3"

val detektVersion = "1.23.1"

resolvers += Resolver.jcenterRepo

Compile / console / scalacOptions ~= {
  _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
}

libraryDependencies ++= {
  Seq(
    "com.codacy" %% "codacy-metrics-scala-seed" % "0.2.2",
    "io.gitlab.arturbosch.detekt" % "detekt-core" % detektVersion,
    "io.gitlab.arturbosch.detekt" % "detekt-api" % detektVersion,
    "io.gitlab.arturbosch.detekt" % "detekt-rules" % detektVersion,
    "io.gitlab.arturbosch.detekt" % "detekt-cli" % detektVersion,
    "io.gitlab.arturbosch.detekt" % "detekt-generator" % detektVersion
  )
}

enablePlugins(JavaAppPackaging)
