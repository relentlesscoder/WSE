package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

public class PhraseRanker implements BaseRanker {

  private Index index;
  private int n_gram = 2;

  public PhraseRanker(Index index) {
    this.index = index;
  }

  @Override
  public Vector<ScoredDocument> runQuery(String query) {
    Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();

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
      Vector<String> queryVector = new Vector<String>();
      while (scanner.hasNext()) {
        String term = scanner.next();
        queryVector.add(term);
      }

      Document document = index.getDoc(docId);
      List<String> titleVector = document.getTitleList();
      List<String> bodyVector = document.getBodyList();

      // n-gram equal to the query size if the query size is less than n-gram
      n_gram = n_gram > queryVector.size() ? queryVector.size() : n_gram;

      // generate the n-gram vector for query, document title and document body
      List<String> nGramQueryVector = nGramGenerator(queryVector, n_gram);
      List<String> nGramTitleVector = nGramGenerator(titleVector, n_gram);
      List<String> nGramBodyVector = nGramGenerator(bodyVector, n_gram);

      double score = 0.0;
      for (int i = 0; i < nGramQueryVector.size(); ++i) {
        // Scan title
        for (int j = 0; j < nGramTitleVector.size(); ++j) {
          if (nGramQueryVector.get(i).equals(nGramTitleVector.get(j))) {
            score += 1.0;
          }
        }
        // Scan body
        for (int j = 0; j < nGramBodyVector.size(); ++j) {
          if (nGramQueryVector.get(i).equals(nGramBodyVector.get(j))) {
            score += 1.0;
          }
        }
      }

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

  // n-gram vector generator
  private List<String> nGramGenerator(List<String> content, int n_gram) {
    List<String> nGramVector = new ArrayList<String>();
    for (int i = 0; i <= content.size() - n_gram; i++) {
      StringBuilder sb = new StringBuilder();
      for (int j = i; j < i + n_gram; j++) {
        sb.append(content.get(j)).append(" ");
      }
      nGramVector.add(sb.substring(0, sb.length() - 1));
    }
    return nGramVector;
  }

}
