package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

import java.util.*;

public class RankerNumViews extends Ranker {
  IndexerInvertedCompressed indexerInvertedCompressed;

  public RankerNumViews(Options options, CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    this.indexerInvertedCompressed = (IndexerInvertedCompressed) this._indexer;
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
    int nextDocid = -1;

    while (true) {
      Document document = indexerInvertedCompressed.nextDoc(query, nextDocid);
      if (document == null) {
        break;
      }

      rankQueue.add(new ScoredDocument(document, document.getNumViews(), document.getPageRank(), document.getNumViews()));
      nextDocid = document._docid;

      if (rankQueue.size() > numResults) {
        rankQueue.poll();
      }
    }

    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    ScoredDocument scoredDoc = null;

    while ((scoredDoc = rankQueue.poll()) != null) {
      results.add(scoredDoc);
    }

    Collections.sort(results, Collections.reverseOrder());

    return results;
  }
}
