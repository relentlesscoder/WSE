package edu.nyu.cs.cs2580;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Vector;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * Handles each incoming query, students do not need to change this class except
 * to provide more query time CGI arguments and the HTML output.
 * <p/>
 * N.B. This class is not thread-safe.
 * 
 * @author congyu
 * @author fdiaz
 */
class QueryHandler implements HttpHandler {

	// For accessing the underlying documents to be used by the Ranker. Since
	// we are not worried about thread-safety here, the Indexer class must take
	// care of thread-safety.
	private Indexer _indexer;

	private static final String HTML_HEADER = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\r\n<html>\r\n<head>\r\n<title>Web Search Engine</title>\r\n<style type=\"text/css\">\r\n.doc_title { font-size: 1.2em; font-weight: bold; }\r\n</style>\r\n</head>\r\n<body>\r\n";
	private static final String HTML_FOOTER = "</body>\r\n</html>";

	public QueryHandler(Options options, Indexer indexer) {
		_indexer = indexer;
	}

	private void respondWithMsg(HttpExchange exchange, final String message)
	    throws IOException {
		Headers responseHeaders = exchange.getResponseHeaders();
		responseHeaders.set("Content-Type", "text/plain");
		exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
		OutputStream responseBody = exchange.getResponseBody();
		responseBody.write(message.getBytes());
		responseBody.close();
	}

	private void repondWithHtmlFile(HttpExchange exchange, String dataPath) {
		int index = dataPath.indexOf("/data/");
		if (index != -1) {
			dataPath = dataPath.substring(index + 1, dataPath.length());
			String filePath = System.getProperty("user.dir") + "\\"
			    + dataPath.replace('/', '\\');
			try {
				File file = new File(filePath);
				byte[] bytearray = new byte[(int) file.length()];
				FileInputStream fis = new FileInputStream(file);
				BufferedInputStream bis = new BufferedInputStream(fis);
				bis.read(bytearray, 0, bytearray.length);
				Headers responseHeaders = exchange.getResponseHeaders();
				responseHeaders.set("Server", "Java HTTP Search Server");
				responseHeaders.set("Content-Type", "text/html; charset=iso-8859-1");
				responseHeaders.set("Cache-Control", "no-cache");
				// responseHeaders.set("Status", "HTTP/1.1 200 OK");
				// responseHeaders.set("Content-Length",
				// Integer.toString(queryResponse.length()));
				exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK,
				    bytearray.length);
				OutputStream responseBody = exchange.getResponseBody();
				responseBody.write(bytearray, 0, bytearray.length);
				responseBody.close();
			} catch (Exception e) {

			}
		}
	}

	private void constructTextOutput(final Vector<ScoredDocument> docs,
	    StringBuffer response) {
		for (ScoredDocument doc : docs) {
			response.append(response.length() > 0 ? "\n" : "");
			response.append(doc.asTextResult());
		}
		response.append(response.length() > 0 ? "\n" : "");
	}

	private String constructorHtmlOutput(String queryText,
	    Vector<ScoredDocument> scoredDocuments) {
		StringBuilder output = new StringBuilder();
		output.append("<div id=\"title\"><h1>Your search for term ");
		output.append(queryText);
		output.append(" returns " + Integer.toString(scoredDocuments.size())
		    + " documents.</h1></div>\r\n");
		output.append("<div id=\"divSearchContainer\">\r\n");
		output.append("<ul id=\"unorderedList\">\r\n");
		for (ScoredDocument scoredDocument : scoredDocuments) {
			output.append("<li class=\"divDocument" + "\">\r\n");
			output.append("<a href=\"" + scoredDocument.getServerUrl()
			    + "\" class=\"doc_title\">" + scoredDocument.getTitle() + "</a>\r\n");
			output.append("<p class=\"score\">" + scoredDocument.getScore()
			    + "</p>\r\n");
			output.append("</li>\r\n");
		}
		output.append("</ul>\r\n");
		output.append("</div>\r\n");
		return output.toString();
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
		String uriQuery = exchange.getRequestURI().getQuery();
		String uriPath = exchange.getRequestURI().getPath();
		if (uriPath == null || uriQuery == null) {
			if (exchange.getRequestURI().toString().contains("/data/")) {
				repondWithHtmlFile(exchange, uriPath);
			} else {
				respondWithMsg(exchange, "Something wrong with the URI!");
			}
		}
		if (!uriPath.equals("/search")) {
			respondWithMsg(exchange, "Only /search is handled!");
		}
		System.out.println("Query: " + uriQuery);

		// Process the CGI arguments.
		CgiArguments cgiArgs = new CgiArguments(uriQuery);
		if (cgiArgs._query.isEmpty()) {
			respondWithMsg(exchange, "No query is given!");
		}

		// Create the ranker.
		Ranker ranker = Ranker.Factory.getRankerByArguments(cgiArgs,
		    SearchEngine.OPTIONS, _indexer);
		if (ranker == null) {
			respondWithMsg(exchange, "Ranker " + cgiArgs._rankerType.toString()
			    + " is not valid!");
		}

		// Processing the query.
		Query processedQuery;
		if (cgiArgs._query.matches(".*(\".+\").*")) {
			processedQuery = new QueryPhrase(cgiArgs._query, true);
		} else {
			processedQuery = new QueryPhrase(cgiArgs._query, false);
		}
		processedQuery.processQuery();

		if (processedQuery.terms == null || processedQuery.terms.size() < 0) {
			respondWithMsg(exchange, "Invalid query text!");
		}

		// Ranking.
		Vector<ScoredDocument> scoredDocs = ranker.runQuery(processedQuery,
		    cgiArgs._numResults);
		StringBuffer response = new StringBuffer();
		switch (cgiArgs._outputFormat) {
		case TEXT:
			constructTextOutput(scoredDocs, response);
			respondWithMsg(exchange, response.toString());
			break;
		case HTML:
			String queryResponse = "";
			queryResponse += HTML_HEADER;
			queryResponse += constructorHtmlOutput(cgiArgs._query, scoredDocs);
			queryResponse += HTML_FOOTER;
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Server", "Java HTTP Search Server");
			responseHeaders.set("Content-Type", "text/html; charset=iso-8859-1");
			responseHeaders.set("Cache-Control", "no-cache");
			// responseHeaders.set("Status", "HTTP/1.1 200 OK");
			// responseHeaders.set("Content-Length",
			// Integer.toString(queryResponse.length()));
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
			OutputStream responseBody = exchange.getResponseBody();
			responseBody.write(queryResponse.getBytes());
			responseBody.close();
			break;
		default:
			// nothing
		}
		System.out.println("Finished query: " + cgiArgs._query);
	}

	/**
	 * CGI arguments provided by the user through the URL. This will determine
	 * which Ranker to use and what output format to adopt. For simplicity, all
	 * arguments are publicly accessible.
	 */
	public static class CgiArguments {
		// The raw user query
		public String _query = "";
		public RankerType _rankerType = RankerType.NONE;
		public OutputFormat _outputFormat = OutputFormat.TEXT;
		// How many results to return
		private int _numResults = 20;

		public CgiArguments(String uriQuery) {
			String[] params = uriQuery.split("&");
			for (String param : params) {
				String[] keyval = param.split("=", 2);
				if (keyval.length < 2) {
					continue;
				}
				String key = keyval[0].toLowerCase();
				String val = keyval[1];
				if (key.equals("query")) {
					_query = val;
				} else if (key.equals("num")) {
					try {
						_numResults = Integer.parseInt(val);
					} catch (NumberFormatException e) {
						// Ignored, search engine should never fail upon invalid user input.
					}
				} else if (key.equals("ranker")) {
					try {
						_rankerType = RankerType.valueOf(val.toUpperCase());
					} catch (IllegalArgumentException e) {
						// Ignored, search engine should never fail upon invalid user input.
					}
				} else if (key.equals("format")) {
					try {
						_outputFormat = OutputFormat.valueOf(val.toUpperCase());
					} catch (IllegalArgumentException e) {
						// Ignored, search engine should never fail upon invalid user input.
					}
				}
			} // End of iterating over params
		}

		// The type of the ranker we will be using.
		public enum RankerType {
			NONE, FULLSCAN, CONJUNCTIVE, FAVORITE, COSINE, PHRASE, QL, LINEAR,
		}

		// The output format.
		public enum OutputFormat {
			TEXT, HTML,
		}
	}
}
