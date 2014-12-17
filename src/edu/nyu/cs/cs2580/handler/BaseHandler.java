package edu.nyu.cs.cs2580.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.nyu.cs.cs2580.*;
import edu.nyu.cs.cs2580.spellCheck.BKTree.BKTree;
import edu.nyu.cs.cs2580.spellCheck.BKTree.DamerauLevenshteinAlgorithm;
import edu.nyu.cs.cs2580.spellCheck.BKTree.DistanceAlgo;
import edu.nyu.cs.cs2580.index.IndexerInvertedCompressed;
import edu.nyu.cs.cs2580.document.ScoredDocument;
import edu.nyu.cs.cs2580.index.Indexer;
import edu.nyu.cs.cs2580.query.Query;
import edu.nyu.cs.cs2580.spellCheck.MisspellDataSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

public abstract class BaseHandler implements HttpHandler {
  // Common misspell data corpora file, reference: http://www.dcs.bbk.ac.uk/~ROGER/corpora.html
  private static final File ASPELL_FILE = new File("spellCheckTestData/aspell.dat");
  private static final File MISSP_FILE = new File("spellCheckTestData/missp.dat");
  private static final File WIKIPEDIA_FILE = new File("spellCheckTestData/wikipedia.dat");

  private static final File DICTIONARY_FILE = new File("spellCheckTestData/words");

  private MisspellDataSet _misspellDataSet;

  // For accessing the underlying documents to be used by the Ranker. Since
  // we are not worried about thread-safety here, the Indexer class must take
  // care of thread-safety.
  protected IndexerInvertedCompressed _indexer;
  protected SearchEngine.Options _options;

  protected static final String PLACE_HOLDER = "|Dynamic-Content-Place-Holder|";
  protected static final int STATUS_SUCCESS = 0;
  protected static final String STATUS_SUCCESS_MSG = "SUCCESS";

  // For spell check
  DistanceAlgo<String> _distanceAlgo;
  protected BKTree<String> _bkTree;

  // Constructor
  public BaseHandler(SearchEngine.Options options, Indexer indexer) throws IOException {
    //TODO: Not handled well... For now that's just convenience
    _indexer = (IndexerInvertedCompressed) indexer;
    _options = options;

    _distanceAlgo = new DamerauLevenshteinAlgorithm<String>();
    _bkTree = new BKTree<String>(_distanceAlgo, _indexer.getTermFrequency());
    _bkTree.addDictionary(DICTIONARY_FILE);
//    _bkTree.addAll(_indexer.getDictionaryTerms());
    _misspellDataSet = new MisspellDataSet();
    _misspellDataSet.addData(ASPELL_FILE);
    _misspellDataSet.addData(MISSP_FILE);
    _misspellDataSet.addData(WIKIPEDIA_FILE);
    _bkTree.addMisspellDataSet(_misspellDataSet);
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
