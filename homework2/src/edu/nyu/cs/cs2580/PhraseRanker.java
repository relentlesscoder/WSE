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
  public List<ScoredDocument> runQuery(String query) {
    List<ScoredDocument> retrieval_results = new ArrayList<ScoredDocument>();

    for (int docId = 0; docId < index.numDocs(); docId++) {
      retrieval_results.add(scoreDocument(query, docId));
    }

    return retrieval_results;
  }

  public ScoredDocument scoreDocument(String query, int docId) {
    ScoredDocument scoredDocument = null;

    Scanner scanner = null;
    try {
      scanner = new Scanner(query);
      // Query vector
      List<String> queryList = new ArrayList<String>();
      while (scanner.hasNext()) {
        String term = scanner.next();
        queryList.add(term);
      }

      Document document = index.getDoc(docId);
      List<String> titleVector = document.getTitleList();
      List<String> bodyVector = document.getBodyList();

      // n-gram equal to the query size if the query size is less than n-gram
      n_gram = n_gram > queryList.size() ? queryList.size() : n_gram;

      // generate the n-gram vector for query, document title and document body
      List<String> nGramQueryList = nGramGenerator(queryList, n_gram);
      List<String> nGramTitleList = nGramGenerator(titleVector, n_gram);
      List<String> nGramBodyList = nGramGenerator(bodyVector, n_gram);

      double score = 0.0;
      for (int i = 0; i < nGramQueryList.size(); ++i) {
        // Scan title
        for (int j = 0; j < nGramTitleList.size(); ++j) {
          if (nGramQueryList.get(i).equals(nGramTitleList.get(j))) {
            score += 1.0;
          }
        }
        // Scan body
        for (int j = 0; j < nGramBodyList.size(); ++j) {
          if (nGramQueryList.get(i).equals(nGramBodyList.get(j))) {
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
    List<String> nGramList = new ArrayList<String>();
    for (int i = 0; i <= content.size() - n_gram; i++) {
      StringBuilder sb = new StringBuilder();
      for (int j = i; j < i + n_gram; j++) {
        sb.append(content.get(j)).append(" ");
      }
      nGramList.add(sb.substring(0, sb.length() - 1));
    }
    return nGramList;
  }

}
