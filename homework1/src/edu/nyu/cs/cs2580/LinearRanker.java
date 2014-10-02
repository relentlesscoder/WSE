package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.util.List;

public class LinearRanker implements BaseRanker {
  private Index index;
  private final static double BETA = 0.50;

  public LinearRanker(Index index) {
    this.index = index;
  }

  @Override
  public List<ScoredDocument> runQuery(String query) {
    List<ScoredDocument> retrieval_results = new ArrayList<ScoredDocument>();
    List<ScoredDocument> retrieval_results_VSM;
    List<ScoredDocument> retrieval_results_LM;
    List<ScoredDocument> retrieval_results_phrase;
    List<ScoredDocument> retrieval_results_numviews;

    BaseRanker VSM = new VectorSpaceModel(index);
    retrieval_results_VSM = VSM.runQuery(query);

    BaseRanker LM = new LanguageModel(index);
    retrieval_results_LM = LM.runQuery(query);

    BaseRanker phrase = new PhraseRanker(index);
    retrieval_results_phrase = phrase.runQuery(query);

    BaseRanker numviews = new NumviewsRanker(index);
    retrieval_results_numviews = numviews.runQuery(query);

    for (int docId = 0; docId < index.numDocs(); ++docId) {
      Document document = index.getDoc(docId);
      double score = 0.0;
      score = BETA
          * (retrieval_results_VSM.get(docId)._score
              + retrieval_results_LM.get(docId)._score
              + retrieval_results_phrase.get(docId)._score + retrieval_results_numviews
                .get(docId)._score);

      retrieval_results.add(new ScoredDocument(docId, document
          .getTitleStr(), score));
    }

    return retrieval_results;
  }
}
