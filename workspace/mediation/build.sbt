import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.11.8",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Mediation",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "org.apache.spark" %% "spark-core" % "2.1.0",
    libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.1.0",
    libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.16.0",
    libraryDependencies += "com.storm-enroute" %% "scalameter-core" % "0.6",
    libraryDependencies += "net.liftweb" %% "lift-json" % "2.6",
    libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.12"
  )
mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x => MergeStrategy.first
  }
}
