package edu.nyu.cs.cs2580.rankers;

import edu.nyu.cs.cs2580.document.Document;
import edu.nyu.cs.cs2580.document.ScoredDocument;
import edu.nyu.cs.cs2580.index.Indexer;
import edu.nyu.cs.cs2580.index.IndexerInvertedCompressed;
import edu.nyu.cs.cs2580.query.Query;
import edu.nyu.cs.cs2580.query.QueryPhrase;
import edu.nyu.cs.cs2580.SearchEngine.Options;
import edu.nyu.cs.cs2580.handler.CgiArguments;

import java.util.*;

public class RankerConjunctive extends Ranker {
  private IndexerInvertedCompressed indexer;

  public RankerConjunctive(Options options, CgiArguments arguments,
                           Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    // TODO: check type!!!
    System.out.println("Running query in conjunctive ranker...");

    if (_options._indexerType.equals("inverted-doconly")) {
      // Run the query under the doc only indexer
      results = runQueryDoconlyBased(query, numResults);
    } else {
      // Run the query under the occurrence or compressed indexer
      initPosIndexer();
      results = runQueryPosBased(query, numResults);
    }

    return results;
  }

  /**
   * Initialize the indexer supporting nextPos.
   */
  private void initPosIndexer() {
    indexer = (IndexerInvertedCompressed) _indexer;
  }

  /**
   * Query process for query based on doc only indexer
   *
   * @param query
   * @param numResults
   * @return
   */
  private Vector<ScoredDocument> runQueryDoconlyBased(Query query,
                                                      int numResults) {
    Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    String queryType = query.getClass().getSimpleName();
    int docid = -1;

    if (queryType.equals("QueryPhrase")) {
      System.out
          .println("IndexerInvertedDoconly could not resolve Phrase, process as tokens");
    }

    // Get every document which satisfying all the query terms
    while (true) {
      Document doc = _indexer.nextDoc(query, docid);
      // No more available document...
      if (doc == null) {
        break;
      }

      rankQueue.add(new ScoredDocument(doc, 1.0, doc.getPageRank(), doc.getNumViews()));
      if (rankQueue.size() > numResults) {
        rankQueue.poll();
      }

      docid = doc._docid;
    }

    // Get all results...
    while (!rankQueue.isEmpty()) {
      results.add(rankQueue.poll());
    }

    Collections.sort(results, Collections.reverseOrder());

    return results;
  }

  /**
   * Query process for query based on indexer with pos info
   *
   * @param query
   * @param numResults
   * @return
   */
  private Vector<ScoredDocument> runQueryPosBased(Query query, int numResults) {
    Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
    QueryPhrase queryPhrase = (QueryPhrase) query;
    Document doc = null;
    int docid = -1;

    findNextDoc:
    while ((doc = indexer.nextDoc(query, docid)) != null) {
      // System.out.println("Searching Doc: " + doc._docid);
      double score = 0.0;

      if (queryPhrase.containsPhrase) {
        List<List<String>> phrases = queryPhrase.phrases;
        for (List<String> phraseTerms : phrases) {
          int pos = nextPhrase(phraseTerms, doc._docid, -1);
          if (pos == -1) {
            docid = doc._docid;
            continue findNextDoc;
          }
          while (pos != -1) {
            score += 5.0;
            pos = nextPhrase(phraseTerms, doc._docid, pos);
          }
        }
        for (String term : queryPhrase.soloTerms) {
          int termDocFrequency = indexer.documentTermFrequency(term, doc._docid);
          score += 1.0 * (double) termDocFrequency;
        }
      } else {
        for (String term : query._tokens) {
          int termDocFrequency = indexer.documentTermFrequency(term, doc._docid);
          score += 1.0 * (double) termDocFrequency;
        }
      }

      rankQueue.add(new ScoredDocument(doc, score, doc.getPageRank(), doc.getNumViews()));
      if (rankQueue.size() > numResults) {
        rankQueue.poll();
      }
      docid = doc._docid;
    }

    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    ScoredDocument scoredDoc = null;
    while ((scoredDoc = rankQueue.poll()) != null) {
      results.add(scoredDoc);
    }
    Collections.sort(results, Collections.reverseOrder());
    return results;
  }

  private int nextPhrase(List<String> tokens, int docid, int pos) {
    String firstTerm = tokens.get(0);
    findPhrase:
    while ((pos = indexer.nextPos(firstTerm, docid, pos)) != -1) {
      int previousPos = pos;
      for (int i = 1; i < tokens.size(); i++) {
        pos = indexer.nextPos(tokens.get(i), docid, previousPos);
        if (pos == -1) {
          return -1;
        }
        if (pos != previousPos + 1) {
          pos -= i + 1;
          continue findPhrase;
        }
        previousPos++;
      }
      break;
    }
    // if (pos!=-1){
    // System.out.println("Position found in Doc"+ docid +" : " + pos);
    // }
    return pos;
  }
}
