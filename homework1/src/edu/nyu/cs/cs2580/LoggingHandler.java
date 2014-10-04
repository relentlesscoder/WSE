package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LoggingHandler implements HttpHandler {

  private static final Logger logger = LogManager
      .getLogger(LoggingHandler.class);
  private static final String LOG_FILE_NAME = "hw1.4-log.tsv";
  private static final String ACTION_CLICK = "click";
  private static final String SESSIONID_REQUIRED = "SessionId is required.";
  private static final String DOCID_REQUIRED = "Document Id is required.";
  private static final String QUERY_REQUIRED = "Query is required.";

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    logger.debug("Logging handler start");
    String requestMethod = exchange.getRequestMethod();
    if (!requestMethod.equalsIgnoreCase("GET")) { // GET requests only.
      logger
          .error("Invalid HTTP request method, the server only supports GET.");
      return;
    }

    // Print the user request header.
    Headers requestHeaders = exchange.getRequestHeaders();
    logger.debug("Incoming request: ");
    for (String key : requestHeaders.keySet()) {
      String keyValue = key + ":" + requestHeaders.get(key) + "; ";
      logger.debug(keyValue);
    }

    boolean actionLogged = false;
    String uriQuery = exchange.getRequestURI().getQuery();
    String uriPath = exchange.getRequestURI().getPath();
    Map<String, String> queryMap = new HashMap<String, String>();
    logger.debug("Query: " + uriQuery);
    logger.debug("Path: " + uriPath);

    if ((uriPath != null) && (uriQuery != null)) {
      if (uriPath.equals("/logging")) {
        queryMap = Utility.getQueryMap(uriQuery);
        Set<String> keys = queryMap.keySet();
        if (keys.contains("sessionId")) {
          if (keys.contains("docId")) {
            if (keys.contains("query")) {
              DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
              Date date = new Date();
              String clickLog = queryMap.get("sessionId") + "\t";
              clickLog += queryMap.get("query") + "\t";
              clickLog += queryMap.get("docId") + "\t";
              clickLog += ACTION_CLICK + "\t" + df.format(date) + "\r\n";
              actionLogged = Utility.WriteToFile(clickLog, LOG_FILE_NAME, true);
            } else {
              logger.error(QUERY_REQUIRED);
            }
          } else {
            logger.error(DOCID_REQUIRED);
          }
        } else {
          logger.error(SESSIONID_REQUIRED);
        }
      }
    }

    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Server", "Java HTTP Logging Server");
    responseHeaders.set("Content-Type", "text/html; charset=iso-8859-1");
    responseHeaders.set("Cache-Control", "no-cache");
    if (actionLogged) {
      exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
    } else {
      exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, 0);
    }
    OutputStream responseBody = exchange.getResponseBody();
    responseBody.close();
  }

}
