package edu.nyu.cs.cs2580.Index;

import com.google.gson.Gson;
import edu.nyu.cs.cs2580.SearchEngine;
import edu.nyu.cs.cs2580.crawler.rss.FeedMessage;
import edu.nyu.cs.cs2580.document.DocumentNews;
import edu.nyu.cs.cs2580.preprocess.FilePreprocess;
import edu.nyu.cs.cs2580.rankers.IndexerConstant;

import java.io.*;
import java.util.Date;

public class NewsDocumentProcessor extends DocumentProcessor {
  public NewsDocumentProcessor(File[] files, SearchEngine.Options options) {
    super(files, options);
  }

  @Override
  public void processDocuments() throws IOException {
    Gson gson = new Gson();
    StringBuilder sb = new StringBuilder();
    int documentCount = FilePreprocess.countLines(files[0]);

    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(files[0])));
    splitFileNumber = 0;
    String s;
    int count = 0;

    while ((s = in.readLine()) != null) {
      int docid = count++;
      progressBar.update(docid, documentCount);
      FeedMessage message = gson.fromJson(s, FeedMessage.class);
      DocumentFields documentFields = new DocumentFields(message.getTitle());
      documentFields.setContent(message.getDescription());

      Date time = FilePreprocess.toData(message.getPubDate());
      DocumentNews doc = new DocumentNews(docid, time);
      doc.setTitle(message.getTitle());
      doc.setUrl(message.getLink());

      documents.add(doc);
      populateInvertedIndex(documentFields, docid);

      if (hasReachThresholdCompress()) {
        split(IndexerConstant.NEWS_FEED_CORPUS_INDEX, IndexerConstant.NEWS_FEED_DOCUMENTS, IndexerConstant.EXTENSION_IDX, splitFileNumber++);
      }
    }
    split(IndexerConstant.NEWS_FEED_CORPUS_INDEX, IndexerConstant.NEWS_FEED_DOCUMENTS, IndexerConstant.EXTENSION_IDX, splitFileNumber++);
  }
}