package edu.nyu.cs.cs2580.handler;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import edu.nyu.cs.cs2580.document.Document;
import edu.nyu.cs.cs2580.document.DocumentNews;
import edu.nyu.cs.cs2580.document.ScoredDocument;
import edu.nyu.cs.cs2580.index.Indexer;
import edu.nyu.cs.cs2580.*;
import edu.nyu.cs.cs2580.query.Query;
import edu.nyu.cs.cs2580.query.QueryPhrase;
import edu.nyu.cs.cs2580.rankers.Ranker;
import edu.nyu.cs.cs2580.SearchEngine.Options;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

/**
 * Handles each incoming query, students do not need to change this class except
 * to provide more query time CGI arguments and the HTML output.
 * <p>
 * N.B. This class is not thread-safe.
 *
 * @author congyu
 * @author fdiaz
 */
public class NewsQueryHandler extends BaseHandler {

  class NewsSearchResponse {

    @SerializedName("queryText")
    private String _queryText;

    @SerializedName("results")
    private ArrayList<NewsSearchResult> _results;

    @SerializedName("status")
    private SearchStatus _status;

    public NewsSearchResponse(String queryText, ArrayList<NewsSearchResult> results, SearchStatus status){
      _queryText = queryText;
      _results = results;
      _status = status;
    }
  }

  public NewsQueryHandler(Options options, Indexer indexer) throws IOException {
    super(options, indexer);
  }

  private void constructTextOutput(final Vector<ScoredDocument> docs, StringBuffer response) {
    for (ScoredDocument doc : docs) {
      response.append(response.length() > 0 ? "\n" : "");
      response.append(doc.asTextResult());
    }
    response.append(response.length() > 0 ? "\n" : "");
  }

  private String constructHtmlOutput(String queryText, Vector<ScoredDocument> scoredDocuments, String template) {
    StringBuilder output = new StringBuilder();
    output.append("<div id=\"title\"><h1>Your search for term ");
    output.append(queryText);
    output.append(" returns " + Integer.toString(scoredDocuments.size())
        + " documents.</h1></div>\r\n");
    output.append("<div id=\"divSearchContainer\">\r\n");
    output.append("<ul id=\"unorderedList\">\r\n");
    for (ScoredDocument scoredDocument : scoredDocuments) {
      DocumentNews document = (DocumentNews)scoredDocument.getDocument();
      output.append("<li class=\"divDocument" + "\">\r\n");
      output.append("<a href=\"" + scoredDocument.getServerUrl()
          + "\" class=\"doc_title\">" + scoredDocument.getTitle() + "</a>\r\n");
      output.append("<span >" + document.getSource() + "<span>-</span>" + "<span>"+ document.getTime() +"</span>"
              + "</p>\r\n");
      output.append("<p class=\"score\">Score: " + scoredDocument.getScore()
          + "</p>\r\n");
      output.append("<p>" + document.getDescription()
          + "</p>\r\n");
      output.append("</li>\r\n");
    }
    output.append("</ul>\r\n");
    output.append("</div>\r\n");
    return template.replace(PLACE_HOLDER, output.toString());
  }

  private String constructJsonOutput(String queryText, Vector<ScoredDocument> scoredDocuments) {

    ArrayList<NewsSearchResult> results = new ArrayList<NewsSearchResult>();
    NewsSearchResult result;
    for (ScoredDocument scoredDocument : scoredDocuments) {
      DocumentNews document = (DocumentNews)scoredDocument.getDocument();
      result = new NewsSearchResult(scoredDocument.getTitle(), scoredDocument.getServerUrl(),
          scoredDocument.getScore(), document.getTime().getTime(), document.getSource(), document.getDescription());
      results.add(result);
    }
    //TODO: add error handling status
    SearchStatus status = new SearchStatus(STATUS_SUCCESS, STATUS_SUCCESS_MSG);
    NewsSearchResponse searchResponse = new NewsSearchResponse(queryText, results, status);

    Gson gson = new Gson();
    String response = gson.toJson(searchResponse);

    return response;
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

  public void handle(HttpExchange exchange) throws IOException {
    String requestMethod = exchange.getRequestMethod();
    String message = "";
    if (!requestMethod.equalsIgnoreCase("GET")) {
      // GET requests only.
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

    if (!uriPath.equals("/search/news")) {
      message = "Only /search/news is handled!";
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
    if (cgiArgs._rankerType != CgiArguments.RankerType.NEWS) {
      // Only use news ranker
      return;
    }
    Ranker ranker = Ranker.Factory.getRankerByArguments(cgiArgs, SearchEngine.OPTIONS, _indexer);
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
    Vector<ScoredDocument> scoredDocs = ranker.runQuery(processedQuery, cgiArgs._numResults);

    switch (cgiArgs._outputFormat) {
      case TEXT: {
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
      case JSON: {
        String queryResponse = constructJsonOutput(cgiArgs._query, scoredDocs);
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Server", "Java JSON API");
        responseHeaders.set("Content-Type", "application/jsonp; charset=UTF-8");
        responseHeaders.set("Cache-Control", "no-cache");
        responseHeaders.set("Access-Control-Allow-Origin", "*");
        responseHeaders.set("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
        responseHeaders.set("Access-Control-Allow-Methods", "GET");
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
  }
}

