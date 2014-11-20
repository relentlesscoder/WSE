package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

import java.util.*;

public class RankerLinear extends Ranker {
  private final static double BETA_COS = 1.0;
  private final static double BETA_LM = 10.0;
  private final static double BETA_PHRASE = 0.001;
  private final static double BETA_NUMVIEWS = 0.00001;

  public RankerLinear(Options options, CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    Vector<ScoredDocument> retrievalResults = new Vector<ScoredDocument>();
    List<ScoredDocument> scoredDocuments = new ArrayList<ScoredDocument>();

    Ranker VSM = new RankerVSM(_options, _arguments, _indexer);
    Ranker LM = new RankerQL(_options, _arguments, _indexer);
    Ranker phrase = new RankerPhrase(_options, _arguments, _indexer);
    Ranker numviews = new RankerNumViews(_options, _arguments, _indexer);

    for (int docId = 0; docId < _indexer.numDocs(); ++docId) {
      Document document = _indexer.getDoc(docId);
      scoredDocuments.add(new ScoredDocument(document, 0.0));
    }

    addRankerScore(scoredDocuments, VSM.runQuery(query, numResults), BETA_COS);
    addRankerScore(scoredDocuments, LM.runQuery(query, numResults), BETA_LM);
    addRankerScore(scoredDocuments, phrase.runQuery(query, numResults),
        BETA_PHRASE);
    addRankerScore(scoredDocuments, numviews.runQuery(query, numResults),
        BETA_NUMVIEWS);

    // Sort the scoredDocument decreasingly
    Collections.sort(scoredDocuments, new Comparator<ScoredDocument>() {
      @Override
      public int compare(ScoredDocument o1, ScoredDocument o2) {
        return (o2.getScore() > o1.getScore()) ? 1 : (o2.getScore() < o1
            .getScore()) ? -1 : 0;
      }
    });

    int insertCount = Math.min(numResults, scoredDocuments.size());
    for (int i = 0; i < insertCount; i++) {
      retrievalResults.add(scoredDocuments.get(i));
    }

    return retrievalResults;
  }

  private void addRankerScore(List<ScoredDocument> linearRankerResults,
                              List<ScoredDocument> otherRankerResults, double beta) {
    for (int docId = 0; docId < _indexer.numDocs(); ++docId) {
      double originScore = linearRankerResults.get(docId).getScore();
      double newScore = originScore + beta
          * otherRankerResults.get(docId).getScore();
      linearRankerResults.get(docId).setScore(newScore);
    }
  }
}
