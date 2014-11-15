package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

import java.util.*;

public class PhraseRanker extends Ranker {

  private int n_gram = 2;

  public PhraseRanker(Options options, CgiArguments arguments, Indexer indexer) {
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

    // Query vector
    List<String> queryList = new ArrayList<String>();
    for (String term : query.terms) {
      queryList.add(term);
    }

    DocumentFull document = (DocumentFull) _indexer.getDoc(docId);
    List<String> titleVector = document.getConvertedTitleTokens();
    List<String> bodyVector = document.getConvertedBodyTokens();

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

    scoredDocument = new ScoredDocument(document, score);

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
