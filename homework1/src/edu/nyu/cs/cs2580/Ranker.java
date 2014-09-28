package edu.nyu.cs.cs2580;

import java.util.Scanner;
import java.util.Vector;

class Ranker {
  private Index _index;

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

  public Ranker(String index_source) {
    _index = new Index(index_source);
  }

  public Vector<ScoredDocument> runQuery(String query, String rankerType) {

    Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();
    for (int i = 0; i < _index.numDocs(); ++i) {
      retrieval_results.add(scoreDocument(query, i, rankerType));
    }

    return retrieval_results;
  }

  public ScoredDocument scoreDocument(String query, int did, String rankerType) {

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
      Vector<String> documentVector = document.get_title_vector();

      double score = 0.0;

      // TODO: invoke algorithm according to the ranker type
      RankingAlgorithm rankingAlgorithm = null;
      Ranker_Type ranker = Ranker_Type.valueOf(rankerType.toUpperCase());
      switch (ranker) {
      case COSINE:
        rankingAlgorithm = new VectorSpaceModel();
        break;
      case QL:
        break;
      case PHRASE:
        break;
      case LINEAR:
        break;
      default:
      }
      score = rankingAlgorithm.scoreDocument(queryVector, documentVector);
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
}
