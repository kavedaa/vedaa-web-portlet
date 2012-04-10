package com.vedaadata.web.binding.portlet

import javax.portlet.PortletRequest
import scala.collection.JavaConversions

case class IdParam(
  id: String,
  name: String,
  value: String)

object IdParamBinder
{
  //  Assumes parameters on the form "name_id"
  val paramPattern = """([A-Za-z0-9-]+)_([A-Za-z0-9-]+)""".r

  def paramNames(request: PortletRequest) =
    JavaConversions.enumerationAsScalaIterator(request.getParameterNames.asInstanceOf[java.util.Enumeration[String]]).toList

  def idParams(request: PortletRequest) =
    paramNames(request) map { paramNameWithId =>
      try {
        val paramPattern(paramName, id) = paramNameWithId
        Some(IdParam(id, paramName, request.getParameter(paramNameWithId)))
      }
      catch {
        case ex => None
      }
    } flatten

  //  returns Map(id -> Map(name -> value))
  def bindPerId(request: PortletRequest): Map[String, Map[String, String]] =
    idParams(request) groupBy { _.id } map { idParamMap =>
      idParamMap._1 -> (idParamMap._2 map { idParam => idParam.name -> idParam.value } toMap)
    }

  //  returns Map(name -> Map(id -> value))
  def bindPerName(request: PortletRequest): Map[String, Map[String, String]] =
    idParams(request) groupBy { _.name } map { idParamMap =>
      idParamMap._1 -> (idParamMap._2 map { idParam => idParam.id -> idParam.value } toMap)
    }
}
