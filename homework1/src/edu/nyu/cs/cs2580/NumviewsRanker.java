package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.util.List;

public class NumviewsRanker implements BaseRanker {
  private Index index;

  public NumviewsRanker(Index index) {
    this.index = index;
  }

  @Override
  public List<ScoredDocument> runQuery(String query) {
    List<ScoredDocument> retrieval_results = new ArrayList<ScoredDocument>();

    for (int docId = 0; docId < index.numDocs(); ++docId) {
      Document document = index.getDoc(docId);
      retrieval_results.add(new ScoredDocument(docId, document
          .getTitleStr(), document.getNumviews()));
    }

    return retrieval_results;
  }
}
