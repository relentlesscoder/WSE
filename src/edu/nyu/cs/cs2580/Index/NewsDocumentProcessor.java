package edu.nyu.cs.cs2580.Index;

import com.google.gson.Gson;
import edu.nyu.cs.cs2580.SearchEngine;
import edu.nyu.cs.cs2580.crawler.rss.FeedMessage;
import edu.nyu.cs.cs2580.document.DocumentNews;
import edu.nyu.cs.cs2580.preprocess.FilePreprocess;

import java.io.*;
import java.util.*;

public class NewsDocumentProcessor extends DocumentProcessor {
  public NewsDocumentProcessor(File[] files, SearchEngine.Options options) {
    super(files, options);
  }

  @Override
  public void processDocuments() throws IOException {
    processMsgDocument(0);
  }

  protected void processMsgDocument(int dummy) throws IOException{
    Gson gson = new Gson();
    StringBuilder sb = new StringBuilder();
    int documentCount = FilePreprocess.countLines(files[dummy]);

    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(files[dummy])));
    String s;
    int count = 0;

    while ((s = in.readLine()) != null) {
      int docid = count++;
      progressBar.update(docid, documentCount);
      FeedMessage message = gson.fromJson(s,FeedMessage.class);
      DocumentFields documentFields = new DocumentFields(message.getTitle());
      documentFields.setContent(message.getDescription());

      Date time = FilePreprocess.toData(message.getPubDate());
      DocumentNews doc = new DocumentNews(docid,time);
      doc.setTitle(message.getTitle());
      doc.setUrl(message.getLink());

      documents.add(doc);
      populateInvertedIndex(documentFields, docid);
    }

  }


}