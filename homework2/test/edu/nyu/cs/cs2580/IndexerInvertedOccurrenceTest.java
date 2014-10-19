package edu.nyu.cs.cs2580;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexerInvertedOccurrenceTest extends TestCase {
  String optionStr = "homework2/test/edu/nyu/cs/cs2580/engine_occurrenceIndexer_test.conf";
  Indexer indexer;
  SearchEngine.Options OPTIONS;

  @Before
  public void setUp() throws Exception {
    OPTIONS = new SearchEngine.Options(optionStr);
    indexer = Indexer.Factory.getIndexerByOption(OPTIONS);
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void testNextDoc() throws Exception {
    indexer.constructIndex();
    Query query = new Query("new york times rolling taylor swift");
    query.processQuery();
    Document document1 = indexer.nextDoc(query, -1);
  }
}