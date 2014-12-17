package edu.nyu.cs.cs2580.handler;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import edu.nyu.cs.cs2580.SearchEngine;
import edu.nyu.cs.cs2580.document.DocumentNews;
import edu.nyu.cs.cs2580.document.ScoredDocument;
import edu.nyu.cs.cs2580.index.Indexer;
import edu.nyu.cs.cs2580.preprocess.Topic;
import edu.nyu.cs.cs2580.query.Query;
import edu.nyu.cs.cs2580.query.QueryPhrase;
import edu.nyu.cs.cs2580.rankers.Ranker;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by tanis on 12/17/14.
 */
public class TopicsHandler extends BaseHandler {

  class TopTopics {

    @SerializedName("keywords")
    private String _tokens;

    @SerializedName("results")
    private ArrayList<NewsSearchResult> _results;

    public TopTopics(String text, ArrayList<NewsSearchResult> results){
      _tokens = text;
      _results = results;
    }
  }

  class TopTopicsResponse {

    @SerializedName("results")
    private ArrayList<TopTopics> _results;

    @SerializedName("status")
    private SearchStatus _status;

    public TopTopicsResponse(ArrayList<TopTopics> results, SearchStatus status){
      _results = results;
      _status = status;
    }
  }

  public TopicsHandler(SearchEngine.Options options, Indexer indexer) throws IOException {
    super(options, indexer);
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

    if (!uriPath.equals("/search/topics")) {
      message = "Only /search/news is handled!";
      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set("Content-Type", "text/plain");
      exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
      OutputStream responseBody = exchange.getResponseBody();
      responseBody.write(message.getBytes());
      responseBody.flush();
      responseBody.close();
    }

    // Process the CGI arguments.
    CgiArguments cgiArgs = new CgiArguments(uriQuery);
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

    ArrayList<TopTopics> topics = new ArrayList<TopTopics>();
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("data/news/topic/topicRank.txt"))));
    String line;
    int topicNum = 5;
    while ((line = br.readLine()) != null && topicNum>=0) {
      String[] str = line.split("\t");
      String query = str[1].trim();
      Vector<ScoredDocument> scoredDocuments = topicTopDocs(ranker,query, cgiArgs);
      ArrayList<NewsSearchResult> results = new ArrayList<NewsSearchResult>();
      NewsSearchResult result;
      for (ScoredDocument scoredDocument : scoredDocuments) {
        DocumentNews document = (DocumentNews) scoredDocument.getDocument();
        result = new NewsSearchResult(scoredDocument.getTitle(), scoredDocument.getServerUrl(),
                scoredDocument.getScore(), document.getTime().getTime(), document.getSource(), document.getDescription());
        results.add(result);
      }
      topics.add(new TopTopics(query,results));
      topicNum--;
    }

    SearchStatus status = new SearchStatus(STATUS_SUCCESS, STATUS_SUCCESS_MSG);
    TopTopicsResponse searchResponse = new TopTopicsResponse(topics, status);
    Gson gson = new Gson();
    String response = gson.toJson(searchResponse);
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Server", "Java JSON API");
    responseHeaders.set("Content-Type", "application/jsonp; charset=UTF-8");
    responseHeaders.set("Cache-Control", "no-cache");
    responseHeaders.set("Access-Control-Allow-Origin", "*");
    responseHeaders.set("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
    responseHeaders.set("Access-Control-Allow-Methods", "GET");
    exchange.sendResponseHeaders(200, response.getBytes().length);
    OutputStream responseBody = exchange.getResponseBody();
    responseBody.write(response.getBytes());
    responseBody.flush();
    responseBody.close();
  }

  private Vector<ScoredDocument> topicTopDocs(Ranker ranker, String query, CgiArguments cgiArgs){
    // Processing the query.
    Query processedQuery;
    if (query.matches(".*(\".+\").*")) {
      processedQuery = new QueryPhrase(query, true);
    } else {
      processedQuery = new QueryPhrase(query, false);
    }
    processedQuery.processQuery();

    // Ranking.
    Vector<ScoredDocument> scoredDocs = ranker.runQuery(processedQuery, cgiArgs._numResults);
    return scoredDocs;
  }

}
