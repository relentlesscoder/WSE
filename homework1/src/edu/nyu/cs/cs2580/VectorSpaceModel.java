package edu.nyu.cs.cs2580;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class VectorSpaceModel implements BaseRanker {

  private static final Logger logger = LogManager
      .getLogger(VectorSpaceModel.class);

  private Index index;

  public VectorSpaceModel(Index index) {
    this.index = index;
  }

  private static int getTermFrequency(String term,
                                      HashMap<String, Integer> termsMap) {

    int termFrequency = 0;

    if (termsMap != null && termsMap.containsKey(term)) {
      termFrequency = termsMap.get(term);
    }

    return termFrequency;
  }

  private static double getInverseDocumentFrequency(int documentFrequency,
                                                    int numDocs) {
    return Math.log((double) numDocs / (double) (1 + documentFrequency));
  }

  @Override
  public List<ScoredDocument> runQuery(String query) {
    logger.debug("Start running query");
    List<ScoredDocument> retrieval_results = new ArrayList<ScoredDocument>();
    for (int i = 0; i < index.numDocs(); ++i) {
      retrieval_results.add(scoreDocument(query, i));
    }

    logger.debug("Finish running query");
    return retrieval_results;
  }

  public ScoredDocument scoreDocument(String query, int did) {

    ScoredDocument scoredDocument = null;
    // Build query vector
    Scanner scanner = null;
    try {
      scanner = new Scanner(query);
      HashMap<String, Integer> queryMap = new HashMap<String, Integer>();
      while (scanner.hasNext()) {
        String term = scanner.next().toLowerCase();
        if (queryMap.containsKey(term)) {
          int tf = queryMap.get(term);
          queryMap.put(term, tf + 1);
        } else {
          queryMap.put(term, 1);
        }
      }

      // Get the document vector. For hw1, you don't have to worry about the
      // details of how index works.
      Document document = index.getDoc(did);
      double score = cosineSimilarity(document, queryMap);
      scoredDocument = new ScoredDocument(did, document.getTitleStr(),
          score);
    } catch (Exception e) {
      logger.error("Score document error while processing doc "
          + Integer.toString(did) + ", due to: " + e);
    } finally {
      if (scanner != null) {
        scanner.close();
      }
    }

    return scoredDocument;
  }

  private double cosineSimilarity(Document document,
                                  HashMap<String, Integer> queryMap) {
    double score = 0.0;

    int numTerms = index.numTerms();
    double interSection = 0.0;
    double normDoc = 0.0;
    double normQuery = 0.0;
    int numDocs = index.numDocs();
    for (int i = 0; i < numTerms; i++) {
      String term = index.getTerm(i);
      int documentFrequency = index.documentFrequency(term);
      int termFrequency = document.termFrequency(term);
      double tfIdfDoc = tfIdfDocument(termFrequency, documentFrequency, numDocs);
      double tfidfQuery = tfIdfQuery(term, queryMap, documentFrequency, numDocs);
      interSection += tfIdfDoc * tfidfQuery;
      normDoc += Math.pow(tfIdfDoc, 2);
      normQuery += Math.pow(tfidfQuery, 2);
    }
    score = interSection / (Math.sqrt(normDoc) * Math.sqrt(normQuery));
    logger.debug("DocId: " + Integer.toString(document.docId) + " score: "
        + score);
    return score;
  }

  /**
   * Calculates the term frequency-inverse document frequency weight of the
   * vector
   *
   * @return the term frequency-inverse document frequency weight
   */
  private double tfIdfDocument(int termFrequency, int documentFrequency,
                               int numDocs) {
    double idf = getInverseDocumentFrequency(documentFrequency, numDocs);
    return termFrequency * idf;
  }

  private double tfIdfQuery(String term, HashMap<String, Integer> queryMap,
                            int documentFrequency, int numDocs) {

    int tf = getTermFrequency(term, queryMap);
    double idf = getInverseDocumentFrequency(documentFrequency, numDocs);

    return tf * idf;
  }

}