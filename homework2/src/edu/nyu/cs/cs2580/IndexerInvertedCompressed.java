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

  // Key is the term and value is the offsets for each of the documents
  private Map<String, List<Byte>> docIdOffsetsMap =
      new HashMap<String, List<Byte>>();

  private Map<String, List<Integer>> uncompressedInvertedIndex
      = new HashMap<String, List<Integer>>();

  private Map<String, List<Integer>> uncompressedDocIdOffsetsMap =
      new HashMap<String, List<Integer>>();

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
    //TODO: Change it back later...
    File folder = new File("/Users/youlongli/Documents/Dropbox/cs/WS/WSE/homework2/data/smallWiki");
//    File folder = new File(_options._corpusPrefix);
    File[] listOfFiles = folder.listFiles();

    // Process file/document one by one.
    for (int docid = 0; docid < listOfFiles.length; docid++) {
      if (listOfFiles[docid].isFile()) {
        processDocument(listOfFiles[docid], docid);
      }
    }

    // Compress...
    populateCompressedInvertedIndex();
    populateCompressedDocidOffsetMap();
    // Clear the un compressed data...
    uncompressedInvertedIndex.clear();
    uncompressedDocIdOffsetsMap.clear();

    _numDocs = _documents.size();

    System.out.println(
        "Indexed " + Integer.toString(_numDocs) + " docs with " +
            Long.toString(_totalTermFrequency) + " terms.");

    //TODO:
    String indexFile = "./corpus.idx";
//    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Store index to: " + indexFile);
    ObjectOutputStream writer =
        new ObjectOutputStream(new FileOutputStream(indexFile));
    writer.writeObject(this);
    writer.close();
  }

  /**
   * Populate the compressed docid offset map.
   */
  private void populateCompressedDocidOffsetMap() {
    for (Map.Entry entry : uncompressedDocIdOffsetsMap.entrySet()) {
      String term = (String) entry.getKey();
      List<Integer> list = (List<Integer>) entry.getValue();
      List<Byte> compressedList = vByteEncodingList(list);
      docIdOffsetsMap.put(term, compressedList);
    }
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
    readInvertedIndex(title + bodyText, docid);

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
    Scanner scanner = new Scanner(content).useDelimiter("\\W");
    Map<String, List<Integer>> tmpUncompressedInvertedIndex
        = new HashMap<String, List<Integer>>();

    int offset = 0;

    while (scanner.hasNext()) {
      String token = scanner.next().toLowerCase();

      if (!_termCorpusFrequency.containsKey(token)) {
        _termCorpusFrequency.put(token, 1);
      } else {
        _termCorpusFrequency.put(token, _termCorpusFrequency.get(token) + 1);
      }

      if (tmpUncompressedInvertedIndex.containsKey(token)) {
        tmpUncompressedInvertedIndex.get(token)
            .set(1, tmpUncompressedInvertedIndex.get(token).get(1) + 1);
        tmpUncompressedInvertedIndex.get(token).add(offset);
      } else {
        List<Integer> tmpList = new ArrayList<Integer>();
        tmpList.add(docid);
        tmpList.add(1);
        tmpList.add(offset);
        tmpUncompressedInvertedIndex.put(token, tmpList);
      }

      _totalTermFrequency++;
      offset++;
    }

    // Copy back...
    for (Map.Entry entry : tmpUncompressedInvertedIndex.entrySet()) {
      String term = (String) entry.getKey();
      List<Integer> termPostingList = (List<Integer>) entry.getValue();
      List<Integer> tmpOffsetList = new ArrayList<Integer>();

      if (uncompressedInvertedIndex.containsKey(term)) {
        List<Integer> tmpIndexList = new ArrayList<Integer>();
        tmpIndexList = uncompressedInvertedIndex.get(term);

        tmpOffsetList = uncompressedDocIdOffsetsMap.get(term);

        tmpOffsetList.add(tmpIndexList.size());
        tmpIndexList.addAll(termPostingList);

        uncompressedInvertedIndex.put(term, tmpIndexList);
        uncompressedDocIdOffsetsMap.put(term, tmpOffsetList);
      } else {
        tmpOffsetList.add(0);
        uncompressedInvertedIndex.put(term, termPostingList);
        uncompressedDocIdOffsetsMap.put(term, tmpOffsetList);
      }
    }
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

  /**
   * Populate the compressed invertedIndex.
   * It will compress by using delta instead of index.
   * Then it will compress by using v-byte encoding.
   */
  private void populateCompressedInvertedIndex() {
    for (Map.Entry entry : uncompressedInvertedIndex.entrySet()) {
      List<Integer> termInfoList = (List<Integer>) entry.getValue();
      List<Byte> deltaList = new ArrayList<Byte>();
      String term = (String) entry.getKey();

      int curr = 0;
      int prevDocId = -1;
      int prevOffset = -1;

      while (curr < termInfoList.size()) {
        List<Integer> offsets = new ArrayList<Integer>();
        int docId = termInfoList.get(curr);
        curr++;
        int occurs = termInfoList.get(curr);
        curr++;
        for (int i = 0; i < occurs; i++) {
          offsets.add(termInfoList.get(curr));
          curr++;
        }

        // Get the delta for docid
        if (prevDocId != -1) {
          docId = docId - prevDocId;
        }

        // Get the delta for offsets
        prevOffset = offsets.get(0);
        for (int i = 1; i < offsets.size(); i++) {
          offsets.set(i, offsets.get(i) - prevOffset);
        }

        deltaList.addAll(vByteEncoding(docId));
        deltaList.addAll(vByteEncoding(occurs));
        for (Integer offset : offsets) {
          deltaList.addAll(vByteEncoding(offset));
        }
      }

      invertedIndex.put(term, deltaList);
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
    if ((b >> 1 & 1) == 1) {
      // The first bit is 1, it's the end.
      return true;
    } else {
      return false;
    }
  }

  /**
   * Decode a list of bytes to one integer number
   *
   * @param bytes
   * @return
   */
  private int vByteDecoding(List<Byte> bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      String s = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
      sb.append(s.substring(1));
    }

    return Integer.parseInt(sb.toString(), 2);
  }

  /**
   * End a integer number to a list of 1-4 bytes
   *
   * @param num
   * @return
   */
  private List<Byte> vByteEncoding(int num) {
    List<Byte> bytes = new ArrayList<Byte>();
    // Get the binary string of the number
    String binaryStr = Integer.toBinaryString(num);
    // This is the number of bytes the number will need
    int length = (binaryStr.length() - 1) / 7 + 1;

    int start = 0;
    int end = 0;

    // Get one byte at a time
    for (int i = 0; i < length; i++) {
      if (i == 0) {
        end = binaryStr.length() - 7 * (length - 1);
      } else {
        start = end;
        end += 7;
      }

      String s = binaryStr.substring(start, end);
      byte b = 0;
      b = Byte.parseByte(s, 2);

      if (i == 0 && length == 1) {
        b = (byte) (b | (1 << 7));
      } else if (i == length - 1) {
        b = (byte) (b | (1 << 7));
      }

      bytes.add(b);
    }

    return bytes;
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