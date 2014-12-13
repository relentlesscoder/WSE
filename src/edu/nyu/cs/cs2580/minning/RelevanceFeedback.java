package edu.nyu.cs.cs2580.minning;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import edu.nyu.cs.cs2580.query.Query;
import edu.nyu.cs.cs2580.handler.TermPrf;
import edu.nyu.cs.cs2580.tokenizer.Tokenizer;
import edu.nyu.cs.cs2580.document.ScoredDocument;
import edu.nyu.cs.cs2580.Index.IndexerInvertedCompressed;
import edu.nyu.cs.cs2580.handler.CgiArguments;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by tanis on 11/18/14.
 */
public class RelevanceFeedback {
//  // Options to configure each concrete Ranker.
//  protected SearchEngine.Options _options;
//  // CGI arguments user provide through the URL.
//  protected QueryHandler.CgiArguments _arguments;
//
//  // The Indexer via which documents are retrieved, see {@code IndexerFullScan}
//  // for a concrete implementation. N.B. Be careful about thread safety here.
//  protected Indexer _indexer;
//
//  public RelevanceFeedback (Options options, CgiArguments arguments, Indexer indexer){
//    _options = options;
//    _arguments = arguments;
//    _indexer = indexer;
//  }

  public static String extendQuery(CgiArguments arguments, IndexerInvertedCompressed indexer,
                                   Query query, Vector<ScoredDocument> scoredDocs) {
    int numDocs = scoredDocs.size();
    int numTerms = arguments._numTerms;

    Multiset<Integer> termFrequency = HashMultiset.create();

    for (ScoredDocument sd : scoredDocs) {
      termFrequency.addAll(indexer.getDocidTermFrequency(sd.getDocID()));
    }

    List<TermPrf> topTerms = new ArrayList<TermPrf>();
    int topTermsCount = 0;

    for (int i : Multisets.copyHighestCountFirst(termFrequency).elementSet()) {
      String term = indexer.getTermById(i);
      if (Tokenizer.stopwordFilter(term) == null || query._tokens.contains(term)) {
        continue;
      }
      int frequency = termFrequency.count(i);
//      System.out.println("Element: " + term + " Frequency: " + frequency);
      topTerms.add(new TermPrf(term, frequency));
      topTermsCount += frequency;
      numTerms--;
      if (numTerms == 0) {
        break;
      }
    }

    StringBuilder sb = new StringBuilder();

    for (TermPrf t : topTerms) {
      t.computePrf(topTermsCount);
      sb.append(t.term).append('\t').append(t.prf).append('\n');
    }

//    System.out.print(sb.substring(0,sb.length()-1));
    return sb.substring(0, sb.length() - 1);
  }
}