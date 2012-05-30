package org.elyast.orbit.system.tests.scala

import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.Request
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class SomeHandler extends AbstractHandler {

  override def handle(target: String, baseRequest: Request,
    request: HttpServletRequest, response: HttpServletResponse): Unit = {
    response.setContentType("application/json;charset=utf-8");

    response.getWriter()
      .println(
        "[{\"surname\":\"Name\",\"class\":\"org.elyast.orbit.system.tests.Bunny\"}]");
    response.setStatus(HttpServletResponse.SC_OK);
    baseRequest.setHandled(true);
  }
}