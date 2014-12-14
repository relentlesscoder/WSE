package edu.nyu.cs.cs2580.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.nyu.cs.cs2580.*;
import edu.nyu.cs.cs2580.document.ScoredDocument;
import edu.nyu.cs.cs2580.index.Indexer;
import edu.nyu.cs.cs2580.query.Query;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

public abstract class BaseHandler implements HttpHandler {
  // For accessing the underlying documents to be used by the Ranker. Since
  // we are not worried about thread-safety here, the Indexer class must take
  // care of thread-safety.
  protected Indexer _indexer;
  protected SearchEngine.Options _options;

  protected static final String PLACE_HOLDER = "|Dynamic-Content-Place-Holder|";
  protected static final int STATUS_SUCCESS = 0;
  protected static final String STATUS_SUCCESS_MSG = "SUCCESS";

  // Constructor
  public BaseHandler(SearchEngine.Options options, Indexer indexer) {
    _indexer = indexer;
    _options = options;
  }

  // Construct plain text response
  private void constructTextResponse(final Vector<ScoredDocument> docs, StringBuilder response) {
    for (ScoredDocument doc : docs) {
      response.append(response.length() > 0 ? "\n" : "");
      response.append(doc.asTextResult());
    }
    response.append(response.length() > 0 ? "\n" : "");
  }

  // Construct html response
  @Deprecated
  private String constructHtmlResponse(String queryText, Vector<ScoredDocument> scoredDocuments, String template) {
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

  // Construct JSON response for search results
  private String constructJsonSearchResponse(String queryText, Vector<ScoredDocument> scoredDocuments) {

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

  // Construct JSON response for PRF
  private String constructJsonPrfResponse(Query processedQuery, String suggestionTerms) {
    Vector<String> queryTerms = processedQuery._tokens;
    String[] termsAndPrf = suggestionTerms.split("\n");
    String[] suggestionQueries = new String[termsAndPrf.length];

    StringBuilder originalQuerySB = new StringBuilder();
    for (String queryTerm : queryTerms) {
      originalQuerySB.append(queryTerm);
      originalQuerySB.append(" ");
    }
    String originalQuery = originalQuerySB.toString();

    for (int i = 0; i < termsAndPrf.length; i++) {
      String termAndPrf = termsAndPrf[i];
      String[] strArray = termAndPrf.split("\t");

      suggestionQueries[i] = originalQuery + strArray[0];
    }
    //TODO: add error handling status
    Gson gson = new Gson();
    String response = gson.toJson(suggestionQueries);

    return response;
  }

  public abstract void handle(HttpExchange exchange) throws IOException;

//  public void handle(HttpExchange exchange) throws IOException {
//    String requestMethod = exchange.getRequestMethod();
//    String message = "";
//    if (!requestMethod.equalsIgnoreCase("GET")) { // GET requests only.
//      return;
//    }
//
//    // Print the user request header.
//    Headers requestHeaders = exchange.getRequestHeaders();
//    System.out.print("Incoming request: ");
//    for (String key : requestHeaders.keySet()) {
//      System.out.print(key + ":" + requestHeaders.get(key) + "; ");
//    }
//    System.out.println();
//
//    // Validate the incoming request.
//    String uriQuery = exchange.getRequestURI().getQuery();
//    String uriPath = exchange.getRequestURI().getPath();
//    if (uriPath == null || uriQuery == null) {
//      message = "Something wrong with the URI!";
//      Headers responseHeaders = exchange.getResponseHeaders();
//      responseHeaders.set("Content-Type", "text/plain");
//      exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
//      OutputStream responseBody = exchange.getResponseBody();
//      responseBody.write(message.getBytes());
//      responseBody.flush();
//      responseBody.close();
//    }
//    if (!uriPath.equals("/search") && !uriPath.equals("/prf")) {
//      message = "Only /search or /prf is handled!";
//      Headers responseHeaders = exchange.getResponseHeaders();
//      responseHeaders.set("Content-Type", "text/plain");
//      exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
//      OutputStream responseBody = exchange.getResponseBody();
//      responseBody.write(message.getBytes());
//      responseBody.flush();
//      responseBody.close();
//    }
//    System.out.println("Query: " + uriQuery);
//
//    // Process the CGI arguments.
//    CgiArguments cgiArgs = new CgiArguments(uriQuery);
//    if (cgiArgs._query.isEmpty()) {
//      message = "No query is given!";
//      Headers responseHeaders = exchange.getResponseHeaders();
//      responseHeaders.set("Content-Type", "text/plain");
//      exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
//      OutputStream responseBody = exchange.getResponseBody();
//      responseBody.write(message.getBytes());
//      responseBody.flush();
//      responseBody.close();
//    }
//
//    // Create the ranker.
//    Ranker ranker = Ranker.Factory.getRankerByArguments(cgiArgs,
//        SearchEngine.OPTIONS, _indexer);
//    if (ranker == null) {
//      message =
//          "Ranker " + cgiArgs._rankerType.toString() + " is not valid!";
//      Headers responseHeaders = exchange.getResponseHeaders();
//      responseHeaders.set("Content-Type", "text/plain");
//      exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
//      OutputStream responseBody = exchange.getResponseBody();
//      responseBody.write(message.getBytes());
//      responseBody.flush();
//      responseBody.close();
//    }
//
//
//    // Processing the query.
//    Query processedQuery;
//    if (cgiArgs._query.matches(".*(\".+\").*")) {
//      processedQuery = new QueryPhrase(cgiArgs._query, true);
//    } else {
//      processedQuery = new QueryPhrase(cgiArgs._query, false);
//    }
//    processedQuery.processQuery();
//
//    if (processedQuery._tokens == null || processedQuery._tokens.size() <= 0) {
//      message = "Invalid query text!";
//      Headers responseHeaders = exchange.getResponseHeaders();
//      responseHeaders.set("Content-Type", "text/plain");
//      exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
//      OutputStream responseBody = exchange.getResponseBody();
//      responseBody.write(message.getBytes());
//      responseBody.flush();
//      responseBody.close();
//    }
//
//    // Ranking.
//    if (uriPath.equals("/search")) {
//      Vector<ScoredDocument> scoredDocs = ranker.runQuery(processedQuery, cgiArgs._numResults);
//
//      switch (cgiArgs._outputFormat) {
//        case TEXT: {
//          StringBuffer response = new StringBuffer();
//          constructTextResponse(scoredDocs, response);
//          Headers responseHeaders = exchange.getResponseHeaders();
//          responseHeaders.set("Content-Type", "text/plain");
//          exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
//          OutputStream responseBody = exchange.getResponseBody();
//          responseBody.write(response.toString().getBytes());
//          responseBody.close();
//          break;
//        }
//        case HTML: {
//          String queryResponse = readHtmlTemplate(_options._resultTemplate);
//          queryResponse = constructHtmlResponse(cgiArgs._query, scoredDocs,
//              queryResponse);
//          Headers responseHeaders = exchange.getResponseHeaders();
//          responseHeaders.set("Server", "Java HTTP Search Server");
//          responseHeaders.set("Content-Type", "text/html; charset=iso-8859-1");
//          responseHeaders.set("Cache-Control", "no-cache");
//          exchange.sendResponseHeaders(200, queryResponse.getBytes().length);
//          OutputStream responseBody = exchange.getResponseBody();
//          responseBody.write(queryResponse.getBytes());
//          responseBody.flush();
//          responseBody.close();
//          break;
//        }
//        case JSON: {
//          String queryResponse = constructJsonSearchResponse(cgiArgs._query, scoredDocs);
//          Headers responseHeaders = exchange.getResponseHeaders();
//          responseHeaders.set("Server", "Java JSON API");
//          responseHeaders.set("Content-Type", "application/jsonp; charset=UTF-8");
//          responseHeaders.set("Cache-Control", "no-cache");
//          responseHeaders.set("Access-Control-Allow-Origin","*");
//          responseHeaders.set("Access-Control-Allow-Headers","Origin, X-Requested-With, Content-Type, Accept");
//          responseHeaders.set("Access-Control-Allow-Methods","GET");
//          exchange.sendResponseHeaders(200, queryResponse.getBytes().length);
//          OutputStream responseBody = exchange.getResponseBody();
//          responseBody.write(queryResponse.getBytes());
//          responseBody.flush();
//          responseBody.close();
//          break;
//        }
//        default:
//          // nothing
//      }
//      System.out.println("Finished query: " + cgiArgs._query);
//    } else if (uriPath.equals("/prf")) {
//      Vector<ScoredDocument> scoredDocs = ranker.runQuery(processedQuery, cgiArgs._numDocs);
//      String response = RelevanceFeedback.extendQuery(cgiArgs,
//          (IndexerInvertedCompressed) _indexer, processedQuery, scoredDocs);
//
//      switch (cgiArgs._outputFormat) {
//        case TEXT: {
//          Headers responseHeaders = exchange.getResponseHeaders();
//          responseHeaders.set("Content-Type", "text/plain");
//          exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
//          OutputStream responseBody = exchange.getResponseBody();
//          responseBody.write(response.getBytes());
//          responseBody.flush();
//          responseBody.close();
//          break;
//        }
//        case JSON: {
//          String queryResponse = constructJsonPrfResponse(processedQuery, response);
//          Headers responseHeaders = exchange.getResponseHeaders();
//          responseHeaders.set("Server", "Java JSON API");
//          responseHeaders.set("Content-Type", "application/jsonp; charset=UTF-8");
//          responseHeaders.set("Cache-Control", "no-cache");
//          responseHeaders.set("Access-Control-Allow-Origin", "*");
//          responseHeaders.set("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
//          responseHeaders.set("Access-Control-Allow-Methods", "GET");
//          exchange.sendResponseHeaders(200, queryResponse.getBytes().length);
//          OutputStream responseBody = exchange.getResponseBody();
//          responseBody.write(queryResponse.getBytes());
//          responseBody.flush();
//          responseBody.close();
//          break;
//        }
//        default:
//          // nothing
//      }
//    }
//  }

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
