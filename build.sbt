ThisBuild / scalaVersion := "2.13.3"
libraryDependencies ++= Seq(
  compilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
  compilerPlugin("org.augustjune" %% "context-applied" % "0.1.4"),
  "org.typelevel" %% "cats-core" % "2.2.0",
  "org.typelevel" %% "cats-effect" % "2.2.0",
  "dev.profunktor" %% "console4cats" % "0.8.1",
  "org.manatki" %% "derevo-cats" % "0.10.5",
  "org.manatki" %% "derevo-cats-tagless" % "0.10.5",
  "co.fs2" %% "fs2-core" % "2.4.4",
  "com.olegpy" %% "meow-mtl-core" % "0.4.0",
  "com.olegpy" %% "meow-mtl-effects" % "0.4.0",
  "io.estatico" %% "newtype" % "0.4.3",
  "eu.timepit" %% "refined" % "0.9.12",
  "com.github.julien-truffaut" %% "monocle-core" % "2.0.1",
  "com.github.julien-truffaut" %% "monocle-macro" % "2.0.1"
)
scalacOptions += "-Ymacro-annotations"
