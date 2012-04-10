package com.vedaadata.web.view.portlet

import com.vedaadata.template._
import com.vedaadata.web.view.ViewUtil

class TemplateView(beginning: String, end: String)(implicit manager: XhtmlTemplateManager) extends XmlView {
  def xml(implicit ctxPath: ContextPath) =
    manager.loadContextified(beginning, end)
}

class BinderView(beginning: String, end: String)(implicit val manager: XhtmlTemplateManager)
  extends TemplateView(beginning, end) with ElementBinder with BinderDsl with ViewUtil {
    override def xml(implicit ctxPath: ContextPath) = new LinkContextifier(ctxPath.path)(bind(super.xml))
}
