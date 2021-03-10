name := "vedaa-web-portlet"

organization := "no.vedaadata"

version := "1.5-SNAPSHOT"

scalaVersion := "2.13.4"

libraryDependencies ++= Seq(
	"javax.servlet" % "servlet-api" % "2.5" % "provided",
	"javax.portlet" % "portlet-api" % "1.0" % "provided"
)

libraryDependencies ++= Seq(
	"no.vedaadata" %% "vedaa-web" % "1.5-SNAPSHOT"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)
