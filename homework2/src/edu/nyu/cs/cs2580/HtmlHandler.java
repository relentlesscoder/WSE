package edu.nyu.cs.cs2580;

import java.io.IOException;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import edu.nyu.cs.cs2580.SearchEngine.Options;

public class HtmlHandler implements HttpHandler {

	private Options _options;

	public HtmlHandler(Options options) {
		_options = options;
	}

	public void handle(HttpExchange exchange) throws IOException {
		String requestMethod = exchange.getRequestMethod();
		if (!requestMethod.equalsIgnoreCase("GET")) { // GET requests only.
			return;
		}

		// Print the user request header.
		Headers requestHeaders = exchange.getRequestHeaders();
		System.out.print("Incoming request: ");
		for (String key : requestHeaders.keySet()) {
			System.out.print(key + ":" + requestHeaders.get(key) + "; ");
		}
		System.out.println();

		// Validate the incoming request.
		String uriPath = exchange.getRequestURI().getPath();
		if (uriPath != null && !uriPath.isEmpty()) {
			if (uriPath.equals("/home")) {
				uriPath = mapToServerPath(_options._searchTemplate);
				WebUtil.repondWithHtmlFile(exchange, uriPath);
			} else {
				uriPath = mapToServerPath(uriPath);
				WebUtil.repondWithHtmlFile(exchange, uriPath);
			}
		} else {
			WebUtil.respondWithMsg(exchange, "Invalid URI path!");
		}
	}

	private static String mapToServerPath(String uriPath) {
		String serverPath = null;
		if (uriPath.startsWith("/")) {
			serverPath = uriPath.substring(1, uriPath.length());
		}
		serverPath = System.getProperty("user.dir") + "\\"
		    + serverPath.replace('/', '\\');

		return serverPath;
	}

}
