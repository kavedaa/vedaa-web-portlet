name := "vedaa-web-portlet"

version := "1.0"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
	"javax.servlet" % "servlet-api" % "2.5" % "provided",
	"javax.portlet" % "portlet-api" % "1.0" % "provided"
)

unmanagedJars in Compile += Attributed.blank(file("E:/prog/jvm/lib/vedaa-web/target/scala-2.9.1/vedaa-web_2.9.1-1.0.jar"))

unmanagedJars in Compile += Attributed.blank(file("E:/prog/jvm/lib/vedaa-template/target/scala-2.9.1/vedaa-template_2.9.1-1.0.jar"))

unmanagedJars in Compile += Attributed.blank(file("E:/prog/jvm/lib/vedaa-util/target/scala-2.9.1/vedaa-util_2.9.1-1.0.jar"))


