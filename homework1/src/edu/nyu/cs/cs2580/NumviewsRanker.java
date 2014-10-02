package edu.nyu.cs.cs2580;

import java.util.Vector;

public class NumviewsRanker implements BaseRanker {
  private Index index;

  public NumviewsRanker(Index index) {
    this.index = index;
  }

  @Override
  public Vector<ScoredDocument> runQuery(String query) {
    Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();

    for (int docId = 0; docId < index.numDocs(); ++docId) {
      Document document = index.getDoc(docId);
      retrieval_results.add(new ScoredDocument(docId, document
          .get_title_string(), document.get_numviews()));
    }

    return retrieval_results;
  }
}
