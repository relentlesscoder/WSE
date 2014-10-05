package edu.nyu.cs.cs2580;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

class QueryHandler implements HttpHandler {

  private static final Logger logger = LogManager.getLogger(QueryHandler.class);
  private static final String LOG_FILE_NAME = "hw1.4-log.tsv";
  private static final String ACTION_RENDER = "render";
  private static final String HTML_HEADER = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\r\n<html>\r\n<head>\r\n<title>Web Search Engine</title>\r\n<script src=\"//ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js\"></script>\r\n<script type=\"text/javascript\">\r\n$(document).ready(function () {\r\n$(\".clickLoggingTrigger\").click(function () {\r\nvar docId = $(this).attr(\"doc-id\");\r\nvar sessionId = $(\"#divSesstionId\").text();\r\nvar query = $(\"#divQueryText\").text();\r\nvar url = 'http://' + location.host + '/logging?query=' + encodeURIComponent(query) + '&docId=' + encodeURIComponent(docId) + '&sessionId=' + encodeURIComponent(sessionId);\r\n$.ajax({\r\nurl: url,\r\ntype: 'GET',\r\ncache: false,\r\nstatusCode: {\r\n500: function(){\r\nconsole.log('Server internal error.');\r\n}},\r\nsuccess: function() {\r\nconsole.log('click event logged.');\r\n},\r\nerror: function(){\r\nconsole.log('click event logging failed.');\r\n}});\r\n});\r\n});\r\n</script>\r\n</head>\r\n<body>\r\n";
  private static final String HTML_FOOTER = "</body>\r\n</html>";
  private static final String QUERY_REQUIRED = "Query is required!\n";
  private static final String RANKER_REQUIRED = "Ranker is required!\n";
  private static final String FORMAT_REQUIRED = "Format is required!\n";
  private static final String INVALID_RANKER = "Ranker type is invalid!\n";
  private static final String INVALID_FORMAT = "Format type is invalid!\n";
  private List<String> VALID_RANKER = new ArrayList<String>();
  private List<String> VALID_FORMAT = new ArrayList<String>();
  private Index index;
  private final UUID sessionId;

  public QueryHandler(Index index) {
    VALID_RANKER.add("cosine");
    VALID_RANKER.add("QL");
    VALID_RANKER.add("numviews");
    VALID_RANKER.add("phrase");
    VALID_RANKER.add("linear");
    VALID_FORMAT.add("text");
    VALID_FORMAT.add("html");
    this.index = index;
    sessionId = UUID.randomUUID();
  }

  private BaseRanker initRanker(RANKER_TYPE rankerType) {
    BaseRanker ranker;

    switch (rankerType) {
      case QL:
        ranker = new LanguageModel(index);
        break;
      case PHRASE:
        ranker = new PhraseRanker(index);
        break;
      case LINEAR:
        ranker = new LinearRanker(index);
        break;
      case NUMVIEWS:
        ranker = new NumviewsRanker(index);
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

  private String buildLog(String queryText, List<ScoredDocument> scoredDocuments) {
    StringBuilder sb = new StringBuilder();
    DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
    Date date = new Date();

    for (ScoredDocument scoredDocument : scoredDocuments) {
      String clickLog = sessionId + "\t";
      clickLog += queryText + "\t";
      clickLog += scoredDocument.getDocId() + "\t";
      clickLog += ACTION_RENDER + "\t" + df.format(date) + "\r\n";
      sb.append(clickLog);
    }

    return sb.toString();
  }

  private String buildHtmlOutput(String queryText,
                                 List<ScoredDocument> scoredDocuments, UUID sessionId) {
    StringBuilder output = new StringBuilder();
    output.append("<div>Your search for term ");
    output.append(queryText);
    output.append(" returns " + Integer.toString(scoredDocuments.size())
        + " documents.</div>\r\n");
    output.append("<div id=\"divSearchContainer\">\r\n");
    for (ScoredDocument scoredDocument : scoredDocuments) {
      output.append("<div id=\"divDocument" + scoredDocument.getDocId()
          + "\" class=\"clickLoggingTrigger\" doc-id=\""
          + scoredDocument.getDocId() + "\">\r\n");
      output.append("<div>" + scoredDocument.getTitle() + "</div>\r\n");
      output.append("<div>" + scoredDocument.getScore() + "</div>\r\n");
      output.append("</div>\r\n");
    }
    output.append("</div>\r\n");
    output.append("<div id=\"divSesstionId\" style=\"display:none\">"
        + sessionId + "</div>\r\n");
    output.append("<div id=\"divQueryText\" style=\"display:none\">"
        + queryText + "</div>\r\n");
    return output.toString();
  }

  private String convertScoredDocToString(List<ScoredDocument> scoredDocuments, String query) {
    StringBuilder sb = new StringBuilder();

    for (ScoredDocument scoredDocument : scoredDocuments) {
      sb.append(query + "\t");
      sb.append(scoredDocument.toString());
      sb.append('\n');
    }

    return sb.toString();
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
    Map<String, String> queryMap;
    List<ScoredDocument> scoredDocuments = new ArrayList<ScoredDocument>();
    logger.debug("Query: " + uriQuery);
    logger.debug("Path: " + uriPath);

    if ((uriPath != null) && (uriQuery != null)) {
      if (uriPath.equals("/search")) {
        queryMap = Utility.getQueryMap(uriQuery);

        if (!queryMap.containsKey("query")) {
          queryResponse = QUERY_REQUIRED;
        } else if (!queryMap.containsKey("ranker")) {
          queryResponse = RANKER_REQUIRED;
        } else if (!queryMap.containsKey("format")) {
          queryResponse = FORMAT_REQUIRED;
        } else if (!VALID_RANKER.contains(queryMap.get("ranker"))) {
          queryResponse = INVALID_RANKER;
        } else {
          // Everything looks fine, proceed...
          String fileName = "";
          String query = queryMap.get("query");
          String rankerStr = queryMap.get("ranker");
          RANKER_TYPE rankerType = RANKER_TYPE.valueOf(rankerStr.toUpperCase());

          BaseRanker ranker = initRanker(rankerType);
          scoredDocuments = ranker.runQuery(query);


          switch (rankerType) {
            case QL:
              fileName += "hw1.1-ql.tsv";
              break;
            case PHRASE:
              fileName += "hw1.1-phrase.tsv";
              break;
            case LINEAR:
              fileName += "hw1.1-linear.tsv";
              break;
            case NUMVIEWS:
              fileName += "hw1.1-numviews.tsv";
              break;
            case COSINE:
            default:
              fileName += "hw1.1-vsm.tsv";
          }


          // Sort the scoredDocument decreasingly
          Collections.sort(scoredDocuments,
              new Comparator<ScoredDocument>() {
                @Override
                public int compare(ScoredDocument o1, ScoredDocument o2) {
                  return (o2.getScore() > o1.getScore()) ? 1 : (o2
                      .getScore() < o1.getScore()) ? -1 : 0;
                }
              });

          // Write the result to file
          Utility.WriteToFile(convertScoredDocToString(scoredDocuments, query), fileName, false);

          // Write the log to file
          String log = buildLog(queryMap.get("query"), scoredDocuments);
          Utility.WriteToFile(log, LOG_FILE_NAME, true);
        }

        RESPONSE_FORMAT format = null;

        if (!VALID_FORMAT.contains(queryMap.get("format"))) {
          queryResponse = INVALID_FORMAT;
        } else {
          format = getResponseFormat(queryMap.get("format"));
        }

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
     * Map to linear-based model
     */
    LINEAR,
    /**
     * Map to numviewed-based model
     */
    NUMVIEWS
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
