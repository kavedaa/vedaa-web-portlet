# vedaa-web-portlet

vedaa-web-portlet is the portlet part of the vedaa-web framework. It shares most of it features with the [servlet part](https://github.com/kavedaa/vedaa-web/tree/1.5), so it is recommended you read the documentation for that first.

Note that so far only Portlets 1.0 (JSR-168) is supported.

## Getting started

Clone the project from GitHub and publish it to your local repository by running the [SBT](http://www.scala-sbt.org/) command `publish-local` on it.

To use the framework in your SBT-based project, add the following dependency to your `.sbt` build file:

```scala
libraryDependencies += "no.vedaadata" %% "vedaa-web-portlet" % "1.5-SNAPSHOT"
```
You also need the Portlet API:

```scala
libraryDependencies +=	"javax.portlet" % "portlet-api" % "1.0" % "provided"
```
To package a web application with SBT you also need the [xsbt-web-plugin](https://github.com/JamesEarlDouglas/xsbt-web-plugin).

See the [example applications](https://github.com/kavedaa/vedaa-web-portlet-examples/tree/master/portlet-examples-applications) for a complete build file setup.

## Usage

Using the portlet framework, you will create one instance of `RouterPortlet` for every portlet in your application. The configuration of `portlet.xml` and possibly other portal-specific configuration files will be exactly as for traditional portlets.

Here is a simple implementation of a portlet:

```scala
class MyPortlet extends RouterPortlet {

  render {
    
    case _ => "We got a render request" 
  }
  
  action {
    
    case _ => println("We got an action request")
  }
}
```

### Routing

The portlet framework uses the same pattern matching and extractors approach as the servlet counterpart. However, for portlets, the routing is much simpler. The only thing you can match on (except for using your own custom extractors) is *portlet mode*:

```scala
render {
    
    case view() => "We got view mode"
      
    case edit() => "We got edit mode"
      
    case help() => "We got help mode"
  }
```

### Renderers

The portlet counterpart to the servlet framework's `Servicer` is the `Renderer` class. Each  `case` expression in a `render` block must evaluate to an instance of this class:

```scala
abstract class Renderer {
  def render(implicit c: RenderCycle)
}
```

Minimal example:

```scala
  render {

    case _ => new Renderer {
      def render(implicit c: RenderCycle) {
        c.response setContentType "text/html"
        c.response.getWriter print "Hello world"
      }
    }
  }
```
Similarly to servicers, some useful predefined renderes are available.

### Parameters and session data

Accessing parameters works exactly as for servlets.

Accessing session data works similarly, with the exception that there is an additional extractor `AppSession` for extracting the session using the application scope instead of the normal portlet scope.



