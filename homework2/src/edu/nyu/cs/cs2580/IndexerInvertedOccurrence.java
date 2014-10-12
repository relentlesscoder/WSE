package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.SearchEngine.Options;
import org.jsoup.Jsoup;

import java.io.*;
import java.util.*;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends Indexer {
  // Temporary UID...
  private static final long serialVersionUID = 1L;

  // Inverted index, ket is the term and value is the document ID the term
  // appears in the corpus with its occurrences and offsets.
  private Map<String, List<Integer>> invertedIndex =
      new HashMap<String, List<Integer>>();

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

    System.out.println(
        "Indexed " + Integer.toString(_numDocs) + " docs with " +
            Long.toString(_totalTermFrequency) + " terms.");

    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Store index to: " + indexFile);
    ObjectOutputStream writer =
        new ObjectOutputStream(new FileOutputStream(indexFile));
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

    DocumentIndexed doc = new DocumentIndexed(docid);
    doc.setTitle(title);
    doc.setUrl(file.getAbsolutePath());

    _documents.add(doc);

    // Populate the inverted index
    readInvertedIndex(title, docid);
    readInvertedIndex(bodyText, docid);

    // TODO: Deal with all the links...
//    Elements links = jsoupDoc.select("a[href]");
  }

  /**
   * Read the content of {@code docid} and populate the inverted index.
   *
   * @param content
   * @param docid
   */
  private void readInvertedIndex(String content, int docid) {
    // TODO: Tmporary way for extract tokens...
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
        invertedIndex.get(token).add(docid);
        invertedIndex.get(token).add(offset);
      } else {
        // The token does not exist in the index, add it first, then add the
        // docid and the token's offset
        List<Integer> tmpList = new ArrayList<Integer>();
        tmpList.add(docid);
        tmpList.add(offset);
        invertedIndex.put(token, tmpList);
      }

      _totalTermFrequency++;
      offset++;
    }
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Load index from: " + indexFile);

    ObjectInputStream reader =
        new ObjectInputStream(new FileInputStream(indexFile));
    IndexerInvertedOccurrence loaded = (IndexerInvertedOccurrence) reader.readObject();

    this._documents = loaded._documents;

    // TODO: What does that mean?
    // Compute numDocs and totalTermFrequency b/c Indexer is not serializable. -- > ?
    this._numDocs = _documents.size();
    for (Integer freq : loaded._termCorpusFrequency.values()) {
      this._totalTermFrequency += freq;
    }
    this.invertedIndex = loaded.invertedIndex;
    this._termCorpusFrequency = loaded._termCorpusFrequency;
    reader.close();

    System.out.println(Integer.toString(_numDocs) + " documents loaded " +
        "with " + Long.toString(_totalTermFrequency) + " terms!");
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
