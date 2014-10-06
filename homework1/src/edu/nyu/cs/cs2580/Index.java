package edu.nyu.cs.cs2580;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Index {
  private static final Logger logger = LogManager.getLogger();

  public List<Document> _documents;

  public Index(String indexDir) {
    logger.info("Begin to index documents...");

    // TODO: Delete later..
    System.out.println("Begin to index documents...");

    // Initialize the documents
    _documents = new ArrayList<Document>();

    try {
      logger.info("Reading documents from {}...", indexDir);
      BufferedReader reader = new BufferedReader(new FileReader(indexDir));
      try {
        String line = null;
        int docId = 0;
        while ((line = reader.readLine()) != null) {
          _documents.add(new Document(docId, line));
          docId++;
        }
      } finally {
        logger.info("Closing the file...", indexDir);
        reader.close();
      }
    } catch (IOException ioe) {
      logger.error("Oops... {}", ioe.getMessage());
    }
    logger.info("Done indexing {} documents...", _documents.size());

    // TODO: Delete later..
    System.out.println("Done indexing " + _documents.size() + " documents...");
  }

  /**
   * Return the number of documents a specific term occurs in.
   */
  public int documentFrequency(String term) {
    return Document.documentFrequency(term);
  }

  /**
   * Return the number of occurrences of a specific term in the entire
   * collection.
   */
  public int termFrequency(String term) {
    return Document.termFrequency(term);
  }

  /**
   * Returns the total number of words occurrences in the collection
   * (i.e. the sum of termFrequency(s) over all words in the vocabulary).
   */
  public int termFrequency() {
    return Document.termFrequency();
  }

  /**
   * Return the number of different terms in the collection.
   */
  public int numTerms() {
    return Document.numTerms();
  }

  /**
   * Get a term from the dictionary by a its index
   */
  public String getTerm(int index) {
    return Document.getTerm(index);
  }

  /**
   * Return the number of documents.
   */
  public int numDocs() {
    return _documents.size();
  }

  /**
   * Return a document according to the document id.
   */
  public Document getDoc(int docId) {
    return (docId >= _documents.size() || docId < 0) ? null : _documents.get(docId);
  }
}
