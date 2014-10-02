package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

public class LanguageModel implements BaseRanker {

  private Index index;
  private final static double LAMDA = 0.50;

  public LanguageModel(Index index) {
    this.index = index;
  }

  @Override
  public List<ScoredDocument> runQuery(String query) {
    List<ScoredDocument> retrieval_results = new ArrayList<ScoredDocument>();

    for (int docId = 0; docId < index.numDocs(); docId++) {
      retrieval_results.add(scoreDocument(query, docId));
    }

    return retrieval_results;
  }

  public ScoredDocument scoreDocument(String query, int docId) {
    ScoredDocument scoredDocument = null;
    // C is the total number of word occurrences in the collection.
    int C = index.termFrequency();

    Scanner scanner = null;
    try {
      scanner = new Scanner(query);
      // Query vector
      Vector<String> Q = new Vector<String>();
      while (scanner.hasNext()) {
        String term = scanner.next();
        Q.add(term);
      }

      Document document = index.getDoc(docId);
      List<String> titleVectorVector = document.getTitleList();
      List<String> bodyVector = document.getBodyList();

      // Score the document. Here we have provided a very simple ranking model,
      // where a document is scored 1.0 if it gets hit by at least one query
      // term.
      double score = 0.0;

      for (int i = 0; i < Q.size(); ++i) {
        String qi = Q.get(i);

        // fqi_D is the number of times word qi occurs in document D.
        int fqi_D = 0;
        // cqi is the number of times a query word occurs in the collection of
        // documents
        int cqi = index.documentFrequency(qi);
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

      scoredDocument = new ScoredDocument(docId, document.getTitleStr(),
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
