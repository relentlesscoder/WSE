package edu.nyu.cs.cs2580;

import java.util.Scanner;
import java.util.Vector;

class Ranker implements BaseRanker {
  private Index index;

  public Ranker(Index index) {
    this.index = index;
  }

  public Vector<ScoredDocument> runQuery(String query) {

    Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();
    for (int i = 0; i < index.numDocs(); ++i) {
      retrieval_results.add(scoreDocument(query, i));
    }

    return retrieval_results;
  }

  public ScoredDocument scoreDocument(String query, int did) {

    ScoredDocument scoredDocument = null;
    // Build query vector
    Scanner scanner = null;
    try {
      scanner = new Scanner(query);
      Vector<String> queryVector = new Vector<String>();
      while (scanner.hasNext()) {
        String term = scanner.next();
        queryVector.add(term);
      }

      // Get the document vector. For hw1, you don't have to worry about the
      // details of how index works.
      Document document = index.getDoc(did);
      Vector<String> documentVector = document.get_title_vector();

      // Score the document. Here we have provided a very simple ranking model,
      // where a document is scored 1.0 if it gets hit by at least one query
      // term.
      double score = 0.0;
      for (int i = 0; i < documentVector.size(); ++i) {
        for (int j = 0; j < queryVector.size(); ++j) {
          if (documentVector.get(i).equals(queryVector.get(j))) {
            score = 1.0;
            break;
          }
        }
      }
      scoredDocument = new ScoredDocument(did, document.get_title_string(),
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
