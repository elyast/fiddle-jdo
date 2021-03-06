package org.elyast.orbit.system.tests.scala

import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.Request
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JsonContentHandler extends AbstractHandler {

  override def handle(target: String, baseRequest: Request,
    request: HttpServletRequest, response: HttpServletResponse): Unit = {
    response.setContentType("application/json;charset=utf-8");
    println("Query String: " + baseRequest.getQueryString)
    val headers = baseRequest.getHeaderNames()
    while(headers.hasMoreElements) {
      val headerName = headers.nextElement.asInstanceOf[String]
      val header = baseRequest.getHeader(headerName)
      println("header: " + headerName + "=" + header)
    }
    response.getWriter()
      .print(
        """[{"surname":"Name","class":"org.elyast.orbit.system.tests.Bunny"}]""");
    response.setStatus(HttpServletResponse.SC_OK);
    baseRequest.setHandled(true);
  }
}