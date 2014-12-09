package edu.nyu.cs.cs2580.Index;

import edu.nyu.cs.cs2580.Document.DocumentIndexed;
import edu.nyu.cs.cs2580.Rankers.IndexerConstant;
import edu.nyu.cs.cs2580.SearchEngine;
import edu.nyu.cs.cs2580.Utils.Util;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class HtmlDocumentProcessor extends DocumentProcessor {
  public HtmlDocumentProcessor(File[] files, SearchEngine.Options options) {
    super(files, options);
  }

  /**
   * @return the total term frequency.
   */
  @Override
  public void processDocuments() throws IOException {
    for (int docid = 0; docid < files.length; docid++) {
      checkNotNull(files[docid], "File can not be null!");
      // Update the progress bar first :)
      progressBar.update(docid, files.length);
      // Now process the document
      processDocument(docid);
      // Write to a file if memory usage has reach the memory threshold
      if (hasReachThresholdCompress()) {
        split(IndexerConstant.HTML_CORPUS_INDEX, IndexerConstant.HTML_DOCUMENTS, IndexerConstant.EXTENSION_IDX, partialFileCount);
        partialFileCount++;
      }
    }

    // Write the rest partial inverted index...
    split(IndexerConstant.HTML_CORPUS_INDEX, IndexerConstant.HTML_DOCUMENTS, IndexerConstant.EXTENSION_IDX, partialFileCount);
  }

  /**
   * Process the web page document. First store the document, then populate the inverted
   * index.
   *
   * @param docid The file's document ID.
   * @throws java.io.IOException
   */
  protected void processDocument(int docid) throws IOException {
    org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(files[docid], "UTF-8");

    String bodyText = jsoupDoc.body().text();
    String title = jsoupDoc.title();

    // Create the document and store it.
    DocumentIndexed doc = new DocumentIndexed(docid);
    doc.setTitle(title);
    doc.setUrl(files[docid].getName());

    documents.add(doc);

    // Populate the inverted index.
    populateInvertedIndex(title + " " + bodyText, docid);
  }
}
