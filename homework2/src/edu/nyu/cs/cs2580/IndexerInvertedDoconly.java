package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.jsoup.Jsoup;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedDoconly extends Indexer implements Serializable {
  // Temporary UID...
  private static final long serialVersionUID = 1L;

  // Inverted index, ket is the term and value is the list of doc IDs the term
  // appears in the corpus.
  private Map<String, List<Integer>> invertedIndex = new HashMap<String, List<Integer>>();

  // Term frequency, key is the term and value is the number of times the term
  // appears in the corpus.
  private Map<String, Integer> _termCorpusFrequency = new HashMap<String, Integer>();

  // Stores all Document in memory.
  private Vector<DocumentIndexed> _documents = new Vector<DocumentIndexed>();

  // Provided for serialization
  public IndexerInvertedDoconly() {
  }

  public IndexerInvertedDoconly(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {
    // TODO: Change it back later...
    File folder = new File(
        "/Users/youlongli/Documents/Dropbox/cs/WS/WSE/homework2/data/smallWiki");
    // File folder = new File(_options._corpusPrefix);
    File[] listOfFiles = folder.listFiles();

    // Process file/document one by one.
    for (int docid = 0; docid < listOfFiles.length; docid++) {
      if (listOfFiles[docid].isFile()) {
        processDocument(listOfFiles[docid], docid);
      }
    }

    _numDocs = _documents.size();

    System.out.println("Indexed " + Integer.toString(_numDocs) + " docs with "
        + Long.toString(_totalTermFrequency) + " terms.");

    // TODO:
    String indexFile = "./corpus.idx";
    // String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Store index to: " + indexFile);
    ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(
        indexFile));
    writer.writeObject(this);
    writer.close();
  }

  /**
   * Process the document file, populate the inverted index and store the
   * document.
   * 
   * @param file
   * @param docid
   * @throws IOException
   */
  private void processDocument(File file, int docid) throws IOException {
    org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(file, "UTF-8");
    // TODO: Tmporary way for extract text...
    String bodyText = jsoupDoc.body().text();
    String title = jsoupDoc.title();

    // Create DocumentIndexed and store it
    DocumentIndexed doc = new DocumentIndexed(docid);
    doc.setTitle(title);
    doc.setUrl(file.getAbsolutePath());

    _documents.add(doc);

    // Populate the inverted index
    readInvertedIndex(title, docid);
    readInvertedIndex(bodyText, docid);

    // TODO: Deal with all the links...
    // Elements links = jsoupDoc.select("a[href]");
  }

  /**
   * Read the content of the document and populate the inverted index.
   * 
   * @param content
   * @param docid
   */
  private void readInvertedIndex(String content, int docid) {
    // TODO: Tmporary way for extract tokens...
    // Scanner scanner = new Scanner(content).useDelimiter("\\W");
    Tokenizer tokenizer = new Tokenizer(new StringReader(content));

    while (tokenizer.hasNext()) {
      String token = tokenizer.getText();

      _totalTermFrequency++;

      if (!_termCorpusFrequency.containsKey(token)) {
        _termCorpusFrequency.put(token, 1);
      } else {
        _termCorpusFrequency.put(token, _termCorpusFrequency.get(token) + 1);
      }

      // Populate the inverted index
      if (invertedIndex.containsKey(token)) {
        // The token exists in the index
        if (!invertedIndex.get(token).contains(docid)) {
          // The docid does not exist in the index for the token, add it in.
          invertedIndex.get(token).add(docid);
        }
      } else {
        // The token does not exist in the index, add it first, then add the
        // docid
        List<Integer> tmpList = new ArrayList<Integer>();
        tmpList.add(docid);
        invertedIndex.put(token, tmpList);
      }
    }
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Load index from: " + indexFile);

    ObjectInputStream reader = new ObjectInputStream(new FileInputStream(
        indexFile));
    IndexerInvertedDoconly loaded = (IndexerInvertedDoconly) reader
        .readObject();

    this._documents = loaded._documents;

    // TODO: What does that mean?
    // Compute numDocs and totalTermFrequency b/c Indexer is not serializable.
    // -- > ?
    this._numDocs = _documents.size();
    for (Integer freq : loaded._termCorpusFrequency.values()) {
      this._totalTermFrequency += freq;
    }
    this.invertedIndex = loaded.invertedIndex;
    this._termCorpusFrequency = loaded._termCorpusFrequency;
    reader.close();

    System.out.println(Integer.toString(_numDocs) + " documents loaded "
        + "with " + Long.toString(_totalTermFrequency) + " terms!");
  }

  @Override
  public Document getDoc(int docid) {
    return _documents.get(docid);
  }

  @Override
  public DocumentIndexed nextDoc(Query query, int docid) {
    List<List<Integer>> queryDocidList = new ArrayList<List<Integer>>();
    Vector<String> tokens = query._tokens;
    int tokenSize = tokens.size();

    for (int i = 0; i < tokenSize; i++) {
      queryDocidList.add(invertedIndex.get(tokens.get(i)));
    }

    int nextDocid = nextDocid(queryDocidList, docid);

    if (nextDocid != -1) {
      return _documents.get(nextDocid);
    } else {
      return null;
    }
  }

  /**
   * Return the next document ID after the current document ID which satisfying
   * the query or -1 if no such document exists... This function uses document
   * at a time retrieval method.
   * 
   * @param queryDocidList
   * @param docid
   * @return the next Document ID after {@code docid} satisfying {@code query}
   *         or -1 if no such document exists.
   */
  private int nextDocid(List<List<Integer>> queryDocidList, int docid) {
    boolean hasFound = true;
    int largestDocid = -1;

    // For each query term's document ID list, find the largest docId because it
    // is a reasonable candidate.
    for (int i = 0; i < queryDocidList.size(); i++) {
      List<Integer> docidList = queryDocidList.get(i);
      // Get the next document ID next to the current {@code docid} in the list
      int nextDocid = getNextDocid(docidList, docid);
      if (nextDocid == -1) {
        // The next document ID does not exist... so no next document will be
        // available.
        return -1;
      }
      largestDocid = Math.max(largestDocid, nextDocid);
    }

    // Check if the largest document ID satisfy all query terms.
    for (int i = 0; i < queryDocidList.size(); i++) {
      List<Integer> docidList = queryDocidList.get(i);
      if (!hasDocid(docidList, docid)) {
        // This document ID does not satisfy one of the query term...
        // Check the next...
        return nextDocid(queryDocidList, largestDocid);
      }
    }

    // If the satisfied document ID has been found, return it.
    return largestDocid;
  }

  /**
   * Return the next document ID after the current document or -1 if no such
   * document ID exists.
   * 
   * @param docidList
   * @param docid
   * @return
   */
  private int getNextDocid(List<Integer> docidList, int docid) {
    int size = docidList.size();

    // Base case
    if (size == 0 || docidList.get(size - 1) <= docid) {
      return -1;
    }

    if (docidList.get(0) > docid) {
      return docidList.get(0);
    }

    // Use binary search for the next document ID right after {@code docid}
    int low = 0;
    int high = docidList.size() - 1;

    // If the docid exists in the document ID list, then set the low to its
    // index to speed up the search.
    if (docidList.contains(docid)) {
      low = docidList.indexOf(docid);
    }

    while (high - low > 1) {
      int mid = low + (high - low) / 2;
      int midDocid = docidList.get(mid);
      if (midDocid <= docid) {
        low = mid;
      } else {
        high = mid;
      }
    }

    return docidList.get(high);
  }

  /**
   * Return the document ID after the current document or -1 if no such document
   * ID exists.
   * 
   * @param docidList
   * @param docid
   * @return
   */
  private boolean hasDocid(List<Integer> docidList, int docid) {
    int size = docidList.size();

    if (size == 0 || docidList.get(size - 1) < docid) {
      return false;
    }

    // Use binary search to find if the {@code docid} exists in the list
    int low = 0;
    int high = docidList.size() - 1;

    while (low <= high) {
      int mid = low + (high - low) / 2;
      if (docidList.get(mid) == docid) {
        return true;
      } else if (docidList.get(mid) < docid) {
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }

    return false;
  }

  @Override
  public int corpusDocFrequencyByTerm(String term) {
    if (invertedIndex.containsKey(term)) {
      return invertedIndex.get(term).size();
    } else {
      return 0;
    }
  }

  @Override
  public int corpusTermFrequency(String term) {
    return _termCorpusFrequency.get(term);
  }

  @Override
  public int documentTermFrequency(String term, String url) {
    SearchEngine.Check(false, "Not implemented!");
    return 0;
  }
}
