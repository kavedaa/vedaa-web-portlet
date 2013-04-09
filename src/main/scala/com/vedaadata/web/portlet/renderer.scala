package com.vedaadata.web.portlet

import javax.portlet._
import scala.xml.PrettyPrinter

abstract class Renderer {

  def render(implicit c: RenderCycle)

  def contextify(link: String)(implicit c: RenderCycle) =
    if (!link.startsWith("/")) c.request.getContextPath + "/" + link
    else link
    
  def renderURL(ps: (String, Any)*)(implicit c: RenderCycle): String = renderURL(ps)

  def renderURL(ps1: Seq[(String, Any)], ps2: (String, Any)*)(implicit c: RenderCycle): String =
    setParams(c.response.createRenderURL, ps1 ++ ps2).toString

  def actionURL(ps: (String, Any)*)(implicit c: RenderCycle): String = actionURL(ps)

  def actionURL(ps1: Seq[(String, Any)], ps2: (String, Any)*)(implicit c: RenderCycle): String =
    setParams(c.response.createActionURL, ps1 ++ ps2).toString

  private def setParams(url: PortletURL, params: Seq[(String, Any)]) = {
    params foreach {
      case (param, value) =>
        url.setParameter(param, value.toString)
    }
    url
  }
    
}

class TextRenderer(text: String) extends Renderer {
  def render(implicit c: RenderCycle) {
//    import collection.JavaConversions._
//    c.request.getResponseContentTypes foreach println
    //	this is the only content type supported...still it must be set...
    c.response setContentType "text/html; charset=utf-8"
    c.response.getWriter print text
  }
}

abstract class XmlRenderer extends Renderer {

  def contentType: String

  def xml(implicit c: RenderCycle): scala.xml.Elem

  def prettyPrint = true

  def render(implicit c: RenderCycle) {
    if (prettyPrint) renderPretty
    else renderPlain
  }

  private def renderPretty(implicit c: RenderCycle) {
    val printer = new PrettyPrinter(1024, 2)
    val sb = new StringBuilder
    printer format (xml, sb)
    renderString(sb.toString)
  }

  private def renderPlain(implicit c: RenderCycle) {
    renderString(xml.toString)
  }

  private def renderString(content: String)(implicit c: RenderCycle) {
    c.response setContentType contentType
    c.response.getWriter print content
  }
}

abstract class SimpleXhtml extends XmlRenderer {

  def contentType = "text/html"
  
  def cssFiles: List[String] = Nil
  def jsFiles: List[String] = Nil

  def wrapperId = ""
  def wrapperStyleClass = ""

  def fragment(implicit c: RenderCycle): scala.xml.Elem

  def xml(implicit c: RenderCycle) =
    <div id={ wrapperId } class={ wrapperStyleClass }>
      {
        cssFiles.reverse map { cssFile =>
          <link rel="stylesheet" type="text/css" href={ contextify(cssFile) }/>
        }
      }
      {
        jsFiles.reverse map { jsFile =>
          <script src={ contextify(jsFile) }></script>
        }
      }
      { fragment }
    </div>
}

/**
 * Convenient SimpleXhtml factories.
 */
object SimpleXhtml {

  def apply(fragment0: scala.xml.Elem): SimpleXhtml =
    new SimpleXhtml {
      def fragment(implicit c: RenderCycle) = fragment
    }
}


object Renderer {
  implicit def textRenderer(s: String) = new TextRenderer(s)
  implicit def xmlRenderer(xml0: scala.xml.Elem) = new XmlRenderer {
    def contentType = "text/html"
    def xml(implicit c: RenderCycle) = xml0
  }
}


