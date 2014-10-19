package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

public class NumviewsRanker extends Ranker {

  public NumviewsRanker(Options options, CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    Vector<ScoredDocument> retrievalResults = new Vector<ScoredDocument>();
    List<ScoredDocument> scoredDocuments = new ArrayList<ScoredDocument>();

    for (int docId = 0; docId < _indexer.numDocs(); ++docId) {
      Document document = _indexer.getDoc(docId);
      scoredDocuments.add(new ScoredDocument(document, document.getNumViews()));
    }

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
}
