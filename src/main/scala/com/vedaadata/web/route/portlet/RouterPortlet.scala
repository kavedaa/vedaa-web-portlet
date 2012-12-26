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

  def debug = false

  val renderRouteBuilder = new ListBuffer[PartialFunction[RenderContext, View]]
  val actionRouteBuilder = new ListBuffer[PartialFunction[ActionContext, Unit]]

  var renderDefault: PartialFunction[RenderContext, View] = {
    case _ =>
      "No valid route was found for this request."
  }

  val actionDefault: PartialFunction[ActionContext, Unit] = { case _ => }

  lazy val renderRoutes = renderRouteBuilder :+ renderDefault reduceLeft { _ orElse _ }
  lazy val actionRoutes = actionRouteBuilder :+ actionDefault reduceLeft { _ orElse _ }

  def render(r: PartialFunction[RenderContext, View]) { r +=: renderRouteBuilder }
  def action(r: PartialFunction[ActionContext, Unit]) { r +=: actionRouteBuilder }

  def default(view: View) { renderDefault = { case _ => view } }

  override def doDispatch(request: RenderRequest, response: RenderResponse) {
    if (debug) {
      println("Portlet mode: " + request.getPortletMode)
      val params = Params(request)
      println("Parameters: " + params)
    }
    implicit val ctx = RenderContext(request, response)
    renderRoutes(ctx) render ()
  }

  override def processAction(request: ActionRequest, response: ActionResponse) {
    request.setCharacterEncoding(encoding)
    actionRoutes(ActionContext(request, response))
  }

  class Mode(mode: PortletMode) {
    def unapply(ctx: RenderContext) =
      if (ctx.request.getPortletMode == mode) {
        if (debug) println("Render matching mode: " + mode)
        Some(ctx.request, ctx.response)
      }
      else {
        if (debug) println("No mode match.")
        None
      }
    def unapply(ctx: ActionContext) =
      if (ctx.request.getPortletMode == mode) {
        if (debug) println("Action matching mode: " + mode)
        Some(ctx.request, ctx.response)
      }
      else {
        if (debug) println("No mode match.")
        None
      }
  }

  object view extends Mode(PortletMode.VIEW)
  object edit extends Mode(PortletMode.EDIT)
  object help extends Mode(PortletMode.HELP)

  object Params {
    def apply(request: PortletRequest) =
      new Params(mapAsScalaMap(request.getParameterMap).toMap.asInstanceOf[Map[String, Array[String]]])
    def unapply(request: PortletRequest) = {
      if (debug) println("Extracting parameters.")
      Some(apply(request))
    }
      
  }

  class Session(self: PortletSession, scope: Int) {
    def apply(s: String) = Option(self.getAttribute(s, scope))
    def update(s: String, x: Any) { self.setAttribute(s, x, scope) }
  }

  object Session {
    def unapply(request: PortletRequest) = {
      if (debug) println("Extracting portlet session.")
      Some(new Session(request.getPortletSession, PortletSession.PORTLET_SCOPE))
    }
  }

  object AppSession {
    def unapply(request: PortletRequest) = {
      if (debug) println("Extracting app session.")
     Some(new Session(request.getPortletSession, PortletSession.APPLICATION_SCOPE)) 
    }
  }

  abstract class SessionData[T](name: String) {
    def init: T
    def apply(session: Session) = session(name) match {
      case Some(x) =>
        if (debug) println("Found session data as: " + name)
        x.asInstanceOf[T]
      case None =>
        if (debug) println("Initializing session data as: " + name)
        val data = init
        session(name) = data
        data
    }
    def unapply(session: Session) = Some(apply(session))
    def update(session: Session, data: T) { session(name) = data }
  }

  def setRenderParameters(ps: (String, Any)*)(implicit response: ActionResponse) {
    ps foreach { case (k, v) =>
      response setRenderParameter(k, v.toString)
    }
  }
}
