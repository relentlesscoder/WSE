package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

import java.util.*;

public class LanguageModel extends Ranker {

  private final static double LAMDA = 0.50;

  public LanguageModel(Options options, CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    Vector<ScoredDocument> retrievalResults = new Vector<ScoredDocument>();
    List<ScoredDocument> scoredDocuments = new ArrayList<ScoredDocument>();

    for (int docId = 0; docId < _indexer.numDocs(); docId++) {
      scoredDocuments.add(scoreDocument(query, docId));
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
    // C is the total number of word occurrences in the collection.
    long C = _indexer.totalTermFrequency();

    // Query vector
    List<String> queryList = new ArrayList<String>();
    for (String term : query.terms) {
      queryList.add(term);
    }

    DocumentFull document = (DocumentFull) _indexer.getDoc(docId);
    List<String> titleVectorVector = document.getConvertedTitleTokens();
    List<String> bodyVector = document.getConvertedBodyTokens();

    // Score the document. Here we have provided a very simple ranking model,
    // where a document is scored 1.0 if it gets hit by at least one query
    // term.
    double score = 0.0;

    for (int i = 0; i < queryList.size(); ++i) {
      String qi = queryList.get(i);

      // fqi_D is the number of times word qi occurs in document D.
      int fqi_D = 0;
      // cqi is the number of times a query word occurs in the collection of
      // documents
      int cqi = _indexer.corpusDocFrequencyByTerm(qi);
      // D is the number of words in D.
      double D = titleVectorVector.size() + bodyVector.size();

      // Calculate fqi_D
      for (int j = 0; j < titleVectorVector.size(); ++j) {
        if (titleVectorVector.get(j).equals(qi)) {
          fqi_D++;
        }
      }
      // Calculate fqi_D
      for (int j = 0; j < bodyVector.size(); ++j) {
        if (bodyVector.get(j).equals(qi)) {
          fqi_D++;
        }
      }

      score += Math.log((1 - LAMDA) * (fqi_D / D) + LAMDA * (cqi / C));
    }

    // TODO: Not sure...
    score = Math.exp(score);

    scoredDocument = new ScoredDocument(document, score);

    return scoredDocument;
  }
}
