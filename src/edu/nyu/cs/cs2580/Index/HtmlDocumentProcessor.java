package edu.nyu.cs.cs2580.Index;

import com.google.common.io.Files;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import edu.nyu.cs.cs2580.document.DocumentIndexed;
import edu.nyu.cs.cs2580.rankers.IndexerConstant;
import edu.nyu.cs.cs2580.SearchEngine;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
      // Update the progress bar first :)
      progressBar.update(docid, files.length);

      // Process the document and populate the inverted index
      processDocument(docid);

      // Write to a file if memory usage has reach the memory threshold
      if (hasReachSizeThreshold()) {
        split(IndexerConstant.HTML_CORPUS_INDEX, IndexerConstant.HTML_DOCUMENTS, IndexerConstant.EXTENSION_IDX, splitFileNumber++);
      }
    }

    // Write the rest partial inverted index...
    split(IndexerConstant.HTML_CORPUS_INDEX, IndexerConstant.HTML_DOCUMENTS, IndexerConstant.EXTENSION_IDX, splitFileNumber);
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

    String title = jsoupDoc.title();

    DocumentFields documentFields = new DocumentFields(title);

    String htmlContent = Files.toString(files[docid], StandardCharsets.UTF_8);

    try {
      documentFields.setContent(ArticleExtractor.INSTANCE.getText(htmlContent));
    } catch (BoilerpipeProcessingException ignore) {
//      e.printStackTrace();
    }

    // Create the document and store it.
    DocumentIndexed doc = new DocumentIndexed(docid);
    doc.setTitle(title);
    doc.setUrl(files[docid].getName());

    documents.add(doc);

    // Populate the inverted index.
    populateInvertedIndex(documentFields, docid);
  }
}
