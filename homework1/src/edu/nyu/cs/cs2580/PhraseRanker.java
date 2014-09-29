package edu.nyu.cs.cs2580;

import java.util.Comparator;
import java.util.Scanner;
import java.util.Vector;

public class PhraseRanker implements BaseRanker {

  private Index _index;
  private n_gram = 2;

  public PhraseRanker(String index_source) {
    _index = new Index(index_source);
  }

  @Override
  public Vector<ScoredDocument> runQuery(String query) {
    Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();

    for (int docId = 0; docId < _index.numDocs(); docId++) {
      retrieval_results.add(scoreDocument(query, docId));
    }

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
      Vector<String> queryVector = new Vector<String>();
      while (scanner.hasNext()) {
        String term = scanner.next();
        queryVector.add(term);
      }

      Document document = _index.getDoc(docId);
      Vector<String> titleVector = document.get_title_vector();
      Vector<String> bodyVector = document.get_body_vector();

      // n-gram equal to the query size if the query size is less than n-gram
      n_gram = n_gram>queryVector.size()? queryVector.size() : n_gram; 
      
      // generate the n-gram vector for query, document title and document body
      Vector<String> nGramQueryVector = nGramVector(queryVector,n_gram); 
      Vector<String> nGramTitleVector = nGramVector(titleVector,n_gram);
      Vector<String> nGramBodyVector = nGramVector(bodyVector,n_gram);

	  double score = 0.0;
	  for (int i = 0; i < nGramQueryVector.size(); ++i) {
	  	// Scan title
        for (int j = 0; j < nGramTitleVector.size(); ++j) {
          if (nGramQueryVector.get(i).equals(nGramTitleVector.get(j))) {
            score += 1.0;
          }
        }
        // Scan body
        for (int j = 0; j < nGramBodyVector.size(); ++j){
          if (nGramQueryVector.get(i).equals(nGramBodyVector.get(j))) {
            score += 1.0;
          }
        }
      }

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
  private Vector<String> nGramGenerator(Vector<String> content, int n_gram){
  	Vector nGramVector = new Vector<String>();
  	for (int i=0; i<=content.size()-n; i++){
		StringBuilder sb = new StringBuilder();
		for(int j=i; j<i+n; j++){
			System.out.println(j);
			sb.append(content.get(j)).append(" ");
		}
		nGramVector.add(sb.substring(0,sb.length()-1));
	}
	return nGramVector;
  }

}
