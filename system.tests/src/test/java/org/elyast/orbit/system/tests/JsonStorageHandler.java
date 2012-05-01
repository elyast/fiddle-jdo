package org.elyast.orbit.system.tests;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.google.common.io.CharStreams;

public final class JsonStorageHandler extends AbstractHandler {
	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		DatanucleusTest.LOGGER.debug("Target {}, base {} ", target, baseRequest);
		DatanucleusTest.LOGGER.debug("Content: {}",
				CharStreams.toString(baseRequest.getReader()));
		response.setContentType("application/json;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		if (baseRequest.getMethod().equals("GET")) {
			response.getWriter()
					.println(
							"[{\"surname\":\"Name\",\"class\":\"org.elyast.orbit.system.tests.Bunny\"}]");
		}
	}
}