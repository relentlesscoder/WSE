package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.SearchEngine.Options;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends Indexer {
  // Temporary UID...
  private static final long serialVersionUID = 1L;

  // Maps each term to the list of doc IDs, occurrences and offsets they appear in
  private Map<String, List<TermOffset>> invertedIndex =
      new HashMap<String, List<TermOffset>>();

  // Term frequency, key is the integer representation of the term and value is
  // the number of times the term appears in the corpus.
  private Map<String, Integer> _termCorpusFrequency =
      new HashMap<String, Integer>();

  // Stores all Document in memory.
  private Vector<DocumentIndexed> _documents = new Vector<DocumentIndexed>();

  // Provided for serialization
  public IndexerInvertedOccurrence() {
  }

  public IndexerInvertedOccurrence(Options options) {
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
    int offset = 0;

    while (scanner.hasNext()) {
      String token = scanner.next();

      if (!_termCorpusFrequency.containsKey(token)) {
        _termCorpusFrequency.put(token, 1);
      } else {
        _termCorpusFrequency.put(token, _termCorpusFrequency.get(token) + 1);
      }

      TermOffset termOffset = new TermOffset(docid, offset);

      if (invertedIndex.containsKey(token)) {
        // The token exists in the index
        invertedIndex.get(token).add(termOffset);
      } else {
        // The token does not exist in the index, add it first, then add the docid
        List<TermOffset> tmpList = new ArrayList<TermOffset>();
        tmpList.add(termOffset);
        invertedIndex.put(token, tmpList);
      }

      _totalTermFrequency++;
      offset++;
    }
  }

  public class TermOffset {
    public final int docid;
    public final int offset;

    TermOffset(int docid, int offset) {
      this.docid = docid;
      this.offset = offset;
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
   * In HW2, you should be using {@link DocumentIndexed}.
   */
  @Override
  public Document nextDoc(Query query, int docid) {
    return null;
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
