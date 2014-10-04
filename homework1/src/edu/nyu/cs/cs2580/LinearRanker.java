package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.util.List;

public class LinearRanker implements BaseRanker {
  private Index index;
  private final static double BETA_COS = 1.0;
  private final static double BETA_LM = 1.0;
  private final static double BETA_PHRASE = 1.0;
  private final static double BETA_NUMVIEWS = 0.0001;

  public LinearRanker(Index index) {
    this.index = index;
  }

  @Override
  public List<ScoredDocument> runQuery(String query) {
    List<ScoredDocument> retrievalResults = new ArrayList<ScoredDocument>();
    BaseRanker VSM = new VectorSpaceModel(index);
    BaseRanker LM = new LanguageModel(index);
    BaseRanker phrase = new PhraseRanker(index);
    BaseRanker numviews = new NumviewsRanker(index);

    for (int docId = 0; docId < index.numDocs(); ++docId) {
      Document document = index.getDoc(docId);
      retrievalResults.add(new ScoredDocument(docId, document
          .getTitleStr(), 0.0));
    }

    addRankerScore(retrievalResults, VSM.runQuery(query), BETA_COS);
    addRankerScore(retrievalResults, LM.runQuery(query), BETA_LM);
    addRankerScore(retrievalResults, phrase.runQuery(query), BETA_PHRASE);
    addRankerScore(retrievalResults, numviews.runQuery(query), BETA_NUMVIEWS);

    return retrievalResults;
  }

  private void addRankerScore(List<ScoredDocument> linearRankerResults, List<ScoredDocument> otherRankerResults, double beta) {
    for (int docId = 0; docId < index.numDocs(); ++docId) {
      Document document = index.getDoc(docId);

      double originScore = linearRankerResults.get(docId).getScore();
      double newScore = originScore + beta * otherRankerResults.get(docId).getScore();
      linearRankerResults.get(docId).setScore(newScore);
    }
  }
}
