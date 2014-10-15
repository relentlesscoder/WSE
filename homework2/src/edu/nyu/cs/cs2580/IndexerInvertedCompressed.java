package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.SearchEngine.Options;
import org.jsoup.Jsoup;

import java.io.*;
import java.util.*;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends Indexer implements Serializable {
  // Temporary UID...
  private static final long serialVersionUID = 1L;

  // Compressed inverted index, key is the term and value is the document
  // ID the term appears in the corpus with its occurrences and offsets.
  private Map<String, List<Byte>> invertedIndex =
      new HashMap<String, List<Byte>>();

  // Key is the term and value is the offsets for each of the document ID in
  // the inverted index
  private Map<String, List<Byte>> docIdOffsetMap =
      new HashMap<String, List<Byte>>();

  // Key is the term and value is the previous docid of that term.
  // This is used to construct the index and will be cleared at the end.
  Map<String, Integer> prevDocidMap = new HashMap<String, Integer>();

  // Term frequency, key is the integer representation of the term and value is
  // the number of times the term appears in the corpus.
  private Map<String, Integer> _termCorpusFrequency =
      new HashMap<String, Integer>();

  // Stores all Document in memory.
  private Vector<DocumentIndexed> _documents = new Vector<DocumentIndexed>();

  // Provided for serialization
  public IndexerInvertedCompressed() {
  }

  public IndexerInvertedCompressed(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {
    //TODO: Only for testing locally in the IDE...
    //    File folder = new File(_options._corpusPrefix);
    File folder = new File("/Users/youlongli/Documents/Dropbox/cs/WS/WSE/homework2/data/smallWiki");
    File[] listOfFiles = folder.listFiles();

    // Process file/document one by one.
    for (int docid = 0; docid < listOfFiles.length; docid++) {
      if (listOfFiles[docid].isFile()) {
        processDocument(listOfFiles[docid], docid);
      }
    }

    _numDocs = _documents.size();

    // Clear the previous document ID map since it's no longer needed...
    prevDocidMap.clear();

    System.out.println(
        "Indexed " + Integer.toString(_numDocs) + " docs with " +
            Long.toString(_totalTermFrequency) + " terms.");

    //TODO: Only for testing locally in the IDE...
    //    String indexFile = _options._indexPrefix + "/corpus.idx";
    String indexFile = "./corpus.idx";

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
   * @param file a file waiting for indexing
   * @param docid the file's document ID
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
    readInvertedIndex(title + " " + bodyText, docid);

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
    // Key is the term and value is the posting list
    Map<String, List<Integer>> tmpInvertedIndex = new HashMap<String, List<Integer>>();
    // Key is the term and value is the offset for the docid in the posting list
    Map<String, Integer> tmpDocIdOffsetMap = new HashMap<String, Integer>();

    int offsetOfDoc = 0;

    // TODO: Temporary. Need a better tokenizer and stemming later...
    Scanner scanner = new Scanner(content).useDelimiter("\\W");

    // Deal with each token first
    while (scanner.hasNext()) {
      String token = scanner.next().toLowerCase();

      if (tmpInvertedIndex.containsKey(token)) {
        // The token has already been seen at least once in the document.
        int occurs = tmpInvertedIndex.get(token).get(1);
        // Update the occurrence
        tmpInvertedIndex.get(token).set(1, occurs + 1);
        // Add the offset of this token
        tmpInvertedIndex.get(token).add(offsetOfDoc);
      } else {
        // This is the first time the token has been seen in the document
        if (invertedIndex.containsKey(token)) {
          // The inverted index already has the token in other previous documents
          List<Integer> tmpList = new ArrayList<Integer>();
          // Get the docid of the previous document containing this token
          int prevDocid = prevDocidMap.get(token);
          // Get the delta for this document
          int deltaDocid = docid - prevDocid;
          tmpList.add(deltaDocid);
          tmpList.add(1);
          tmpList.add(offsetOfDoc);
          tmpInvertedIndex.put(token, tmpList);

          // Get the docid offset of the posting list which starts from the end
          // of the existing one
          tmpDocIdOffsetMap.put(token, docIdOffsetMap.size());
        } else {
          // This is the first time the token has been seen in the entire index
          List<Integer> tmpList = new ArrayList<Integer>();
          // Since this is the first document in the posting list, no need to calculate
          // the delta.
          tmpList.add(docid);
          tmpList.add(1);
          tmpList.add(offsetOfDoc);
          tmpInvertedIndex.put(token, tmpList);

          // Get the docid offset of the posting list which shall be the first
          tmpDocIdOffsetMap.put(token, 0);
        }
      }

      if (!_termCorpusFrequency.containsKey(token)) {
        _termCorpusFrequency.put(token, 1);
      } else {
        _termCorpusFrequency.put(token, _termCorpusFrequency.get(token) + 1);
      }

      _totalTermFrequency++;
      offsetOfDoc++;
    }

    // Convert all offset of the posting list to deltas for compression
    convertOffsetToDelta(tmpInvertedIndex);

    for (String term : tmpInvertedIndex.keySet()) {
      // Update prevDocidMap
      prevDocidMap.put(term, docid);

      // Get the offset of each term's docid in the posting list
      int offset = tmpDocIdOffsetMap.get(term);
      // Encode the posting list
      List<Byte> partialPostingList = vByteEncodingList(tmpInvertedIndex.get(term));

      if (invertedIndex.containsKey(term)) {
        // Update the docidOffsetMap for the term
        List<Byte> byteList = docIdOffsetMap.get(term);
        // Add the encoded offset
        byteList.addAll(vByteEncoding(offset));
        docIdOffsetMap.put(term, byteList);

        // Add the posting list to a existing one
        List<Byte> postingList = invertedIndex.get(term);
        postingList.addAll(partialPostingList);
        invertedIndex.put(term, postingList);
      } else {
        // Set the initial offset for the term in docidOffsetMap
        docIdOffsetMap.put(term, vByteEncoding(offset));

        // Set the initial posting list for the term
        invertedIndex.put(term, partialPostingList);
      }
    }
  }

  /**
   * Convert the offsets of each posting list to deltas.
   * e.g.
   * From:
   * List: 0 (docid), 4 (occurrence), 5, 12, 18, 29
   * To:
   * List: 0 (docid), 4 (occurrence), 5, 7, 6, 11
   *
   * @param partialInvertedIndex
   */
  private void convertOffsetToDelta(Map<String, List<Integer>> partialInvertedIndex) {
    for (List<Integer> list : partialInvertedIndex.values()) {
      if (list.get(1) > 1) {
        for (int i = list.size() - 1; i > 2; i--) {
          int offset = list.get(i);
          int prevOffset = list.get(i - 1);
          list.set(i, offset - prevOffset);
        }
      }
    }
  }

  /**
   * If the byte is not the end of a v-byte encoded number, return false.
   * Otherwise return true.
   *
   * @param b
   * @return
   */
  private boolean isEndOfNum(byte b) {
    return (b >> 7 & 1) == 1;
  }

  /**
   * Decode a list of bytes to one integer number
   *
   * @param bytes
   * @return
   */
  private int vByteDecoding(List<Byte> bytes) {
    StringBuilder sb = new StringBuilder();

    // Append all bytes together
    for (byte b : bytes) {
      sb.append(Integer.toBinaryString(b & 255 | 256).substring(2));
    }

    // Return the int num according to the binary string
    return Integer.parseInt(sb.toString(), 2);
  }

  /**
   * Encode a integer number to a list of bytes
   *
   * @param num
   * @return
   */
  private List<Byte> vByteEncoding(int num) {
    List<Byte> bytes = new ArrayList<Byte>();

    if (num < 0) {
      throw new IllegalArgumentException("Must be positive number :(");
    }

    // Get the binary string of the number
    String binaryStr = Integer.toBinaryString(num);

    // How many bytes the number will need
    int size = (binaryStr.length() - 1) / 7 + 1;

    int start = 0;
    int end = 0;
    int offset = 7 * (size - 1);

    // Get one byte at a time
    for (int i = 0; i < size; i++) {
      byte b = 0;

      if (i == 0) {
        end = binaryStr.length() - offset;
      } else {
        start = end;
        end += 7;
      }

      // Get the binary string for one byte
      String s = binaryStr.substring(start, end);
      b = Byte.parseByte(s, 2);

      // If it's the last byte, then the highest bit shall be 1, otherwise
      // will be 0 (No operation need...).
      if (size == 1 || i == size - 1) {
        b = (byte) (b | (1 << 7));
      }

      bytes.add(b);
    }

    return bytes;
  }

  /**
   * Decode a list of Bytes to a list of Integers by using v-byte
   *
   * @param byteList
   * @return
   */
  private List<Integer> vByteDecodingList(List<Byte> byteList) {
    List<Integer> res = new ArrayList<Integer>();
    int i = 0;

    while (i < byteList.size()) {
      List<Byte> tmpByteList = new ArrayList<Byte>();

      while (!isEndOfNum(byteList.get(i))) {
        tmpByteList.add(byteList.get(i));
        i++;
      }

      tmpByteList.add(byteList.get(i));
      i++;

      res.add(vByteDecoding(tmpByteList));
    }

    return res;
  }

  /**
   * Encode a list of Integers to a list of Bytes by using v-byte
   *
   * @param list
   * @return
   */
  private List<Byte> vByteEncodingList(List<Integer> list) {
    List<Byte> res = new ArrayList<Byte>();

    for (int i : list) {
      res.addAll(vByteEncoding(i));
    }

    return res;
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Load index from: " + indexFile);

    ObjectInputStream reader =
        new ObjectInputStream(new FileInputStream(indexFile));
    IndexerInvertedCompressed loaded = (IndexerInvertedCompressed) reader.readObject();

    this._documents = loaded._documents;

    // TODO: What does that mean?
    // Compute numDocs and totalTermFrequency b/c Indexer is not serializable. -- > ?
    this._numDocs = _documents.size();
    for (Integer freq : loaded._termCorpusFrequency.values()) {
      this._totalTermFrequency += freq;
    }
    this.invertedIndex = loaded.invertedIndex;
    this.docIdOffsetMap = loaded.docIdOffsetMap;
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
   * In HW2, you should be using {@link DocumentIndexed}
   */
  @Override
  public Document nextDoc(Query query, int docid) {
    //TODO:
//    List<List<Byte>> queryDocidList = new ArrayList<List<Byte>>();
//    Vector<String> tokens = query._tokens;
//    int tokenSize = tokens.size();
//
//    for (int i = 0; i < tokenSize; i++) {
//      queryDocidList.add(invertedIndex.get(tokens.get(i)));
//    }
//
//    int nextDocid = nextDocid(queryDocidList, docid);
//
//    if (nextDocid != -1) {
//      return _documents.get(nextDocid);
//    } else {
//      return null;
//    }
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

  /**
   * @CS2580: Implement this for bonus points.
   */
  @Override
  public int documentTermFrequency(String term, String url) {
    return 0;
  }
}