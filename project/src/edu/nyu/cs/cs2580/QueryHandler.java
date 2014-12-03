	package edu.nyu.cs.cs2580;

	import java.io.*;
	import java.util.ArrayList;
	import java.util.Vector;

	import com.google.gson.Gson;
	import com.sun.net.httpserver.Headers;
	import com.sun.net.httpserver.HttpExchange;
	import com.sun.net.httpserver.HttpHandler;

	import edu.nyu.cs.cs2580.Document.ScoredDocument;
	import edu.nyu.cs.cs2580.Index.Indexer;
	import edu.nyu.cs.cs2580.Index.IndexerInvertedCompressed;
	import edu.nyu.cs.cs2580.Rankers.Ranker;
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
	public class QueryHandler implements HttpHandler {

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
      public int _numDocs=10;
      public int _numTerms=5;

			// The type of the ranker we will be using.
			public enum RankerType {
				NONE, FULLSCAN, CONJUNCTIVE, FAVORITE, COSINE, PHRASE, QL, LINEAR, COMPREHENSIVE,
			}

			public RankerType _rankerType = RankerType.NONE;

			// The output format.
			public enum OutputFormat {
				TEXT, HTML, JSON,
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
					} else if (key.equals("numdocs")) {
			  try {
				_numDocs = Integer.parseInt(val);
			  } catch (IllegalArgumentException e) {
				// Ignored, search engine should never fail upon invalid user input.
			  }
			} else if (key.equals("numterms")) {
			  try {
				_numTerms = Integer.parseInt(val);
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
		private static final int STATUS_SUCCESS = 0;
		private static final String STATUS_SUCCESS_MSG = "SUCCESS";

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
				output.append("<p class=\"score\">Score: " + scoredDocument.getScore()
						+ "</p>\r\n");
				output.append("<p class=\"score\">Rank: " + scoredDocument.getPageRank()
						+ "</p>\r\n");
				output.append("<p class=\"score\">Views: " + scoredDocument.getNumView()
						+ "</p>\r\n");
				output.append("</li>\r\n");
			}
			output.append("</ul>\r\n");
			output.append("</div>\r\n");
			return template.replace(PLACE_HOLDER, output.toString());
		}

		private String constructJsonOutput(String queryText,
										   Vector<ScoredDocument> scoredDocuments){

			ArrayList<SearchResult> results = new ArrayList<SearchResult>();
			SearchResult result;
			for (ScoredDocument scoredDocument : scoredDocuments) {
				result = new SearchResult(scoredDocument.getTitle(), scoredDocument.getServerUrl(),
						scoredDocument.getScore(), scoredDocument.getPageRank(), scoredDocument.getNumView());
				results.add(result);
			}
			//TODO: add error handling status
			SearchStatus status = new SearchStatus(STATUS_SUCCESS, STATUS_SUCCESS_MSG);
			SearchResponse searchResponse = new SearchResponse(queryText, results, status);
			Gson gson = new Gson();
			String response = gson.toJson(searchResponse);

			return response;
		}

		public void handle(HttpExchange exchange) throws IOException {
			String requestMethod = exchange.getRequestMethod();
			String message = "";
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
				message = "Something wrong with the URI!";
				Headers responseHeaders = exchange.getResponseHeaders();
				responseHeaders.set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
				OutputStream responseBody = exchange.getResponseBody();
				responseBody.write(message.getBytes());
				responseBody.flush();
				responseBody.close();
			}
			if (!uriPath.equals("/search")&&!uriPath.equals("/prf")) {
				message = "Only /search or /prf is handled!";
				Headers responseHeaders = exchange.getResponseHeaders();
				responseHeaders.set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
				OutputStream responseBody = exchange.getResponseBody();
				responseBody.write(message.getBytes());
				responseBody.flush();
				responseBody.close();
			}
			System.out.println("Query: " + uriQuery);

			// Process the CGI arguments.
			CgiArguments cgiArgs = new CgiArguments(uriQuery);
			if (cgiArgs._query.isEmpty()) {
				message = "No query is given!";
				Headers responseHeaders = exchange.getResponseHeaders();
				responseHeaders.set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
				OutputStream responseBody = exchange.getResponseBody();
				responseBody.write(message.getBytes());
				responseBody.flush();
				responseBody.close();
			}

			// Create the ranker.
			Ranker ranker = Ranker.Factory.getRankerByArguments(cgiArgs,
				SearchEngine.OPTIONS, _indexer);
			if (ranker == null) {
				message =
						"Ranker " + cgiArgs._rankerType.toString() + " is not valid!";
				Headers responseHeaders = exchange.getResponseHeaders();
				responseHeaders.set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
				OutputStream responseBody = exchange.getResponseBody();
				responseBody.write(message.getBytes());
				responseBody.flush();
				responseBody.close();
			}



			// Processing the query.
			Query processedQuery;
			if (cgiArgs._query.matches(".*(\".+\").*")) {
				processedQuery = new QueryPhrase(cgiArgs._query, true);
			} else {
				processedQuery = new QueryPhrase(cgiArgs._query, false);
			}
			processedQuery.processQuery();

			if (processedQuery._tokens == null || processedQuery._tokens.size() <= 0) {
				message = "Invalid query text!";
				Headers responseHeaders = exchange.getResponseHeaders();
				responseHeaders.set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
				OutputStream responseBody = exchange.getResponseBody();
				responseBody.write(message.getBytes());
				responseBody.flush();
				responseBody.close();
			}

			// Ranking.
		if (uriPath.equals("/search")){
		  Vector<ScoredDocument> scoredDocs = ranker.runQuery(processedQuery, cgiArgs._numResults);

		  switch (cgiArgs._outputFormat) {
			case TEXT:
			{
				StringBuffer response = new StringBuffer();
			  	constructTextOutput(scoredDocs, response);
				Headers responseHeaders = exchange.getResponseHeaders();
				responseHeaders.set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
				OutputStream responseBody = exchange.getResponseBody();
				responseBody.write(response.toString().getBytes());
				responseBody.close();
			  break;
			}
			case HTML: {
				String queryResponse = readHtmlTemplate(_options._resultTemplate);
				queryResponse = constructHtmlOutput(cgiArgs._query, scoredDocs,
						queryResponse);
				Headers responseHeaders = exchange.getResponseHeaders();
				responseHeaders.set("Server", "Java HTTP Search Server");
				responseHeaders.set("Content-Type", "text/html; charset=iso-8859-1");
				responseHeaders.set("Cache-Control", "no-cache");
				exchange.sendResponseHeaders(200, queryResponse.getBytes().length);
				OutputStream responseBody = exchange.getResponseBody();
				responseBody.write(queryResponse.getBytes());
				responseBody.flush();
				responseBody.close();
				break;
			}
			  case JSON:{
				  String queryResponse = constructJsonOutput(cgiArgs._query, scoredDocs);
				  Headers responseHeaders = exchange.getResponseHeaders();
				  responseHeaders.set("Server", "Java JSON API");
				  responseHeaders.set("Content-Type", "application/json");
				  responseHeaders.set("Cache-Control", "no-cache");
				  exchange.sendResponseHeaders(200, queryResponse.getBytes().length);
				  OutputStream responseBody = exchange.getResponseBody();
				  responseBody.write(queryResponse.getBytes());
				  responseBody.flush();
				  responseBody.close();
				  break;
			}
			default:
			  // nothing
		  }
		  System.out.println("Finished query: " + cgiArgs._query);
		} else if (uriPath.equals("/prf")){
		  	Vector<ScoredDocument> scoredDocs = ranker.runQuery(processedQuery, cgiArgs._numDocs);
		  	String response = RelevanceFeedback.extendQuery(cgiArgs,
				  (IndexerInvertedCompressed)_indexer, processedQuery,scoredDocs);
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
			OutputStream responseBody = exchange.getResponseBody();
			responseBody.write(response.getBytes());
			responseBody.flush();
			responseBody.close();
		}
		}

		private String readHtmlTemplate(String filePath) {
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
	}
