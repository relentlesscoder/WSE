package edu.nyu.cs.cs2580;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class VectorSpaceModel implements BaseRanker {
  private static final Logger logger = LogManager.getLogger();

  private Index index;

  public VectorSpaceModel(Index index) {
    this.index = index;
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

  public ScoredDocument scoreDocument(String query, int docId) {
    ScoredDocument scoredDocument = null;
    Scanner scanner = null;

    // Build query vector
    try {
      scanner = new Scanner(query);
      // <Term, Term frequency>
      Map<String, Integer> queryMap = new HashMap<String, Integer>();

      while (scanner.hasNext()) {
        String term = scanner.next().toLowerCase();
        if (queryMap.containsKey(term)) {
          queryMap.put(term, queryMap.get(term) + 1);
        } else {
          queryMap.put(term, 1);
        }
      }

      Document document = index.getDoc(docId);

      double score = cosineSimilarity(document, queryMap);

      scoredDocument = new ScoredDocument(docId, document.getTitleStr(),
          score);

    } catch (Exception e) {
      logger.error("Score document error while processing doc "
          + Integer.toString(docId) + ", due to: " + e);
    } finally {
      if (scanner != null) {
        scanner.close();
      }
    }

    return scoredDocument;
  }

  private double cosineSimilarity(Document document,
                                  Map<String, Integer> queryMap) {
    int numOfDocTerms = document.getBodyList().size() + document.getTitleList().size();
    List<String> allTerms = new ArrayList<String>();
    allTerms.addAll(document.getTitleList());
    allTerms.addAll(document.getBodyList());

    // <term, frequency> of a document
    Map<String, Double> termFrequencyMap = new HashMap<String, Double>();

    for (String s : allTerms) {
      if (termFrequencyMap.containsKey(s)) {
        termFrequencyMap.put(s, termFrequencyMap.get(s) + 1.0);
      } else {
        termFrequencyMap.put(s, 1.0);
      }
    }

    double score = 0.0;
    double d_j = 0.0;
    double q_j = 0.0;
    double a = 0.0;
    double b = 0.0;
    double c = 0.0;

    // Calculate the numerator a and part of the denominator c
    for (Map.Entry entry : queryMap.entrySet()) {
      String query = (String) entry.getKey();
      int queryFrequency = (Integer) entry.getValue();

      q_j = getQueryTermWeights(query, queryFrequency);

      if (termFrequencyMap.containsKey(query)) {
        d_j = getDocumentTermWeights(termFrequencyMap, numOfDocTerms, query);
        a += d_j * q_j;
      }

      c += q_j * q_j;
    }

    // Calculate the part of denominator b
    for (Map.Entry entry : termFrequencyMap.entrySet()) {
      String s = (String) entry.getKey();
      d_j = getDocumentTermWeights(termFrequencyMap, numOfDocTerms, s);
      b += d_j * d_j;
    }

    if (b * c != 0) {
      score = a / Math.sqrt(b * c);
    }

    return score;
  }

  private double getQueryTermWeights(String term, int termFrequency) {
    double tf_ik = termFrequency;
    double idf_k = getInverseDocumentFrequency(term);

    return tf_ik * idf_k;
  }

  private double getDocumentTermWeights(Map<String, Double> termFrequencyMap, int numOfDocTerms, String term) {
    double tf_ik = getTermFrequencyWeight(termFrequencyMap, numOfDocTerms, term);
    double idf_k = getInverseDocumentFrequency(term);

    return tf_ik * idf_k;
  }

  private double getTermFrequencyWeight(Map<String, Double> termFrequencyMap, int numOfDocTerms, String term) {
    double tf_ik = 0.0;
    double f_ik = termFrequencyMap.get(term);

    tf_ik = f_ik / numOfDocTerms;

    return tf_ik;
  }

  private double getInverseDocumentFrequency(String term) {
    double N = index.numDocs();
    double n_t = index.documentFrequency(term);

    return Math.log(N / n_t);
  }
}
