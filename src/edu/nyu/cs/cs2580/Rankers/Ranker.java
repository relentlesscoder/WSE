package edu.nyu.cs.cs2580.rankers;

import edu.nyu.cs.cs2580.document.ScoredDocument;
import edu.nyu.cs.cs2580.index.Indexer;
import edu.nyu.cs.cs2580.spellCheck.SpellCheckResult;
import edu.nyu.cs.cs2580.query.Query;
import edu.nyu.cs.cs2580.SearchEngine.Options;
import edu.nyu.cs.cs2580.handler.CgiArguments;

import java.util.Vector;

/**
 * This is the abstract Ranker class for all concrete Ranker implementations.
 * <p>
 * Use {@link Ranker.Factory} to create your concrete Ranker implementation. Do
 * NOT change the interface in this class!
 * <p>
 * In HW1: {@link RankerFullScan} is the instructor's simple ranker and students
 * implement four additional concrete rankers.
 * <p>
 * In HW2: students will pick a favorite concrete Ranker other than
 * {@link RankerPhrase}, and re-implement it using the more efficient concrete
 * Indexers.
 * <p>
 * 2013-02-16: The instructor's code went through substantial refactoring
 * between HW1 and HW2, students are expected to refactor code accordingly.
 * Refactoring is a common necessity in real world and part of the learning
 * experience.
 *
 * @author congyu
 * @author fdiaz
 */
public abstract class Ranker {
  // Options to configure each concrete Ranker.
  protected Options _options;
  // CGI arguments user provide through the URL.
  protected CgiArguments _arguments;

  // The Indexer via which documents are retrieved, see {@code IndexerFullScan}
  // for a concrete implementation. N.B. Be careful about thread safety here.
  protected Indexer _indexer;

  /**
   * Constructor: the construction of the Ranker requires an Indexer.
   */
  protected Ranker(Options options, CgiArguments arguments, Indexer indexer) {
    _options = options;
    _arguments = arguments;
    _indexer = indexer;
  }

  /**
   * Processes one query.
   *
   * @param query      the parsed user query
   * @param numResults number of results to return
   * @return Up to {@code numResults} scored documents in ranked order
   */
  public abstract Vector<ScoredDocument> runQuery(Query query, int numResults);

  /**
   * All rankers must be created through this factory class based on the
   * provided {@code arguments}.
   */
  public static class Factory {
    public static Ranker getRankerByArguments(CgiArguments arguments,
                                              Options options, Indexer indexer) {
      switch (arguments._rankerType) {
        case CONJUNCTIVE:
          return new RankerConjunctive(options, arguments, indexer);
        case COSINE:
          return new RankerCosine(options, arguments, indexer);
        case QL:
          return new RankerQL(options, arguments, indexer);
        case COMPREHENSIVE:
          return new RankerComprehensive(options, arguments, indexer);
        case NEWS:
          return new NewsRanker(options, arguments, indexer);
        case NONE:
          // Fall through intended
        default:
          // Do nothing.
      }
      return null;
    }
  }
}
