lazy val providedDependencies = Seq(
  "org.apache.spark" %% "spark-core" % "2.1.0",
  "org.apache.spark" %% "spark-sql" % "2.1.0",
  "org.apache.hadoop" % "hadoop-aws" % "2.7.0"
)

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "intowow",
      scalaVersion := "2.11.8",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "S3Partition",
    libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.16.0",
    libraryDependencies += "com.storm-enroute" %% "scalameter-core" % "0.6",
    libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.12",
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.4.0-M3",
    libraryDependencies += "com.amazonaws.scala" % "aws-scala-sdk-cloudwatch" % "1.10.7",
    libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0",
    libraryDependencies ++= providedDependencies.map(_ % "provided")
  )

lazy val mainRunner = project.in(file("mainRunner")).dependsOn(RootProject(file("."))).settings(
  libraryDependencies ++= providedDependencies.map(_ % "compile")
)

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf"                            => MergeStrategy.concat
  case "unwanted.txt"                                => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
