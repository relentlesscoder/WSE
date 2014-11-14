package edu.nyu.cs.cs2580;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class WebUtil {

	public static void respondWithMsg(HttpExchange exchange, final String message) {
		try {
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
			OutputStream responseBody = exchange.getResponseBody();
			responseBody.write(message.getBytes());
			responseBody.close();
		} catch (Exception e) {
		}
	}

	public static void repondWithHtmlFile(HttpExchange exchange, String filePath) {
		try {
			File file = new File(filePath);
			byte[] bytearray = new byte[(int) file.length()];
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream inputStream = new BufferedInputStream(fis);
			inputStream.read(bytearray, 0, bytearray.length);
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Server", "Java HTTP Search Server");
			responseHeaders.set("Content-Type", "text/html; charset=iso-8859-1");
			responseHeaders.set("Cache-Control", "no-cache");
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytearray.length);
			OutputStream responseBody = exchange.getResponseBody();
			responseBody.write(bytearray, 0, bytearray.length);
			responseBody.close();
		} catch (Exception e) {

		}
	}

	public static String readHtmlTemplate(String filePath) {
		String output = "";

		try {
			BufferedReader reader = new BufferedReader(new FileReader(filePath));

			try {
				StringBuilder sb = new StringBuilder();
				String line = reader.readLine();

				while (line != null) {
					sb.append(line);
					sb.append("\n");
					line = reader.readLine();
				}
				output = sb.toString();
			} catch (Exception e) {
			} finally {
				reader.close();
			}
		} catch (Exception e) {
		}

		return output;
	}

	public static void writeToResponse(HttpExchange exchange, String response) {
		try {
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Server", "Java HTTP Search Server");
			responseHeaders.set("Content-Type", "text/html; charset=iso-8859-1");
			responseHeaders.set("Cache-Control", "no-cache");
			// responseHeaders.set("Status", "HTTP/1.1 200 OK");
			// responseHeaders.set("Content-Length",
			// Integer.toString(queryResponse.length()));
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
			OutputStream responseBody = exchange.getResponseBody();
			responseBody.write(response.getBytes());
			responseBody.close();
		} catch (Exception e) {
		}
	}
}
