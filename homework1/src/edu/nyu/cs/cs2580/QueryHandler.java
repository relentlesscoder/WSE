package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

class QueryHandler implements HttpHandler {
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

  private static final String QUERY_REQUIRED = "Query text is required!\n";
  private static final String RANKER_REQUIRED = "Ranker type is required!\n";
  private static final String INVALID_RANKER_TYPE = "Ranker type is invalid!\n";
  private static final ArrayList<String> VALID_RANKER = new ArrayList<String>();

  private String _indexPath;

  public QueryHandler(String indexPath) {
    VALID_RANKER.add("cosine");
    VALID_RANKER.add("QL");
    VALID_RANKER.add("phrase");
    VALID_RANKER.add("linear");
    _indexPath = indexPath;
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
    String queryResponse = "";
    String uriQuery = exchange.getRequestURI().getQuery();
    String uriPath = exchange.getRequestURI().getPath();

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
              // TODO: initialize specific ranker according to the ranker type
              BaseRanker ranker = null;
              Ranker_Type type = Ranker_Type.valueOf(rankerType.toUpperCase());
              switch (type) {
              case QL:
                ranker = new LanguageModel(_indexPath);
                break;
              case PHRASE:
                ranker = new PhraseRanker(_indexPath);
                break;
              case LINEAR:
                ranker = new LinearRanker(_indexPath);
                break;
              case COSINE:
              default:
                ranker = new VectorSpaceModel(_indexPath);
                break;
              }
              Vector<ScoredDocument> sds = ranker.runQuery(query_map
                  .get("query"));
              // Sort the scoredDocument decreasingly
              WSEUtil.sortScore(sds);

              Iterator<ScoredDocument> itr = sds.iterator();
              while (itr.hasNext()) {
                ScoredDocument sd = itr.next();
                if (queryResponse.length() > 0) {
                  queryResponse = queryResponse + "\n";
                }
                queryResponse = queryResponse + query_map.get("query") + "\t"
                    + sd.asString();
              }
              if (queryResponse.length() > 0) {
                queryResponse = queryResponse + "\n";
              }
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
