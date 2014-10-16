package edu.nyu.cs.cs2580;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static edu.nyu.cs.cs2580.SearchEngine.Options;

public class IndexerInvertedCompressedTest extends TestCase {
  String optionStr = "/Users/youlongli/Documents/Dropbox/cs/WS/WSE/homework2/test/edu/nyu/cs/cs2580/engine_compressedIndexer_test.conf";
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
    Query query = new Query("alaska purchase");
    query.processQuery();
    Document document1 = indexer.nextDoc(query, -1);
    Document document2 = indexer.nextDoc(query, document1._docid);
    Document document3 = indexer.nextDoc(query, document2._docid);
    Document document4 = indexer.nextDoc(query, document3._docid);
//    int pos1 = indexer.nextPos("alaska", document2._docid, -1);
  }
}