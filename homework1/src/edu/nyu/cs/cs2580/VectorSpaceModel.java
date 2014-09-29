package edu.nyu.cs.cs2580;

import java.util.Scanner;
import java.util.Vector;

public class VectorSpaceModel implements BaseRanker {

  private Index _index;

  public VectorSpaceModel(String indexSource) {
    _index = new Index(indexSource);
  }

  @Override
  public Vector<ScoredDocument> runQuery(String query) {
    Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();
    for (int i = 0; i < _index.numDocs(); ++i) {
      retrieval_results.add(scoreDocument(query, i));
    }

    return retrieval_results;
  }

  public ScoredDocument scoreDocument(String query, int did) {

    ScoredDocument scoredDocument = null;
    // Build query vector
    Scanner scanner = null;
    try {
      scanner = new Scanner(query);
      Vector<String> queryVector = new Vector<String>();
      while (scanner.hasNext()) {
        String term = scanner.next();
        queryVector.add(term);
      }

      // Get the document vector. For hw1, you don't have to worry about the
      // details of how index works.
      Document document = _index.getDoc(did);
      double score = cosineSimilarity(document, queryVector);
      scoredDocument = new ScoredDocument(did, document.get_title_string(),
          score);
    } catch (Exception e) {
      // TODO: handle exception
    } finally {
      if (scanner != null) {
        scanner.close();
      }
    }

    return scoredDocument;
  }

  private double cosineSimilarity(Document document, Vector<String> query) {
    double score = 0.0;

    int numTerms = _index.numTerms();
    double interSection = 0.0;
    double normDoc = 0.0;
    double normQuery = 0.0;
    for (int i = 1; i <= numTerms; i++) {
      String term = _index.getTerm(i);
      int documentFrequency = _index.documentFrequency(term);
      int numDocs = _index.numDocs();
      interSection += tfIdfDocument(term, document, documentFrequency, numDocs)
          * tfIdfQuery(term, query, documentFrequency, numDocs);
      normDoc += Math.pow(
          tfIdfDocument(term, document, documentFrequency, numDocs), 2);
      normQuery += Math.pow(
          tfIdfQuery(term, query, documentFrequency, numDocs), 2);
    }
    score = interSection / (Math.sqrt(normDoc) * Math.sqrt(normQuery));
    return score;
  }

  /**
   * Calculates the term frequency-inverse document frequency weight of the
   * vector
   * 
   * @return the term frequency-inverse document frequency weight
   */
  private double tfIdfDocument(String term, Document document,
      int documentFrequency, int numDocs) {

    int tf = getTermFrequency(term, document.get_title_vector())
        + getTermFrequency(term, document.get_body_vector());
    double idf = getInverseDocumentFrequency(term, documentFrequency, numDocs);

    return tf * idf;
  }

  private double tfIdfQuery(String term, Vector<String> queryVectors,
      int documentFrequency, int numDocs) {

    int tf = getTermFrequency(term, queryVectors);
    double idf = getInverseDocumentFrequency(term, documentFrequency, numDocs);

    return tf * idf;
  }

  private static int getTermFrequency(String term, Vector<String> vectors) {

    int termFrequency = 0;

    if (vectors != null && vectors.size() > 0) {
      for (String vector : vectors) {
        if (term.equals(vector)) {
          termFrequency++;
        }
      }
    }

    return termFrequency;
  }

  private static double getInverseDocumentFrequency(String term,
      int documentFrequency, int numDocs) {

    double inverseDocumentFrequency = 0.0;
    inverseDocumentFrequency = Math.log((double) numDocs
        / (double) (1 + documentFrequency));

    return inverseDocumentFrequency;
  }

}
