package edu.nyu.cs.cs2580;

import java.io.*;
import java.net.HttpURLConnection;

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
			} else {
				uriPath = mapToServerPath(uriPath);
			}
			File file = new File(uriPath);
			byte[] byteArray = new byte[(int) file.length()];
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream inputStream = new BufferedInputStream(fis);
			inputStream.read(byteArray, 0, byteArray.length);
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Server", "Java HTTP Search Server");
			responseHeaders.set("Content-Type", "text/html; charset=iso-8859-1");
			responseHeaders.set("Cache-Control", "no-cache");
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, byteArray.length);
			OutputStream responseBody = exchange.getResponseBody();
			responseBody.write(byteArray, 0, byteArray.length);
			responseBody.flush();
			responseBody.close();
		} else {
			String message = "Invalid URI path!";
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
			OutputStream responseBody = exchange.getResponseBody();
			responseBody.write(message.getBytes());
			responseBody.flush();
			responseBody.close();
		}
	}

	private static String mapToServerPath(String uriPath) {
		String serverPath = null;
		if (uriPath.startsWith("/")) {
			serverPath = uriPath.substring(1, uriPath.length());
		}
		serverPath = System.getProperty("user.dir") + "/"
		    + serverPath;

		return serverPath;
	}

}
