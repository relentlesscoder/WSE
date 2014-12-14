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

public class RankerPhrase extends Ranker {

  private int n_gram = 2;
  private IndexerInvertedCompressed compIndexer;

  public RankerPhrase(Options options, CgiArguments arguments,
                      Indexer indexer) {
    super(options, arguments, indexer);
    this.compIndexer = (IndexerInvertedCompressed) this._indexer;
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {

    Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
    QueryPhrase queryPhrase = (QueryPhrase) query;
    Document doc = null;
    int docid = -1;

    findNextDoc:
    while ((doc = compIndexer.nextDoc(query, docid)) != null) {
      // System.out.println("Searching Doc: " + doc._docid);
      double score = 0.0;
      List<String> terms = new ArrayList<String>(query._tokens);
      for (int i = 0; i < terms.size() - n_gram + 1; i++) {
        ArrayList<String> phraseTerms = new ArrayList<String>();
        for (int j = 0; j < n_gram; j++) {
          phraseTerms.add(terms.get(i + j));
        }
        int pos = nextPhrase(phraseTerms, doc._docid, -1);
        if (pos == -1) {
          continue;
        }
        while (pos != -1) {
          score += 1.0;
          pos = nextPhrase(phraseTerms, doc._docid, pos);
        }
      }
      if (queryPhrase.containsPhrase) {
        List<List<String>> phrases = queryPhrase.phrases;
        for (List<String> phraseTerms : phrases) {
          int pos = nextPhrase(phraseTerms, doc._docid, -1);
          if (pos == -1) {
            continue;
          }
          while (pos != -1) {
            score += 3.0;
            pos = nextPhrase(phraseTerms, doc._docid, pos);
          }
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

  private int nextPos(String term, int docid, int pos) {
    int result = pos;

    result = compIndexer.nextPos(term, docid, pos);

    return result;
  }

  private int nextPhrase(List<String> tokens, int docid, int pos) {
    String firstTerm = tokens.get(0);
    findPhrase:
    while ((pos = nextPos(firstTerm, docid, pos)) != -1) {
      int previousPos = pos;
      for (int i = 1; i < tokens.size(); i++) {
        pos = nextPos(tokens.get(i), docid, previousPos);
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

  private int documentTermFrequency(String term, int docid) {
    int result = 0;
    result = compIndexer.documentTermFrequency(term, docid);

    return result;
  }

}


