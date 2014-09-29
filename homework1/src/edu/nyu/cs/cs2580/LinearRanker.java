package edu.nyu.cs.cs2580;

import java.util.Vector;

public class LinearRanker implements BaseRanker {
  private Index _index;
  private final static double BETA = 0.50;

  public LinearRanker(String index_source) {
    _index = new Index(index_source);
  }

  @Override
  public Vector<ScoredDocument> runQuery(String query) {
    Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();
    Vector<ScoredDocument> retrieval_results_VSM;
    Vector<ScoredDocument> retrieval_results_LM;
    Vector<ScoredDocument> retrieval_results_phrase;
    Vector<ScoredDocument> retrieval_results_numviews;

    BaseRanker VSM = new VectorSpaceModel(_index);
    retrieval_results_VSM = VSM.runQuery(query);

    BaseRanker LM = new LanguageModel(_index);
    retrieval_results_LM = LM.runQuery(query);

    BaseRanker phrase = new PhraseRanker(_index);
    retrieval_results_phrase = phrase.runQuery(query);

    BaseRanker numviews = new NumviewsRanker(_index);
    retrieval_results_numviews = numviews.runQuery(query);

    for (int docId = 0; docId < _index.numDocs(); ++docId) {
      Document document = _index.getDoc(docId);
      double score = 0.0;
      score = BETA * (retrieval_results_VSM.get(docId)._score
          + retrieval_results_LM.get(docId)._score
          + retrieval_results_phrase.get(docId)._score
          + retrieval_results_numviews.get(docId)._score);

      retrieval_results
          .add(new ScoredDocument(docId, document.get_title_string(), score));
    }

    return retrieval_results;
  }
}
