package edu.nyu.cs.cs2580.Index;

import edu.nyu.cs.cs2580.SearchEngine;

import java.io.File;
import java.io.IOException;

public class NewsDocumentProcessor extends DocumentProcessor {
  public NewsDocumentProcessor(File[] files, SearchEngine.Options options) {
    super(files, options);
  }

  @Override
  public void processDocuments() throws IOException {

  }
}