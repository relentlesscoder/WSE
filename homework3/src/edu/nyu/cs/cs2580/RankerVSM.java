package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

import java.util.*;

public class RankerVSM extends Ranker {

  public RankerVSM(Options options, CgiArguments arguments,
                   Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {

    Vector<ScoredDocument> retrievalResults = new Vector<ScoredDocument>();
    List<ScoredDocument> scoredDocuments = new ArrayList<ScoredDocument>();

    for (int i = 0; i < _indexer.numDocs(); ++i) {
      scoredDocuments.add(scoreDocument(query, i));
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

  public ScoredDocument scoreDocument(Query query, int docId) {
    ScoredDocument scoredDocument = null;

    List<String> tokens = query.terms;
    // <Term, Term frequency>
    Map<String, Integer> queryMap = new HashMap<String, Integer>();

    for (String term : tokens) {
      if (queryMap.containsKey(term)) {
        queryMap.put(term, queryMap.get(term) + 1);
      } else {
        queryMap.put(term, 1);
      }
    }

    DocumentFull document = (DocumentFull) _indexer.getDoc(docId);

    double score = cosineSimilarity(document, queryMap);

    scoredDocument = new ScoredDocument(document, score);

    return scoredDocument;
  }

  private double cosineSimilarity(DocumentFull document,
                                  Map<String, Integer> queryMap) {
    int numOfDocTerms = document.getConvertedBodyTokens().size()
        + document.getConvertedTitleTokens().size();
    List<String> allTerms = new ArrayList<String>();
    allTerms.addAll(document.getConvertedBodyTokens());
    allTerms.addAll(document.getConvertedTitleTokens());

    // <term, frequency> of a document
    Map<String, Double> termFrequencyMap = new HashMap<String, Double>();

    for (String s : allTerms) {
      if (termFrequencyMap.containsKey(s)) {
        termFrequencyMap.put(s, termFrequencyMap.get(s) + 1.0);
      } else {
        termFrequencyMap.put(s, 1.0);
      }
    }

    double score = 0.0;
    double d_j = 0.0;
    double q_j = 0.0;
    double a = 0.0;
    double b = 0.0;
    double c = 0.0;

    // Calculate the numerator a and part of the denominator c
    for (Map.Entry entry : queryMap.entrySet()) {
      String query = (String) entry.getKey();
      int queryFrequency = (Integer) entry.getValue();

      q_j = getQueryTermWeights(query, queryFrequency);

      if (termFrequencyMap.containsKey(query)) {
        d_j = getDocumentTermWeights(termFrequencyMap, numOfDocTerms, query);
        a += d_j * q_j;
      }

      c += q_j * q_j;
    }

    // Calculate the part of denominator b
    for (Map.Entry entry : termFrequencyMap.entrySet()) {
      String s = (String) entry.getKey();
      d_j = getDocumentTermWeights(termFrequencyMap, numOfDocTerms, s);
      b += d_j * d_j;
    }

    if (b * c != 0) {
      score = a / Math.sqrt(b * c);
    }

    return score;
  }

  private double getQueryTermWeights(String term, int termFrequency) {
    double tf_ik = termFrequency;
    double idf_k = getInverseDocumentFrequency(term);

    return tf_ik * idf_k;
  }

  private double getDocumentTermWeights(Map<String, Double> termFrequencyMap,
                                        int numOfDocTerms, String term) {
    double tf_ik = getTermFrequencyWeight(termFrequencyMap, numOfDocTerms, term);
    double idf_k = getInverseDocumentFrequency(term);

    return tf_ik * idf_k;
  }

  private double getTermFrequencyWeight(Map<String, Double> termFrequencyMap,
                                        int numOfDocTerms, String term) {
    double tf_ik = 0.0;
    double f_ik = termFrequencyMap.get(term);

    tf_ik = f_ik / numOfDocTerms;

    return tf_ik;
  }

  private double getInverseDocumentFrequency(String term) {
    double N = _indexer.numDocs();
    double n_t = _indexer.corpusDocFrequencyByTerm(term);

    return Math.log(N / n_t);
  }
}
