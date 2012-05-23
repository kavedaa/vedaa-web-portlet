package com.vedaadata.web.route.portlet

import com.vedaadata.web.route._
import com.vedaadata.web.view.portlet._
import javax.portlet._
import scala.collection.mutable.ListBuffer
import collection.JavaConversions._

abstract class Context {
  val request: PortletRequest
  val response: PortletResponse
}

case class RenderContext(request: RenderRequest, response: RenderResponse) extends Context

case class ActionContext(request: ActionRequest, response: ActionResponse) extends Context

class RouterPortlet(encoding: String = "UTF-8") extends GenericPortlet with CommonExtractors {
  
  val renderRouteBuilder = new ListBuffer[PartialFunction[RenderContext, View]]
  val actionRouteBuilder = new ListBuffer[PartialFunction[ActionContext, Unit]]

  var renderDefault: PartialFunction[RenderContext, View] = { case _ =>
    "No valid route was found for this request."
  }

  val actionDefault: PartialFunction[ActionContext, Unit] = { case _ => }

  lazy val renderRoutes = renderRouteBuilder :+ renderDefault reduceLeft { _ orElse _ }
  lazy val actionRoutes = actionRouteBuilder :+ actionDefault reduceLeft { _ orElse _ }

  def render(r: PartialFunction[RenderContext, View]) { r +=: renderRouteBuilder }
  def action(r: PartialFunction[ActionContext, Unit]) { r +=: actionRouteBuilder }

  def default(view: View) { renderDefault = { case _ => view } }

  override def doDispatch(request: RenderRequest, response: RenderResponse) {
    implicit val ctx = RenderContext(request, response)
    renderRoutes(ctx) render()
  }

  override def processAction(request: ActionRequest, response: ActionResponse) {
    request.setCharacterEncoding(encoding)
    actionRoutes(ActionContext(request, response))
  }

  class Mode(mode: PortletMode) {
    def unapply(ctx: RenderContext) =
      if (ctx.request.getPortletMode == mode) Some(ctx.request, ctx.response)
      else None      
    def unapply(ctx: ActionContext) =
      if (ctx.request.getPortletMode == mode) Some(ctx.request, ctx.response)
      else None      
  } 
  
  object view extends Mode(PortletMode.VIEW)
  object edit extends Mode(PortletMode.EDIT)
  object help extends Mode(PortletMode.HELP)

  object Params {
    def unapply(request: PortletRequest) =
      Some(new Params(mapAsScalaMap(request.getParameterMap).asInstanceOf[Map[String, Array[String]]]))
  }
  
  class Session(self: PortletSession, scope: Int) {
    def apply(s: String) = Option(self.getAttribute(s, scope))
    def update(s: String, x: Any) { self.setAttribute(s, x, scope) }
  }

  object Session {
    def unapply(request: PortletRequest) = {
      Some(new Session(request.getPortletSession, PortletSession.PORTLET_SCOPE))
    }
  }

  object AppSession {
    def unapply(request: PortletRequest) = Some(new Session(request.getPortletSession, PortletSession.APPLICATION_SCOPE))
  }

  abstract class SessionData[T](name: String) {
    def init: T
    def unapply(session: Session) = session(name) match {
      case Some(x) =>
        println("Found session data as: " + name)
        Some(x.asInstanceOf[T])
      case None =>
        println("Initializing session data as: " + name)
        val data = init
        session(name) = data
        Some(data)
    }
    def update(session: Session, data: T) { session(name) = data }
  }

}
