package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.SearchEngine.Options;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedDoconly extends Indexer {
  // Temporary UID...
  private static final long serialVersionUID = 1L;

  // Maps each term to the list of doc IDs they appear in.
  private Map<String, List<Integer>> invertedIndex =
      new HashMap<String, List<Integer>>();

  // Term frequency, key is the integer representation of the term and value is
  // the number of times the term appears in the corpus.
  private Map<String, Integer> _termCorpusFrequency =
      new HashMap<String, Integer>();

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
    File folder = new File(_options._corpusPrefix);
    File[] listOfFiles = folder.listFiles();

    // Process file/document one by one.
    for (int docid = 0; docid < listOfFiles.length; docid++) {
      if (listOfFiles[docid].isFile()) {
        processDocument(listOfFiles[docid], docid);
      }
    }

    _numDocs = _documents.size();
  }

  /**
   * Process the document file, create the inverted document set its title and URL;
   *
   * @param file
   * @param docid
   * @throws IOException
   */
  private void processDocument(File file, int docid) throws IOException {
    org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(file, "UTF-8");
    String bodyText = jsoupDoc.body().text();
    String title = jsoupDoc.title();

    DocumentIndexed doc = new DocumentIndexed(docid);
    doc.setTitle(title);
    doc.setUrl(file.getAbsolutePath());

    _documents.add(doc);

    // Populate the inverted index
    readInvertedIndex(title, docid);
    readInvertedIndex(bodyText, docid);
//    Elements links = jsoupDoc.select("a[href]");
  }

  /**
   * Read the content and populate the inverted index.
   *
   * @param content
   * @param docid
   */
  private void readInvertedIndex(String content, int docid) {
    Scanner scanner = new Scanner(content).useDelimiter("\\s");

    while (scanner.hasNext()) {
      String token = scanner.next();

      _totalTermFrequency++;

      if (!_termCorpusFrequency.containsKey(token)) {
        _termCorpusFrequency.put(token, 1);
      } else {
        _termCorpusFrequency.put(token, _termCorpusFrequency.get(token) + 1);
      }

      if (invertedIndex.containsKey(token)) {
        // The token exists in the index
        if (!invertedIndex.get(token).contains(docid)) {
          // The docid does not exist in the index for the token, add it in.
          invertedIndex.get(token).add(docid);
        }
      } else {
        // The token does not exist in the index, add it first, then add the docid
        List<Integer> tmpList = new ArrayList<Integer>();
        tmpList.add(docid);
        invertedIndex.put(token, tmpList);
      }
    }
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
  }

  @Override
  public Document getDoc(int docid) {
    return _documents.get(docid);
  }

  /**
   * Return the next document after the current document ID which satisfying
   * the query or null if no such document exists...
   *
   * @param query
   * @param docid
   * @return the next Document after {@code docid} satisfying {@code query} or
   * null if no such document exists.
   */
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
   * the query or -1 if no such document exists...
   *
   * @param queryDocidList
   * @param docid
   * @return the next Document ID after {@code docid} satisfying {@code query} or
   * -1 if no such document exists.
   */
  private int nextDocid(List<List<Integer>> queryDocidList, int docid) {
    boolean hasFound = true;
    int highestDocid = -1;

    //
    for (int i = 0; i < queryDocidList.size(); i++) {
      List<Integer> docidList = queryDocidList.get(i);
      int nextDocid = skipNextTo(docidList, docid);
      if (nextDocid == -1) {
        return -1;
      }
      highestDocid = Math.max(highestDocid, nextDocid);
    }

    for (int i = 0; i < queryDocidList.size(); i++) {
      List<Integer> docidList = queryDocidList.get(i);
      if (skipForwardTo(docidList, docid) == -1) {
        hasFound = false;
        break;
      }
    }

    if (hasFound) {
      return highestDocid;
    } else {
      return nextDocid(queryDocidList, highestDocid);
    }
  }

  /**
   * Return the next document ID after the current document or -1 if no such document
   * ID exists.
   *
   * @param docidList
   * @param docid
   * @return
   */
  private int skipNextTo(List<Integer> docidList, int docid) {
    int size = docidList.size();

    if (size == 0 || docidList.get(size - 1) < docid) {
      return -1;
    }

    if (docidList.get(0) > docid) {
      return docidList.get(0);
    }

    int low = 0;
    int high = docidList.size() - 1;

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
   * Return the next document ID after the current document or -1 if no such document
   * ID exists.
   *
   * @param docidList
   * @param docid
   * @return
   */
  private int skipForwardTo(List<Integer> docidList, int docid) {
    int size = docidList.size();

    if (size == 0 || docidList.get(size - 1) < docid) {
      return -1;
    }

    int low = 0;
    int high = docidList.size() - 1;

    while (low < high) {
      int mid = low + (high - low) / 2;
      int midDocid = docidList.get(mid);
      if (midDocid == docid) {
        return docidList.get(mid);
      } else if (midDocid < docid) {
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }

    return -1;
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
