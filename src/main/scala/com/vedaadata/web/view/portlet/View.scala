package com.vedaadata.web.view.portlet

import javax.portlet._
import scala.xml._
import collection.JavaConversions._

abstract class View {
  def render(request: RenderRequest, response: RenderResponse): Unit

  case class ContextPath(path: String)

  object ContextPath {
    def apply(request: RenderRequest): ContextPath =
      ContextPath(request.getContextPath)
  }

  def contextify(link: String)(implicit contextPath: ContextPath) =
    if (!link.startsWith("/")) contextPath.path + "/" + link
    else link
}

abstract class StringView extends View {
  val contentType = "text/html; charset=utf-8"
  def renderString(response: RenderResponse, content: String) {
    response setContentType contentType
    response.getWriter print content
  }
}

class TextView(text: String) extends StringView {
  def render(request: RenderRequest, response: RenderResponse) {
    renderString(response, text)
  }
}

abstract class XmlView extends StringView {
  val prettyPrint = true

  def xml(implicit ctxPath: ContextPath): scala.xml.Elem

  def render(request: RenderRequest, response: RenderResponse) {
    //    println(enumerationAsScalaIterator(request.getResponseContentTypes) map { println } )
    if (prettyPrint) renderPretty(request, response)
    else renderPlain(request, response)
  }

  private def renderPretty(request: RenderRequest, response: RenderResponse) {
    val printer = new PrettyPrinter(1024, 2)
    val sb = new StringBuilder
    printer.format(xml(ContextPath(request)), sb)
    renderString(response, sb toString)
  }

  private def renderPlain(request: RenderRequest, response: RenderResponse) {
    renderString(response, xml(ContextPath(request)) toString)
  }
}

abstract class XhtmlView(wrapperClassName: String) extends XmlView {
  
  def cssFiles: List[String] = Nil
  def jsFiles: List[String] = Nil

  def body(implicit contextPath: ContextPath): scala.xml.Elem

  def xml(implicit contextPath: ContextPath) =
    <div class={ wrapperClassName }>
      {
        cssFiles map { cssFile =>
          <link rel="stylesheet" type="text/css" href={ contextify(cssFile) }/>
        }
      }
      {
        jsFiles map { jsFile =>
          <script src={ contextify(jsFile) }/>
        }
      }
      { body }
    </div>
}

object View {

  implicit def stringToView(s: String) = new TextView(s)

  implicit def elemToView(elem: Elem) = new XhtmlView("") {
    override def cssFiles = List("style.css")
    def body(implicit ctxPath: ContextPath) = elem
  }
}

trait ViewUtil {
  
  def setParams(url: PortletURL, params: Seq[(String, String)]) {
    params foreach {
      case (param, value) =>
        url.setParameter(param, value)
    }
    url    
  }
}

abstract class URL {
  protected def url(response: RenderResponse): PortletURL
  def apply(paramValues: (String, String)*)(implicit response: RenderResponse) = {
    val portletURL = url(response)
    paramValues foreach {
      case (param, value) =>
        portletURL.setParameter(param, value)
    }
    portletURL
  }
}

object RenderURL extends URL {
  protected def url(response: RenderResponse) = response.createRenderURL
}

object ActionURL extends URL {
  protected def url(response: RenderResponse) = response.createActionURL
}
