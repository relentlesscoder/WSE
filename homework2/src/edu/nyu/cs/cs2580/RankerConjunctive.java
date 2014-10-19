package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * Instructors' code for illustration purpose. Non-tested code.
 * 
 * @author congyu
 */
public class RankerConjunctive extends Ranker {

  public RankerConjunctive(Options options,
      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    String indexType = _indexer.getClass().getSimpleName();
    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    if (indexType.equals("IndexerInvertedDoconly")){
      results = runQueryDoconlyBased(query,numResults);
    }else if (indexType.equals("IndexerInvertedOccurrence")){
      results = runQueryPhraseOccBased(query,numResults);
    }else if (indexType.equals("IndexerInvertedCompressed")){
      results = runQueryPhraseCompBased(query,numResults);
    }
    return results;
  }

  //Query process for query based on Doconly indexer
  private Vector<ScoredDocument> runQueryDoconlyBased (Query query, int numResults){
    String queryType = query.getClass().getSimpleName();
    if (queryType.equals("QueryPhrase")){
      System.out.println("IndexerInvertedDoconly could not resolve Phrase, process as tokens");
    }
    Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
    Document doc = null;
    int docid = -1;
    while ((doc = _indexer.nextDoc(query, docid)) != null) {
      rankQueue.add(new ScoredDocument(doc, 1.0));
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

  //Query process for query based on Occurrence indexer
  private Vector<ScoredDocument> runQueryPhraseOccBased (Query query, int numResults){
    IndexerInvertedOccurrence occIndexer = (IndexerInvertedOccurrence)_indexer;
    String queryType = query.getClass().getSimpleName();
    QueryPhrase queryPhrase;

    Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
    Document doc = null;
    int docid = -1;
    while ((doc = occIndexer.nextDoc(query, docid)) != null) {
      if (queryType.equals("QueryPhrase")){
        queryPhrase = (QueryPhrase)query;
      }
      double score = 0.0;
      for(String term : query._tokens){
        int pos = occIndexer.nextPos(term, doc._docid, -1);
        while (pos!=-1) {
          score+=1.0;
          pos = occIndexer.nextPos(term, doc._docid, pos);
        }
      }
      System.out.println(doc._docid+" "+score);
      rankQueue.add(new ScoredDocument(doc, score));
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

  //Query process for query based on Compressed indexer
  private Vector<ScoredDocument> runQueryPhraseCompBased (Query query, int numResults){
    IndexerInvertedCompressed compIndexer = (IndexerInvertedCompressed)_indexer;
    String queryType = query.getClass().getSimpleName();
    QueryPhrase queryPhrase;
    Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();

    Document doc = null;
    int docid = -1;
    while ((doc = compIndexer.nextDoc(query, docid)) != null) {
      double score = 0.0;
//      if (queryType.equals("QueryPhrase")){
//        queryPhrase = (QueryPhrase)query;
//        //If there is no phrase in the document skip current loop
//        int pos = nextPhrase(queryPhrase, docid, -1);
//        if (pos == -1) continue;
//        while (pos != -1){
//          score += 1.0;
//          pos = nextPhrase(queryPhrase, docid, -1);
//        }
//        // add solo term
//      }else{
        for(String term : query._tokens){
          int pos = compIndexer.nextPos(term,doc._docid,-1);
          while (pos!=-1) {
            score+=1.0;
            pos = compIndexer.nextPos(term, doc._docid, pos);
            System.out.println(pos);
          }
        }
        System.out.println(doc._docid+" "+score);
        rankQueue.add(new ScoredDocument(doc, score));
        if (rankQueue.size() > numResults) {
          rankQueue.poll();
        }
        docid = doc._docid;
//      }
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
