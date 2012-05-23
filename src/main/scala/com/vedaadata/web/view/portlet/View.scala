package com.vedaadata.web.view.portlet

import com.vedaadata.web.route.portlet._
import javax.portlet._
import scala.xml._
import collection.JavaConversions._

abstract class View extends ViewUtil {
  
  def render()(implicit ctx: RenderContext): Unit

  def contextify(link: String)(implicit ctx: RenderContext) =
    if (!link.startsWith("/")) ctx.request.getContextPath + "/" + link
    else link
}

abstract class StringView extends View {
  val contentType = "text/html; charset=utf-8"
  def renderString(content: String)(implicit ctx: RenderContext) {
    ctx.response setContentType contentType
    ctx.response.getWriter print content
  }
}

class TextView(text: String) extends StringView {
  def render()(implicit ctx: RenderContext) {
    renderString(text)
  }
}

abstract class XmlView extends StringView {
  val prettyPrint = true

  def xml(implicit ctx: RenderContext): scala.xml.Elem

  def render()(implicit ctx: RenderContext) {
    if (prettyPrint) renderPretty()
    else renderPlain()
  }

  private def renderPretty()(implicit ctx: RenderContext) {
    val printer = new PrettyPrinter(1024, 2)
    val sb = new StringBuilder
    printer.format(xml, sb)
    renderString(sb.toString)
  }

  private def renderPlain()(implicit ctx: RenderContext) {
    renderString(xml.toString)
  }
}

abstract class XhtmlView(wrapperClassName: String) extends XmlView {
  
  def cssFiles: List[String] = Nil
  def jsFiles: List[String] = Nil

  def body(implicit ctx: RenderContext): scala.xml.Elem

  def xml(implicit ctx: RenderContext) =
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
    def body(implicit ctx: RenderContext) = elem
  }
}

trait ViewUtil {
  
  def renderURL(params: (String, String)*)(implicit ctx: RenderContext) =
    setParams(ctx.response.createRenderURL, params).toString
    
  def actionURL(params: (String, String)*)(implicit ctx: RenderContext) =
    setParams(ctx.response.createActionURL, params).toString    

  private def setParams(url: PortletURL, params: Seq[(String, String)]) = {
    params foreach {
      case (param, value) =>
        url.setParameter(param, value)
    }
    url    
  }  
}
