package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

class QueryHandler implements HttpHandler {

  private static final Logger logger = LogManager.getLogger(QueryHandler.class);
  private static final String HTML_HEADER = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"><html><head><title>Web Search Engine</title><script src=\"//ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js\"></script><script type=\"text/javascript\">$(document).ready(function () {$(\".clickLoggingTrigger\").click(function () {var docId = $(this).attr(\"doc-id\");var sessionId = $(\"#divSesstionId\").text();console.log(docId);console.log(sessionId);});});</script></head><body>";
  private static final String HTML_FOOTER = "</body></html>";
  private static final String QUERY_REQUIRED = "Query text is required!\n";
  private static final String RANKER_REQUIRED = "Ranker type is required!\n";
  private static final String INVALID_RANKER_TYPE = "Ranker type is invalid!\n";
  private static final ArrayList<String> VALID_RANKER = new ArrayList<String>();
  private Index index;
  private final UUID sessionId;

  public QueryHandler(Index index) {
    VALID_RANKER.add("cosine");
    VALID_RANKER.add("QL");
    VALID_RANKER.add("phrase");
    VALID_RANKER.add("linear");
    this.index = index;
    sessionId = UUID.randomUUID();
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
    RANKER_TYPE type = RANKER_TYPE.valueOf(rankerType.toUpperCase());
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

  private RESPONSE_FORMAT getResponseFormat(String format) {
    RESPONSE_FORMAT responseFormat = RESPONSE_FORMAT.TEXT;
    if (format != null && format != ""
        && format.equalsIgnoreCase(RESPONSE_FORMAT.HTML.toString())) {
      responseFormat = RESPONSE_FORMAT.HTML;
    }
    return responseFormat;
  }

  private String buildOutput(String queryText,
      List<ScoredDocument> scoredDocuments) {
    logger.debug("Start building output");
    String queryResponse = "";
    Iterator<ScoredDocument> itr = scoredDocuments.iterator();
    while (itr.hasNext()) {
      ScoredDocument sd = itr.next();
      if (queryResponse.length() > 0) {
        queryResponse = queryResponse + "\n";
      }
      queryResponse = queryResponse + queryText + "\t" + sd.toString();
    }
    if (queryResponse.length() > 0) {
      queryResponse = queryResponse + "\n";
    }
    logger.debug("Finish building output");
    return queryResponse;
  }

  private String buildHtmlOutput(String queryText,
      List<ScoredDocument> scoredDocuments, UUID sessionId) {
    StringBuilder output = new StringBuilder();
    output.append("<div>Your search for term ");
    output.append(queryText);
    output.append(" returns " + Integer.toString(scoredDocuments.size())
        + " documents.</div>");
    output.append("<div id=\"divSearchContainer\">");
    for (ScoredDocument scoredDocument : scoredDocuments) {
      output.append("<div id=\"divDocument" + scoredDocument.getDocId()
          + "\" class=\"clickLoggingTrigger\" doc-id=\""
          + scoredDocument.getDocId() + "\">");
      output.append("<div>" + scoredDocument.getTitle() + "</div>");
      output.append("<div>" + scoredDocument.getScore() + "</div>");
      output.append("</div>");
    }
    output.append("</div>");
    output.append("<div id=\"divSesstionId\" style=\"display:none\">"
        + sessionId + "</div>");
    return output.toString();
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
    Map<String, String> queryMap = new HashMap<String, String>();
    List<ScoredDocument> scoredDocuments = new ArrayList<ScoredDocument>();
    logger.debug("Query: " + uriQuery);
    logger.debug("Path: " + uriPath);

    if ((uriPath != null) && (uriQuery != null)) {
      if (uriPath.equals("/search")) {
        queryMap = getQueryMap(uriQuery);
        Set<String> keys = queryMap.keySet();
        if (keys.contains("query")) {
          if (keys.contains("ranker")) {
            String rankerType = queryMap.get("ranker");
            if (!isValidRankerType(rankerType)) {
              queryResponse = INVALID_RANKER_TYPE;
            } else {
              BaseRanker ranker = initRanker(rankerType);
              scoredDocuments = ranker.runQuery(queryMap.get("query"));
              // Sort the scoredDocument decreasingly
              Collections.sort(scoredDocuments,
                  new Comparator<ScoredDocument>() {
                    @Override
                    public int compare(ScoredDocument o1, ScoredDocument o2) {
                      return (o2.getScore() > o1.getScore()) ? 1 : (o2
                          .getScore() < o1.getScore()) ? -1 : 0;
                    }
                  });
            }
          } else {
            queryResponse = RANKER_REQUIRED;
          }
        } else {
          queryResponse = QUERY_REQUIRED;
        }
      }
    }

    RESPONSE_FORMAT format = getResponseFormat(queryMap.get("format"));
    if (format == RESPONSE_FORMAT.HTML) {
      queryResponse += HTML_HEADER;
      queryResponse += buildHtmlOutput(queryMap.get("query"), scoredDocuments,
          sessionId);
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

    } else {
      queryResponse = buildOutput(queryMap.get("query"), scoredDocuments);
      // Construct a simple response.
      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set("Content-Type", "text/plain");
      exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
      OutputStream responseBody = exchange.getResponseBody();
      responseBody.write(queryResponse.getBytes());
      responseBody.close();
    }
  }

  /**
   * Ranker types that are used for ranking documents
   */
  private enum RANKER_TYPE {
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
}
