package edu.nyu.cs.cs2580;

import java.util.Comparator;
import java.util.Scanner;
import java.util.Vector;

public class LanguageModel implements BaseRanker {

  private Index _index;
  private final static double LAMDA = 0.50;

  public LanguageModel(String index_source) {
    _index = new Index(index_source);
  }

  @Override
  public Vector<ScoredDocument> runQuery(String query) {
    Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();

    for (int docId = 0; docId < _index.numDocs(); docId++) {
      retrieval_results.add(scoreDocument(query, docId));
    }
    /*
    retrieval_results.sort(new Comparator<ScoredDocument>() {
      @Override
      public int compare(ScoredDocument o1, ScoredDocument o2) {
        if (o1._score > o2._score) {
          return -1;
        } else if (o1._score < o2._score) {
          return 1;
        } else {
          return 0;
        }
      }
    });
    */
    return retrieval_results;
  }

  public ScoredDocument scoreDocument(String query, int docId) {
    ScoredDocument scoredDocument = null;
    // C is the total number of word occurrences in the collection.
    int C = _index.termFrequency();

    Scanner scanner = null;
    try {
      scanner = new Scanner(query);
      // Query vector
      Vector<String> Q = new Vector<String>();
      while (scanner.hasNext()) {
        String term = scanner.next();
        Q.add(term);
      }

      Document document = _index.getDoc(docId);
      Vector<String> titleVectorVector = document.get_title_vector();
      Vector<String> bodyVector = document.get_body_vector();

      // Score the document. Here we have provided a very simple ranking model,
      // where a document is scored 1.0 if it gets hit by at least one query
      // term.
      double score = 0.0;

      for (int i = 0; i < Q.size(); ++i) {
        String qi = Q.get(i);

        // fqi_D is the number of times word qi occurs in document D.
        int fqi_D = 0;
        // cqi is the number of times a query word occurs in the collection of documents
        int cqi = _index.documentFrequency(qi);
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

      scoredDocument = new ScoredDocument(docId, document.get_title_string(),
          score);
    } catch (Exception e) {
      // TODO: handle exception
    } finally {
      if (scanner != null) {
        scanner.close();
      }
    }


    return scoredDocument;
  }
}
