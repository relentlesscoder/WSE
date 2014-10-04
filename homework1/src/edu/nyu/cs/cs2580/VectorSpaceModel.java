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
    List<String> allTerms = new ArrayList<String>();
    allTerms.addAll(document.getTitleList());
    allTerms.addAll(document.getBodyList());
    Set<String> uniqueTerms = new HashSet<String>(allTerms);

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

      if (allTerms.contains(query)) {
        d_j = getDocumentTermWeights(allTerms, query);
        a += d_j * q_j;
      }

      c += q_j * q_j;
    }

    // Calculate the part of denominator b
    for (String s : uniqueTerms) {
      d_j = getDocumentTermWeights(allTerms, s);
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

  private double getDocumentTermWeights(List<String> allTerms, String term) {
    double tf_ik = getTermFrequencyWeight(allTerms, term);
    double idf_k = getInverseDocumentFrequency(term);

    return tf_ik * idf_k;
  }

  private double getTermFrequencyWeight(List<String> allTerms, String term) {
    double tf_ik = 0.0;
    double f_ik = getTermFrequencyOfDocument(allTerms, term);

    tf_ik = f_ik / allTerms.size();

    return tf_ik;
  }

  private double getTermFrequencyOfDocument(List<String> allTerms, String term) {
    double f_ik = 0.0;

    for (String s : allTerms) {
      if (s.equals(term)) {
        f_ik++;
      }
    }

    return f_ik;
  }

  private double getInverseDocumentFrequency(String term) {
    double N = index.numDocs();
    double n_t = index.documentFrequency(term);

    return Math.log(N / n_t);
  }
}
