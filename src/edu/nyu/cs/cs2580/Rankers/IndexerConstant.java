package edu.nyu.cs.cs2580.Rankers;

public class IndexerConstant {
  public static final long MEGABYTE = 1024L * 1024L;
  public static final long PARTIAL_FILE_SIZE = 6 * IndexerConstant.MEGABYTE;
  public static final long MAX_INVERTED_INDEX_SIZE = 12 * IndexerConstant.MEGABYTE;

  public static final String HTML_CORPUS_INDEX = "html_corpus";
  public static final String HTML_DOCUMENTS = "html_documents";
  public static final String HTML_META = "html_meta";

  public static final String NEWS_FEED_CORPUS_INDEX = "news_feed_corpus";
  public static final String NEWS_FEED_DOCUMENTS = "news_feed_documents";
  public static final String NEWS_FEED_META = "news_feed_meta";

  public static final String EXTENSION_IDX = ".idx";

  private IndexerConstant() {}
}
