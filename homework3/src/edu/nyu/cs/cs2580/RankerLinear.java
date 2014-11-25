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
    Map<Integer, ScoredDocument> scoredDocumentsMap = new HashMap<Integer, ScoredDocument>();

    Ranker VSM = new RankerCosine(_options, _arguments, _indexer);
    Ranker LM = new RankerQL(_options, _arguments, _indexer);
    Ranker phrase = new RankerPhrase(_options, _arguments, _indexer);
    Ranker numviews = new RankerNumViews(_options, _arguments, _indexer);

    addRankerScore(scoredDocumentsMap, VSM.runQuery(query, numResults), BETA_COS);
    addRankerScore(scoredDocumentsMap, LM.runQuery(query, numResults), BETA_LM);
    addRankerScore(scoredDocumentsMap, phrase.runQuery(query, numResults),
        BETA_PHRASE);
    addRankerScore(scoredDocumentsMap, numviews.runQuery(query, numResults),
        BETA_NUMVIEWS);

    Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
    for (ScoredDocument scoredDocument : scoredDocumentsMap.values()) {
      rankQueue.add(scoredDocument);
      if (rankQueue.size() > numResults) {
        rankQueue.poll();
      }
    }

    ScoredDocument scoredDoc = null;
    while ((scoredDoc = rankQueue.poll()) != null) {
      scoredDocuments.add(scoredDoc);
    }

    // Sort the scoredDocument decreasingly
    Collections.sort(scoredDocuments, Collections.reverseOrder());

    // TODO: Fix later...
    retrievalResults.addAll(scoredDocuments);

    return retrievalResults;
  }

  private void addRankerScore(Map<Integer, ScoredDocument> scoredDocumentsMap,
                              List<ScoredDocument> otherRankerResults, double beta) {
    for (ScoredDocument scoredDocument : otherRankerResults) {
      int docid = scoredDocument.getDocid();
      if (scoredDocumentsMap.containsKey(docid)) {
        double originScore = scoredDocumentsMap.get(docid).getScore();
        double newScore = originScore + beta * scoredDocument.getScore();
        scoredDocumentsMap.get(docid).setScore(newScore);
      } else {
        scoredDocumentsMap.put(docid, scoredDocument);
        double newScore = beta * scoredDocument.getScore();
        scoredDocumentsMap.get(docid).setScore(newScore);
      }
    }
  }
}
