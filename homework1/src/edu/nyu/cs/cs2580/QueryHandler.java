package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

class QueryHandler implements HttpHandler {

  private static final Logger logger = LogManager.getLogger(QueryHandler.class);

  /**
   * Ranker types that are used for ranking documents
   */
  private enum Ranker_Type {
    /**
     * Map to vector space model
     */
    COSINE,
    /**
     * Map to query likelihood with Jelinek-Mercer smoothing
     */
    QL,
    /**
     * Map to phrase-based model
     */
    PHRASE,
    /**
     * Map to numviewed-based model
     */
    LINEAR
  }

  /**
   * Define output format of server response
   * 
   */
  private enum RESPONSE_FORMAT {
    /**
     * HTML
     */
    HTML,
    /**
     * Plain text
     */
    TEXT
  }

  private static final String QUERY_REQUIRED = "Query text is required!\n";
  private static final String RANKER_REQUIRED = "Ranker type is required!\n";
  private static final String INVALID_RANKER_TYPE = "Ranker type is invalid!\n";
  private static final ArrayList<String> VALID_RANKER = new ArrayList<String>();

  private Index index;

  public QueryHandler(Index index) {
    VALID_RANKER.add("cosine");
    VALID_RANKER.add("QL");
    VALID_RANKER.add("phrase");
    VALID_RANKER.add("linear");
    this.index = index;
  }

  private static Map<String, String> getQueryMap(String query) {
    String[] params = query.split("&");
    Map<String, String> map = new HashMap<String, String>();
    for (String param : params) {
      String name = param.split("=")[0];
      String value = param.split("=")[1];
      map.put(name, value);
    }
    return map;
  }

  private static boolean isValidRankerType(String rankerType) {
    boolean isValid = true;
    if (rankerType == null || rankerType.isEmpty()) {
      isValid = false;
    } else if (!VALID_RANKER.contains(rankerType)) {
      isValid = false;
    }
    return isValid;
  }

  private BaseRanker initRanker(String rankerType) {
    BaseRanker ranker = null;
    Ranker_Type type = Ranker_Type.valueOf(rankerType.toUpperCase());
    switch (type) {
    case QL:
      ranker = new LanguageModel(index);
      break;
    case PHRASE:
      ranker = new PhraseRanker(index);
      break;
    case LINEAR:
      ranker = new LinearRanker(index);
      break;
    case COSINE:
    default:
      ranker = new VectorSpaceModel(index);
      break;
    }
    return ranker;
  }

  private String buildOutput(String queryText,
      Vector<ScoredDocument> scoredDocuments) {
    logger.debug("Start building output");
    String queryResponse = "";
    Iterator<ScoredDocument> itr = scoredDocuments.iterator();
    while (itr.hasNext()) {
      ScoredDocument sd = itr.next();
      if (queryResponse.length() > 0) {
        queryResponse = queryResponse + "\n";
      }
      queryResponse = queryResponse + queryText + "\t" + sd.asString();
    }
    if (queryResponse.length() > 0) {
      queryResponse = queryResponse + "\n";
    }
    logger.debug("Finish building output");
    return queryResponse;
  }

  public void handle(HttpExchange exchange) throws IOException {
    logger.debug("Query handler start processing query");
    String requestMethod = exchange.getRequestMethod();
    if (!requestMethod.equalsIgnoreCase("GET")) { // GET requests only.
      logger
          .error("Invalid HTTP request method, the server only supports GET.");
      return;
    }

    // Print the user request header.
    Headers requestHeaders = exchange.getRequestHeaders();
    System.out.print("Incoming request: ");
    logger.debug("Incoming request: ");
    for (String key : requestHeaders.keySet()) {
      String keyValue = key + ":" + requestHeaders.get(key) + "; ";
      System.out.print(keyValue);
      logger.debug(keyValue);
    }
    System.out.println();
    String queryResponse = "";
    String uriQuery = exchange.getRequestURI().getQuery();
    String uriPath = exchange.getRequestURI().getPath();
    logger.debug("Query: " + uriQuery);
    logger.debug("Path: " + uriPath);

    if ((uriPath != null) && (uriQuery != null)) {
      if (uriPath.equals("/search")) {
        Map<String, String> query_map = getQueryMap(uriQuery);
        Set<String> keys = query_map.keySet();
        if (keys.contains("query")) {
          if (keys.contains("ranker")) {
            String rankerType = query_map.get("ranker");
            if (!isValidRankerType(rankerType)) {
              queryResponse = INVALID_RANKER_TYPE;
            } else {
              BaseRanker ranker = initRanker(rankerType);
              Vector<ScoredDocument> scoredDocuments = ranker
                  .runQuery(query_map.get("query"));
              // Sort the scoredDocument decreasingly
              WSEUtil.sortScore(scoredDocuments);
              // TODO: add HTML method
              queryResponse = buildOutput(query_map.get("query"),
                  scoredDocuments);
            }
          } else {
            queryResponse = RANKER_REQUIRED;
          }
        } else {
          queryResponse = QUERY_REQUIRED;
        }
      }
    }

    // Construct a simple response.
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/plain");
    exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
    OutputStream responseBody = exchange.getResponseBody();
    responseBody.write(queryResponse.getBytes());
    responseBody.close();
  }
}
