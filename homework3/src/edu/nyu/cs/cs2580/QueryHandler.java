package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.util.Vector;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * Handles each incoming query, students do not need to change this class except
 * to provide more query time CGI arguments and the HTML output.
 * 
 * N.B. This class is not thread-safe.
 * 
 * @author congyu
 * @author fdiaz
 */
class QueryHandler implements HttpHandler {

	/**
	 * CGI arguments provided by the user through the URL. This will determine
	 * which Ranker to use and what output format to adopt. For simplicity, all
	 * arguments are publicly accessible.
	 */
	public static class CgiArguments {
		// The raw user query
		public String _query = "";
		// How many results to return
		private int _numResults = 10;

		// The type of the ranker we will be using.
		public enum RankerType {
			NONE, FULLSCAN, CONJUNCTIVE, FAVORITE, COSINE, PHRASE, QL, LINEAR, COMPREHENSIVE,
		}

		public RankerType _rankerType = RankerType.NONE;

		// The output format.
		public enum OutputFormat {
			TEXT, HTML,
		}

		public OutputFormat _outputFormat = OutputFormat.TEXT;

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
	}

	// For accessing the underlying documents to be used by the Ranker. Since
	// we are not worried about thread-safety here, the Indexer class must take
	// care of thread-safety.
	private Indexer _indexer;
	private Options _options;

	private static final String PLACE_HOLDER = "|Dynamic-Content-Place-Holder|";

	public QueryHandler(Options options, Indexer indexer) {
		_indexer = indexer;
		_options = options;
	}

	private void constructTextOutput(final Vector<ScoredDocument> docs,
	    StringBuffer response) {
		for (ScoredDocument doc : docs) {
			response.append(response.length() > 0 ? "\n" : "");
			response.append(doc.asTextResult());
		}
		response.append(response.length() > 0 ? "\n" : "");
	}

	private String constructHtmlOutput(String queryText,
	    Vector<ScoredDocument> scoredDocuments, String template) {
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
		return template.replace(PLACE_HOLDER, output.toString());
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
			WebUtil.respondWithMsg(exchange, "Something wrong with the URI!");
		}
		if (!uriPath.equals("/search")) {
			WebUtil.respondWithMsg(exchange, "Only /search is handled!");
		}
		System.out.println("Query: " + uriQuery);

		// Process the CGI arguments.
		CgiArguments cgiArgs = new CgiArguments(uriQuery);
		if (cgiArgs._query.isEmpty()) {
			WebUtil.respondWithMsg(exchange, "No query is given!");
		}

		// Create the ranker.
		Ranker ranker = Ranker.Factory.getRankerByArguments(cgiArgs,
		    SearchEngine.OPTIONS, _indexer);
		if (ranker == null) {
			WebUtil.respondWithMsg(exchange,
			    "Ranker " + cgiArgs._rankerType.toString() + " is not valid!");
		}

		// Processing the query.
		Query processedQuery;
		if (cgiArgs._query.matches(".*(\".+\").*")) {
			processedQuery = new QueryPhrase(cgiArgs._query, true);
		} else {
			processedQuery = new QueryPhrase(cgiArgs._query, false);
		}
		processedQuery.processQuery();

		if (processedQuery.terms == null || processedQuery.terms.size() <= 0) {
			WebUtil.respondWithMsg(exchange, "Invalid query text!");
		}

		// Ranking.
		Vector<ScoredDocument> scoredDocs = ranker.runQuery(processedQuery,
		    cgiArgs._numResults);
		StringBuffer response = new StringBuffer();
		switch (cgiArgs._outputFormat) {
		case TEXT:
			constructTextOutput(scoredDocs, response);
			WebUtil.respondWithMsg(exchange, response.toString());
			break;
		case HTML:
			String queryResponse = WebUtil.readHtmlTemplate(_options._resultTemplate);
			queryResponse = constructHtmlOutput(cgiArgs._query, scoredDocs,
			    queryResponse);
			WebUtil.writeToResponse(exchange, queryResponse);
			break;
		default:
			// nothing
		}
		System.out.println("Finished query: " + cgiArgs._query);
	}
}
