package com.vedaadata.web.route.portlet

class ExampleRouter extends RouterPortlet {

  render {
    
    case view(Params(params) & Session(session), resp) =>
      "OK"
      
  }
}