package com.vedaadata.web.portlet

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

import javax.portlet._

import org.apache.commons.fileupload._
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.fileupload.portlet.PortletFileUpload

import com.vedaadata.web._

abstract class PortletCycle {
  val request: PortletRequest
  val response: PortletResponse
}

case class RenderCycle(request: RenderRequest, response: RenderResponse) extends PortletCycle
case class ActionCycle(request: ActionRequest, response: ActionResponse) extends PortletCycle

class RouterPortlet extends GenericPortlet with CommonExtractors {

  private val renderRouteBuilder = new ListBuffer[PartialFunction[RenderCycle, Renderer]]
  private val actionRouteBuilder = new ListBuffer[PartialFunction[ActionCycle, Unit]]

  private var default0: PartialFunction[RenderCycle, Renderer] = {
    case _ => "No valid route was found for this request."
  }

  private var errorHandler: (Exception => Renderer) = (ex: Exception) =>
    <div class="portlet-msg-error">{ ex.getMessage }</div>

  private lazy val renderRoutes = renderRouteBuilder :+ default0 reduceLeft (_ orElse _)
  private lazy val actionRoutes = actionRouteBuilder reduceLeft (_ orElse _)

  /**
   * Defines any number of render routes, usually in the form of `case` statements.
   * An invocation (or several) of this method should be the main part of your portlet (in the constructor).
   */
  def render(r: PartialFunction[RenderCycle, Renderer]) = { r +=: renderRouteBuilder }

  /**
   * Defines any number of action routes, usually in the form of `case` statements.
   */
  def action(r: PartialFunction[ActionCycle, Unit]) = { r +=: actionRouteBuilder }

  /**
   * Defines a default renderer that gets used if none of those defined in `render` matches.
   * (You can of course also define that as the last case in `route`, but
   * using this method makes it more explicit.)
   */
  def default(renderer: Renderer) = { default0 = { case _ => renderer } }

  override protected def render(request: RenderRequest, response: RenderResponse) = {
    val cycle = new RenderCycle(request, response)
    try {
      renderRoutes(cycle) render cycle
    }
    catch {
      case ex: Exception =>
        errorHandler(ex) render cycle
    }
  }

  /**
   * Defines the default encoding for processing action requests as "UTF-8".
   * Override this method to use a different encoding.
   */
  def encoding = "UTF-8"

  override protected def processAction(request: ActionRequest, response: ActionResponse) = {
    request setCharacterEncoding encoding
    actionRoutes(new ActionCycle(request, response))
  }

  protected class Mode(mode: PortletMode) {
    def unapply(c: PortletCycle) = c.request.getPortletMode == mode
  }

  /**
   * Matches a view mode request.
   */
  object view extends Mode(PortletMode.VIEW)

  /**
   * Matches an edit mode request.
   */
  object edit extends Mode(PortletMode.EDIT)

  /**
   * Matches a help mode request.
   */
  object help extends Mode(PortletMode.HELP)

  /**
   * Extracts a RenderRequest or ActionRequest from a cycle.
   */
  object Request {
    def unapply(c: RenderCycle) = Some(c.request)
    def unapply(c: ActionCycle) = Some(c.request)
  }

  /**
   * Extracts a RenderResponse or ActionResponse from a cycle.
   */
  object Response {
    def unapply(c: RenderCycle) = Some(c.response)
    def unapply(c: ActionCycle) = Some(c.response)
  }

  class Parameters private[web] (protected val self: Map[String, Seq[String]])
    extends AbstractParameters {

    def paramsCompanion = Parameters

    lazy val multi = new MultiParameters(self)
  }

  class MultiParameters private[web] (protected val self: Map[String, Seq[String]])
    extends AbstractMultiParameters {

    def paramsCompanion = Parameters
  }

  class MultipartFormdataParameters private[web] (protected val self: Map[String, Seq[String]], protected val fileItems: Seq[FileItem])
    extends AbstractMultipartFormdataParameters {

    def paramsCompanion = MultipartFormdataParameters

    lazy val multi = new MultiParameters(self)    
  }

  /**
   * Extracts request parameters from a cycle.
   */
  object Parameters extends ParametersCompanion {

    def fromRequest(request: PortletRequest) = {
      val parameters = request.getParameterMap.asInstanceOf[java.util.Map[String, Array[String]]].asScala map {
        case (k, v) => (k, v.toSeq)
      }
      new Parameters(parameters.toMap)
    }

    def unapply(c: PortletCycle) =
      Some(fromRequest(c.request))
  }

  /**
   * Extracts request multipart form data parameters from a cycle.
   */
  object MultipartFormdataParameters extends ParametersCompanion {

    def fromRequest(request: ActionRequest) = {

      val items = new PortletFileUpload(new DiskFileItemFactory).parseRequest(request).asScala.toSeq        

      val (formItems, fileItems) = items partition(_.isFormField)

      val params = formItems map { item =>
        item.getFieldName -> item.getString
      }

      val paramsMap = params groupBy { case (name, value) => name } map  { case (name, nameValues) => name -> (nameValues map(_._2)) }

      new MultipartFormdataParameters(paramsMap, fileItems) 
    }

    def unapply(cycle: ActionCycle) =
      Some(fromRequest(cycle.request))
  }


  /**
   * Wraps a PortletSession and pre-specifies a specific scope to use for every
   * getting and setting of attributes. (Instead of specifying the scope
   * at the time of getting and setting, which the raw PortletSession does.)
   */
  class Session(portletSession: PortletSession, scope: Int) {
    def apply(name: String) = Option(portletSession getAttribute (name, scope))
    def update(name: String, value: Any) = { portletSession setAttribute (name, value, scope) }
  }

  /**
   * Extracts a Session with portlet scope from a cycle.
   */
  object Session {
    def unapply(c: PortletCycle) =
      Some(new Session(c.request.getPortletSession, PortletSession.PORTLET_SCOPE))
  }

  /**
   * Extracts a Session with application scope from a cycle.
   */
  object AppSession {
    def unapply(c: PortletCycle) =
      Some(new Session(c.request.getPortletSession, PortletSession.APPLICATION_SCOPE))
  }

  /**
   * Extracts an instance of type T from a named Session attribute.
   * The common use case is to use some mutable T that can be
   * manipulated directly without having to set the attribute again.
   *
   * A concrete object of this class must implement the `init` method.
   */
  abstract class SessionData[T](name: String, debug: Boolean = false) {
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
  }

  def setRenderParameters(ps: (String, Any)*)(implicit response: ActionResponse) = {
    ps foreach {
      case (k, v) =>
        response setRenderParameter (k, v.toString)
    }
  }

}