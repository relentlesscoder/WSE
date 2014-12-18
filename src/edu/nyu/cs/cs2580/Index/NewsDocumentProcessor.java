package edu.nyu.cs.cs2580.index;

import com.google.gson.Gson;
import edu.nyu.cs.cs2580.SearchEngine;
import edu.nyu.cs.cs2580.crawler.News;
import edu.nyu.cs.cs2580.document.DocumentNews;
import edu.nyu.cs.cs2580.preprocess.FilePreprocess;
import edu.nyu.cs.cs2580.rankers.IndexerConstant;

import java.io.*;
import java.util.*;

public class NewsDocumentProcessor extends DocumentProcessor {
  public NewsDocumentProcessor(File[] files, SearchEngine.Options options) {
    super(files, options);
  }

  @Override
  public void processDocuments() throws IOException {

    File file = files[0];
    Gson gson = new Gson();
    String line = "";
    int docid = 0;

    for (int i = 0; i < files.length; i++) {
      if (files[i].getPath().equals("data/news/inter/news.txt")) {
        file = files[i];
      }
    }

    int documentCount = FilePreprocess.countLines(file);
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

    while ((line = br.readLine()) != null) {
      progressBar.update(docid, documentCount);

      News news = gson.fromJson(line, News.class);
      DocumentFields documentFields = new DocumentFields(news.getTitle());
      documentFields.setContent(news.getDescription());
      Date time = news.getPubDate();
      DocumentNews doc = new DocumentNews(docid, time);
      doc.setTitle(news.getTitle());
      doc.setUrl(news.getLink());
      doc.setDescription(news.getDescription());
      doc.setSource(news.getSource());

      documents.add(doc);
      populateInvertedIndex(documentFields, docid);

      if (hasReachSizeThreshold()) {
        split(IndexerConstant.NEWS_FEED_CORPUS_INDEX, IndexerConstant.NEWS_FEED_DOCUMENTS, IndexerConstant.EXTENSION_IDX, splitFileNumber++);
      }
      docid++;
    }
    br.close();
    split(IndexerConstant.NEWS_FEED_CORPUS_INDEX, IndexerConstant.NEWS_FEED_DOCUMENTS, IndexerConstant.EXTENSION_IDX, splitFileNumber++);
  }
}
