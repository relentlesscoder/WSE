package edu.nyu.cs.cs2580.rankers;

import com.google.common.collect.Multiset;
import edu.nyu.cs.cs2580.document.Document;
import edu.nyu.cs.cs2580.document.ScoredDocument;
import edu.nyu.cs.cs2580.index.Indexer;
import edu.nyu.cs.cs2580.index.IndexerInvertedCompressed;
import edu.nyu.cs.cs2580.spellCheck.SpellCheckResult;
import edu.nyu.cs.cs2580.query.Query;
import edu.nyu.cs.cs2580.SearchEngine.Options;
import edu.nyu.cs.cs2580.handler.CgiArguments;

import java.util.*;

public class RankerCosine extends Ranker {
  IndexerInvertedCompressed indexerInvertedCompressed;

  public RankerCosine(Options options, CgiArguments arguments,
                      Indexer indexer) {
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

  public ScoredDocument scoreDocument(Query query, int docId) {
    ScoredDocument scoredDocument = null;

    List<String> tokens = new ArrayList<String>(query._tokens);
    // <Term, Term frequency>
    Map<String, Integer> queryMap = new HashMap<String, Integer>();

    for (String term : tokens) {
      if (queryMap.containsKey(term)) {
        queryMap.put(term, queryMap.get(term) + 1);
      } else {
        queryMap.put(term, 1);
      }
    }

    double score = cosineSimilarity(docId, queryMap);

    Document document = indexerInvertedCompressed.getDoc(docId);
    scoredDocument = new ScoredDocument(document, score, document.getPageRank(), document.getNumViews());
    return scoredDocument;
  }

  private double cosineSimilarity(int docid, Map<String, Integer> queryMap) {
    Multiset<Integer> docTermFrequency = indexerInvertedCompressed.getDocidTermFrequency(docid);
    int numOfDocTerms = docTermFrequency.size();

    double score = 0.0;
    double d_j;
    double q_j;
    double a = 0.0;
    double b = 0.0;
    double c = 0.0;

    // Calculate the numerator a and part of the denominator c
    for (String queryTerm : queryMap.keySet()) {
      int queryTermId = indexerInvertedCompressed.getTermId(queryTerm);
      int queryFrequency = queryMap.get(queryTerm);

      q_j = getQueryTermWeights(queryTermId, queryFrequency);

      if (docTermFrequency.contains(queryTermId)) {
        d_j = getDocumentTermWeights(docTermFrequency, numOfDocTerms, queryTermId);
        a += d_j * q_j;
      }

      c += q_j * q_j;
    }

    // Calculate the part of denominator b
    for (int termId : docTermFrequency.elementSet()) {
      d_j = getDocumentTermWeights(docTermFrequency, numOfDocTerms, termId);
      b += d_j * d_j;
    }

    if (b * c != 0) {
      score = a / Math.sqrt(b * c);
    }

    return score;
  }

  private double getQueryTermWeights(int queryTermId, int termFrequency) {
    double tf_ik = termFrequency;
    double idf_k = getInverseDocumentFrequency(queryTermId);

    return tf_ik * idf_k;
  }

  private double getDocumentTermWeights(Multiset<Integer> docTermFrequency,
                                        int numOfDocTerms, int termId) {
    double tf_ik = getTermFrequencyWeight(docTermFrequency, numOfDocTerms, termId);
    double idf_k = getInverseDocumentFrequency(termId);

    return tf_ik * idf_k;
  }

  private double getTermFrequencyWeight(Multiset<Integer> docTermFrequency,
                                        int numOfDocTerms, int termId) {
    double tf_ik = 0.0;
    double f_ik = docTermFrequency.count(termId);

    tf_ik = f_ik / numOfDocTerms;

    return tf_ik;
  }

  private double getInverseDocumentFrequency(int termId) {
    String term = indexerInvertedCompressed.getTermById(termId);
    double N = _indexer.numDocs();
    double n_t = indexerInvertedCompressed.corpusDocFrequencyByTerm(term);

    return Math.log(N / n_t);
  }
}
