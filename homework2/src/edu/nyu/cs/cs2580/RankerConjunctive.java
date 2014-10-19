package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.util.*;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * Instructors' code for illustration purpose. Non-tested code.
 * 
 * @author congyu
 */
public class RankerConjunctive extends Ranker {

  private IndexerInvertedCompressed compIndexer;
  private IndexerInvertedOccurrence occIndexer;

  public RankerConjunctive(Options options,
      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    String indexType = _indexer.getClass().getSimpleName();
    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    if (_options._indexerType.equals("inverted-doconly")){
      results = runQueryDoconlyBased(query,numResults);
    }else if (_options._indexerType.equals("inverted-occurrence")){
      initPosIndexer();
      results = runQueryPosBased(query,numResults);
    }else if (_options._indexerType.equals("inverted-compressed")){
      initPosIndexer();
      results = runQueryPosBased(query,numResults);
    }
    return results;
  }

  private void initPosIndexer(){
    if (_options._indexerType.equals("inverted-occurrence")){
      occIndexer = (IndexerInvertedOccurrence) _indexer;
    }else if(_options._indexerType.equals("inverted-compressed")){
      compIndexer = (IndexerInvertedCompressed) _indexer;
    }
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

  //Query process for query based on indexer with pos info
  private Vector<ScoredDocument> runQueryPosBased (Query query, int numResults){
    String queryType = query.getClass().getSimpleName();
    QueryPhrase queryPhrase;
    Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();

    Document doc = null;
    int docid = -1;
    findDoc:
    while ((doc = nextDoc(query, docid)) != null) {
      double score = 0.0;
      if (queryType.equals("QueryPhrase")){
        queryPhrase = (QueryPhrase)query;
        //If there is no phrase in the document skip current loop
        ListMultimap<String, String> phrases = queryPhrase._phrases;
        Set<String> keyset = phrases.keySet();

        for(String key : keyset){
          List<String> tokens = phrases.get(key);
          int pos = nextPhrase(tokens, docid, -1);
          if (pos == -1) continue findDoc;
          while (pos != -1){
            score += 1.0;
            pos = nextPhrase(tokens, docid, -1);
          }
        }
      }

      for(String term : query._tokens){
        int pos = nextPos(term, doc._docid, -1);
        while (pos!=-1) {
          score+=1.0;
          pos = nextPos(term, doc._docid, pos);
          System.out.println(pos);
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

  private Document nextDoc(Query query, int docid){
    Document doc = null;
    if (_options._indexerType.equals("inverted-occurrence")){
      doc = occIndexer.nextDoc(query, docid);
    }else if(_options._indexerType.equals("inverted-compressed")){
      doc = compIndexer.nextDoc(query,docid);
    }
    return doc;
  }

  private int nextPos(String term, int docid, int pos){
    int result = pos;
    if (_options._indexerType.equals("inverted-occurrence")){
      result = occIndexer.nextPos(term, docid, pos);
    }else if(_options._indexerType.equals("inverted-compressed")){
      result = compIndexer.nextPos(term, docid, pos);
    }
    return result;
  }

  private int nextPhrase(List<String> tokens, int docid, int pos){
    int docid = nextDoc();

    return pos;
  }
}
