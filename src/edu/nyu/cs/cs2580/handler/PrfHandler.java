package edu.nyu.cs.cs2580.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import edu.nyu.cs.cs2580.document.ScoredDocument;
import edu.nyu.cs.cs2580.index.Indexer;
import edu.nyu.cs.cs2580.*;
import edu.nyu.cs.cs2580.index.IndexerInvertedCompressed;
import edu.nyu.cs.cs2580.minning.RelevanceFeedback;
import edu.nyu.cs.cs2580.query.Query;
import edu.nyu.cs.cs2580.query.QueryPhrase;
import edu.nyu.cs.cs2580.rankers.Ranker;
import edu.nyu.cs.cs2580.SearchEngine.Options;

import java.io.IOException;
import java.io.OutputStream;
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
public class PrfHandler extends BaseHandler {
  public PrfHandler(Options options, Indexer indexer) {
    super(options, indexer);
  }

  private String constructPrfJsonOutput(Query processedQuery, String suggestionTerms) {
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

    if (!uriPath.equals("/prf")) {
      message = "Only /prf is handled!";
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
    Vector<ScoredDocument> scoredDocs = ranker.runQuery(processedQuery, cgiArgs._numDocs);
    String response = RelevanceFeedback.extendQuery(cgiArgs,
        (IndexerInvertedCompressed) _indexer, processedQuery, scoredDocs);

    switch (cgiArgs._outputFormat) {
      case TEXT: {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
        OutputStream responseBody = exchange.getResponseBody();
        responseBody.write(response.getBytes());
        responseBody.flush();
        responseBody.close();
        break;
      }
      case JSON: {
        String queryResponse = constructPrfJsonOutput(processedQuery, response);
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
