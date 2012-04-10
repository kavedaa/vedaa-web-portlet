package com.vedaadata.web.route.portlet

import com.vedaadata.web.route._
import com.vedaadata.web.view.portlet._
import javax.portlet._
import scala.collection.mutable.ListBuffer
import collection.JavaConversions._

abstract class PortletReq {
  val request: PortletRequest
  val response: PortletResponse
}

class RenderReq(val request: RenderRequest, val response: RenderResponse) extends PortletReq

object RenderReq {
  def apply(request: RenderRequest, response: RenderResponse) = new RenderReq(
    request,
    response)
}

class ActionReq(val request: ActionRequest, val response: ActionResponse) extends PortletReq

object ActionReq {
  def apply(request: ActionRequest, response: ActionResponse) = new ActionReq(
    request,
    response)
}

class RouterPortlet extends GenericPortlet
{
  val renderRouteBuilder = new ListBuffer[PartialFunction[RenderReq, View]]
  val actionRouteBuilder = new ListBuffer[PartialFunction[ActionReq, Unit]]

  var renderDefault: PartialFunction[RenderReq, View] = { case _ =>
    "No valid route was found for this request."
  }

  val actionDefault: PartialFunction[ActionReq, Unit] = { case _ => }

  lazy val renderRoutes = renderRouteBuilder :+ renderDefault reduceLeft { _ orElse _ }
  lazy val actionRoutes = actionRouteBuilder :+ actionDefault reduceLeft { _ orElse _ }

  def render(r: PartialFunction[RenderReq, View]) { r +=: renderRouteBuilder }
  def action(r: PartialFunction[ActionReq, Unit]) { r +=: actionRouteBuilder }

  def default(view: View) { renderDefault = { case _ => view } }

  override def doDispatch(request: RenderRequest, response: RenderResponse) {
    renderRoutes(RenderReq(request, response)) render (request, response)
  }

  override def processAction(request: ActionRequest, response: ActionResponse) {
    request.setCharacterEncoding("UTF-8")
    actionRoutes(ActionReq(request, response))
  }

  object + {
    def unapply(req: RenderReq) = Some(req, req.request)
  }

  object - {
    def unapply(req: RenderReq) = Some(req, req.response)
  }

  object ++ {
    def unapply(req: ActionReq) = Some(req, req.request)
  }

  object -- {
    def unapply(req: ActionReq) = Some(req, req.response)
  }

  object & {
    def unapply[T](x: T) = Some(x, x)
  }

  class Mode(val self: PortletMode) {
    def unapply(req: PortletReq) = req match {
      case req: RenderReq =>
        if (req.request.getPortletMode == self)
          Some(new Params(asScalaMap(req.request.getParameterMap).toMap.asInstanceOf[Map[String, Array[String]]]))
        else None
      case req: ActionReq =>
        if (req.request.getPortletMode == self)
          Some(new Params(asScalaMap(req.request.getParameterMap).toMap.asInstanceOf[Map[String, Array[String]]]))
        else None
    }
  }

  object view extends Mode(PortletMode.VIEW)
  object edit extends Mode(PortletMode.EDIT)
  object help extends Mode(PortletMode.HELP)

  object any {
    def unapply(req: PortletReq) = Some(new Params(asScalaMap(req.request.getParameterMap).toMap.asInstanceOf[Map[String, Array[String]]]))
  }

  class SessionW(self: PortletSession, scope: Int) {
    def apply(s: String) = Option(self.getAttribute(s, scope))
    def update(s: String, x: Any) { self.setAttribute(s, x, scope) }
  }

//  implicit def sessionPimp(session: PortletSession) = new SessionW(session)

  object $ {
    def unapply(request: PortletRequest) = $P.unapply(request)
  }

  object $P {
    def unapply(request: PortletRequest) = {
      Some(new SessionW(request.getPortletSession, PortletSession.PORTLET_SCOPE))
    }
  }

  object $A {
    def unapply(request: PortletRequest) = Some(new SessionW(request.getPortletSession, PortletSession.APPLICATION_SCOPE))
  }

  abstract class SessionData[T](name: String) {
    def init: T
    def unapply(session: SessionW) = session(name) match {
      case Some(x) =>
        println("Found session data as: " + name)
        Some(x.asInstanceOf[T])
      case None =>
        println("Initializing session data as: " + name)
        val data = init
        session(name) = data
        Some(data)
    }
    def update(session: SessionW, data: T) { session(name) = data }
  }

}

object test extends RouterPortlet
{
  default {
    "Hallo"
  }

  render {
    case view ( params ) - response =>
      "Whatever"
  }
}