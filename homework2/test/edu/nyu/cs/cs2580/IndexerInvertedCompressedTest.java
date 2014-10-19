package edu.nyu.cs.cs2580;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static edu.nyu.cs.cs2580.SearchEngine.Options;

public class IndexerInvertedCompressedTest extends TestCase {
  String optionStr = "homework2/test/edu/nyu/cs/cs2580/engine_compressedIndexer_test.conf";
  Indexer indexer;
  Options OPTIONS;

  @Before
  public void setUp() throws IOException {
    OPTIONS = new Options(optionStr);
    indexer = Indexer.Factory.getIndexerByOption(OPTIONS);
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void test1() throws IOException {
    indexer.constructIndex();
    Query query = new Query("new york times rolling taylor swift");
    query.processQuery();
    Document document1 = indexer.nextDoc(query, -1);
  }
}