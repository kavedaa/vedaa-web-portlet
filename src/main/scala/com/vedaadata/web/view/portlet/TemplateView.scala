package com.vedaadata.web.view.portlet

import com.vedaadata.web.route.portlet._
import com.vedaadata.template._

class TemplateView(beginning: String, end: String)(implicit manager: XhtmlTemplateManager) extends XmlView {
  def xml(implicit ctx: RenderContext) =
    manager.loadContextified(beginning, end)
}

class BinderView(beginning: String, end: String)(implicit val manager: XhtmlTemplateManager)
  extends TemplateView(beginning, end) with ElementBinder with BinderDsl {
    override def xml(implicit ctx: RenderContext) = new LinkContextifier(ctx.request.getContextPath)(bind(super.xml))
}
