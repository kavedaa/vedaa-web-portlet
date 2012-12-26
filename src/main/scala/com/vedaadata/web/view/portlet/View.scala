package com.vedaadata.web.view.portlet

import com.vedaadata.web.route.portlet._
import com.vedaadata.web.view.ViewUtil
import javax.portlet._
import scala.xml._
import collection.JavaConversions._

abstract class View extends ViewUtil {

  def render()(implicit ctx: RenderContext): Unit

  def contextify(link: String)(implicit ctx: RenderContext) =
    if (!link.startsWith("/")) ctx.request.getContextPath + "/" + link
    else link

  def renderURL(ps: (String, Any)*)(implicit ctx: RenderContext): String = renderURL(ps)

  def renderURL(ps1: Seq[(String, Any)], ps2: (String, Any)*)(implicit ctx: RenderContext): String =
    setParams(ctx.response.createRenderURL, ps1 ++ ps2).toString

  def actionURL(ps: (String, Any)*)(implicit ctx: RenderContext): String = actionURL(ps)

  def actionURL(ps1: Seq[(String, Any)], ps2: (String, Any)*)(implicit ctx: RenderContext): String =
    setParams(ctx.response.createActionURL, ps1 ++ ps2).toString

  private def setParams(url: PortletURL, params: Seq[(String, Any)]) = {
    params foreach {
      case (param, value) =>
        url.setParameter(param, value.toString)
    }
    url
  }
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
