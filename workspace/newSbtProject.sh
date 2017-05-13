mkdir $1
cd $1
mkdir -p src/main/scala
cat << EOF > src/main/scala/$1.scala
object $1{
def main(args: Array[String]) =
println("Hi!")
}
EOF
cat << EOF > build.sbt
lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.11.8",
      version      := "0.1.0-SNAPSHOT"
    )),
    name:= "$1",
    libraryDependencies += "org.apache.spark" %% "spark-core" % "2.1.0",
    libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.1.0"
  )
EOF
mkdir -p project
cd project
cat << EOF > plugins.sbt
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.1.0")
EOF
cd ../..
