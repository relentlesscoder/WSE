package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

import java.util.*;

public class RankerQL extends Ranker {
  private final static double LAMDA = 0.50;
  IndexerInvertedCompressed indexerInvertedCompressed;

  public RankerQL(Options options, CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    this.indexerInvertedCompressed = (IndexerInvertedCompressed) this._indexer;
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    System.out.println("Running query...");
    Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
    int nextDocid = -1;

    while (true) {
      Document document = indexerInvertedCompressed.nextDoc(query,
          nextDocid);
      if (document == null) {
        break;
      }

      rankQueue.add(scoreDocument(query, document._docid));
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

  /**
   * Score the document...
   * @param query the query
   * @param docId document ID
   * @return a ScoreDocument
   */
  public ScoredDocument scoreDocument(Query query, int docId) {
    ScoredDocument scoredDocument = null;
    // C is the total number of word occurrences in the collection.
    long C = indexerInvertedCompressed.totalTermFrequency();

    // Query vector
    List<String> queryList = new ArrayList<String>();
    for (String term : query._tokens) {
      queryList.add(term);
    }

    DocumentIndexed document = indexerInvertedCompressed.getDoc(docId);

    // Score the document. Here we have provided a very simple ranking model,
    // where a document is scored 1.0 if it gets hit by at least one query
    // term.
    double score = 0.0;

    for (int i = 0; i < queryList.size(); ++i) {
      String qi = queryList.get(i);

      // fqi_D is the number of times word qi occurs in document D.
      int fqi_D = indexerInvertedCompressed.documentTermFrequency(qi,
          document._docid);
      // cqi is the number of times a query word occurs in the collection of
      // documents
      int cqi = indexerInvertedCompressed.corpusDocFrequencyByTerm(qi);
      // D is the number of words in D.
      double D = document.getTotalDocTerms();

      score += Math.log((1 - LAMDA) * (fqi_D / D) + LAMDA * (cqi / C));
    }

    score = Math.exp(score);

    scoredDocument = new ScoredDocument(document, score, document.getPageRank(), document.getNumViews());

    return scoredDocument;
  }
}
