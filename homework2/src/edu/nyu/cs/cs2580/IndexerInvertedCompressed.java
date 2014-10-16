package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.SearchEngine.Options;
import org.jsoup.Jsoup;

import java.io.*;
import java.util.*;

/**
 * This is the compressed inverted indexer...
 */
public class IndexerInvertedCompressed extends Indexer implements Serializable {
  private static final long serialVersionUID = 1L;

  // Compressed inverted index.
  // Key is the term and value is the compressed posting list.
  private Map<String, List<Byte>> invertedIndex =
      new HashMap<String, List<Byte>>();

  // The offset of each docid of the posting list for each term.
  // Key is the term and value is the offsets for each of docid in the posting list.
  private Map<String, List<Byte>> postingListOffsetMap =
      new HashMap<String, List<Byte>>();

  // The last/previous docid of the posting list.
  // Key is the term and value is the last/previous docid of the posting list.
  // This is used to construct the index and will be cleared after the process.
  Map<String, Integer> lastDocid = new HashMap<String, Integer>();

  // Term frequency across whole corpus.
  // key is the term and value is the frequency of the term across the whole corpus.
  private Map<String, Integer> _termCorpusFrequency =
      new HashMap<String, Integer>();

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
    // Get all files first
    File folder = new File(_options._corpusPrefix);
    File[] files = folder.listFiles();

    if (files == null) {
      // If no files found, throws the exception...
      throw new NullPointerException("No files found in: " + folder.getPath());
    }

    // Process file/document one by one and assign each of them a unique docid
    for (int docid = 0; docid < files.length; docid++) {
      processDocument(files[docid], docid);
    }

    _numDocs = _documents.size();

    // Clear the previous document ID map since it's no longer needed...
    lastDocid.clear();

    System.out.println("Indexed " + Integer.toString(_numDocs)
        + " docs with " + Long.toString(_totalTermFrequency) + " terms.");

    // Write to file
    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Storing index to: " + indexFile);

    ObjectOutputStream writer =
        new ObjectOutputStream(new FileOutputStream(indexFile));
    writer.writeObject(this);
    writer.close();
    System.out.println("Mission completed :)");
  }

  /**
   * Process the document.
   * First store the document, then populate the inverted index.
   *
   * @param file  The file waiting to be indexed.
   * @param docid The file's document ID.
   * @throws IOException
   */
  private void processDocument(File file, int docid) throws IOException {
    org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(file, "UTF-8");

    // TODO: Temporary way for extracting the text and tile from the html file.
    String bodyText = jsoupDoc.body().text();
    String title = jsoupDoc.title();

    // Create the document and store it.
    DocumentIndexed doc = new DocumentIndexed(docid, this);
    doc.setTitle(title);
    doc.setUrl(file.getAbsolutePath());
    _documents.add(doc);

    // Populate the inverted index.
    populateInvertedIndex(title + " " + bodyText, docid);

    // TODO: Deal with all the links later...
//    Elements links = jsoupDoc.select("a[href]");
  }

  /**
   * Populate the inverted index.
   *
   * @param content The text content of the html document.
   * @param docid   The document ID.
   */
  private void populateInvertedIndex(String content, int docid) {
    // Uncompressed temporary inverted index.
    // Key is the term and value is the uncompressed posting list.
    Map<String, List<Integer>> tmpInvertedIndex =
        new HashMap<String, List<Integer>>();

    // The offset of the current docid of the posting list for each term.
    // Key is the term and value is the offsets for each of docid in the posting list.
    Map<String, Integer> tmpPostingListOffsetMap =
        new HashMap<String, Integer>();

    int position = 0;

    // TODO: Temporary. Need a better tokenizer...
    Scanner scanner = new Scanner(content).useDelimiter("\\W");

    /**************************************************************************
     * Start to process the document one term at a time.
     *************************************************************************/
    while (scanner.hasNext()) {
      // TODO: Temporary. Need stemming...
      String term = scanner.next().toLowerCase();
      if (term.equals("")) {
        continue;
      }

      // Populate the temporary inverted index.
      if (tmpInvertedIndex.containsKey(term)) {
        // The term has already been seen at least once in the document.
        int occurs = tmpInvertedIndex.get(term).get(1);
        // Update the occurrence
        tmpInvertedIndex.get(term).set(1, occurs + 1);
        // Add the position of this term
        tmpInvertedIndex.get(term).add(position);
      } else {
        // This is the first time the term has been seen in the document
        List<Integer> tmpList = new ArrayList<Integer>();
        if (invertedIndex.containsKey(term)) {
          // The inverted index has already seen the term in previous documents

          // Get the last/previous docid of the term's posting list
          int prevDocid = lastDocid.get(term);
          int deltaDocid = docid - prevDocid;
          tmpList.add(deltaDocid);

          // Get the offset for this docid of the posting list, store it temporarily.
          // It should be right after the last docid id of the posting list.
          tmpPostingListOffsetMap.put(term, invertedIndex.get(term).size());
        } else {
          // The inverted index hasn't seen the term in previous documents

          // No need to calculate the delta since it's the first docid of the posting list.
          // the delta.
          tmpList.add(docid);

          // Get the offset for this docid of the posting list, store it temporarily.
          // It should the first docid id of the posting list.
          tmpPostingListOffsetMap.put(term, 0);
        }
        tmpList.add(1);
        tmpList.add(position);
        tmpInvertedIndex.put(term, tmpList);
      }

      // Update the termCorpusFrequency
      if (!_termCorpusFrequency.containsKey(term)) {
        _termCorpusFrequency.put(term, 1);
      } else {
        _termCorpusFrequency.put(term, _termCorpusFrequency.get(term) + 1);
      }

      // Update the totalTermFrequency
      _totalTermFrequency++;
      // Move to the next position
      position++;
    }

    /**************************************************************************
     * Finish the process of all terms.
     *************************************************************************/

    /**************************************************************************
     * Start to compress...
     *************************************************************************/

    // 1. Convert all positions of the posting list to deltas
    convertPositionToDelta(tmpInvertedIndex);

    // 2. Compress the temporary inverted index and populate the inverted index.
    for (String term : tmpInvertedIndex.keySet()) {
      // Update lastDocid
      lastDocid.put(term, docid);

      // Get the offset of the term's docid of the posting list
      int offset = tmpPostingListOffsetMap.get(term);

      // Encode the posting list
      List<Byte> partialPostingList = vByteEncodingList(tmpInvertedIndex.get(term));

      if (invertedIndex.containsKey(term)) {
        // Update the docidOffsetMap for the term
        List<Byte> byteList = postingListOffsetMap.get(term);
        byteList.addAll(vByteEncoding(offset));
        postingListOffsetMap.put(term, byteList);

        // Append the posting list to a existing one
        List<Byte> postingList = invertedIndex.get(term);
        postingList.addAll(partialPostingList);
        invertedIndex.put(term, postingList);
      } else {
        // Set the initial offset for the term in docidOffsetMap
        postingListOffsetMap.put(term, vByteEncoding(offset));

        // Set the initial posting list for the term
        invertedIndex.put(term, partialPostingList);
      }
    }
  }

  /**
   * Convert the offsets of each posting list to deltas.
   * e.g.
   * From: List: 0 (docid), 4 (occurrence), 5, 12, 18, 29
   * To: List: 0 (docid), 4 (occurrence), 5, 7, 6, 11
   *
   * @param partialInvertedIndex The partial/temporary inverted index
   */
  private void convertPositionToDelta(Map<String, List<Integer>> partialInvertedIndex) {
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
   * Check if the byte is the end of the number.
   *
   * @param b A v-byte encoded byte.
   * @return true if the byte is the last byte of the number (the highest bit is 1), otherwise false.
   */
  private boolean isEndOfNum(byte b) {
    return (b >> 7 & 1) == 1;
  }

  /**
   * Decode a list of byteList to one integer number
   *
   * @param byteList A list of v-byte encoded bytes needed to be decoded
   * @return The decoded number
   */
  private int vByteDecoding(List<Byte> byteList) {
    StringBuilder sb = new StringBuilder();

    // Append all byteList together
    for (byte b : byteList) {
      sb.append(Integer.toBinaryString(b & 255 | 256).substring(2));
    }

    // Return the integer number
    return Integer.parseInt(sb.toString(), 2);
  }

  /**
   * Encode a integer number to a list of bytes
   *
   * @param num The number needed to be encoded
   * @return a list of v-byte encoded byte represents the input number
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
      byte b;

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
   * @param byteList A list of v-byte encoded bytes which represents a list of number
   * @return a list of decoded integer numbers
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
   * @param list A list of integer numbers needed to be encoded
   * @return a list of v-byte encoded bytes
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
    this.postingListOffsetMap = loaded.postingListOffsetMap;
    this._termCorpusFrequency = loaded._termCorpusFrequency;
    reader.close();

    System.out.println(Integer.toString(_numDocs) + " documents loaded " +
        "with " + Long.toString(_totalTermFrequency) + " terms!");
  }

  @Override
  public Document getDoc(int docid) {
    return _documents.get(docid);
  }

  @Override
  public Document nextDoc(Query query, int docid) {
    Vector<String> queryTerms = query._tokens;

    // Get the next docid which satisfies the query terms
    int nextDocid = nextCandidateDocid(queryTerms, docid);

    return nextDocid == -1 ? null : _documents.get(nextDocid);
  }

  /**
   * Return the next docid which satisfies the query terms, if none of such docid
   * can be found, return -1.
   * <p/>
   * This function uses document at a time retrieval method.
   *
   * @param queryTerms A list of query terms
   * @param docid      The document ID
   * @return the next docid right after {@code docid} satisfying {@code queryTerms}
   * or -1 if no such document exists.
   */
  private int nextCandidateDocid(Vector<String> queryTerms, int docid) {
    int largestDocid = -1;

    // For each query term's document ID list, find the largest docId because it
    // is a reasonable candidate.
    for (String term : queryTerms) {
      // Get the next document ID next to the current {@code docid} in the list
      int nextDocid = nextDocid(term, docid);
      if (nextDocid == -1) {
        // The next document ID does not exist...
        return -1;
      }
      largestDocid = Math.max(largestDocid, nextDocid);
    }

    // Check if the largest document ID satisfy all query terms.
    for (String term : queryTerms) {
      if (!hasDocid(term, largestDocid)) {
        // The largest docid does not satisfy this term, hence check the next
        return nextCandidateDocid(queryTerms, largestDocid);
      }
    }

    // If the satisfied docid has been found, return it.
    return largestDocid;
  }

  /**
   * Return the next docid after the current one of the posting list, or -1
   * if no such docid exists.
   *
   * @param term  The term...
   * @param docid The document ID
   * @return the next document ID after the current document or -1 if no such document
   * ID exists.
   */
  private int nextDocid(String term, int docid) {
    // Get the decoded posting list
    List<Integer> postingList = vByteDecodingList(postingListOffsetMap.get(term));
    int size = postingList.size();

    // Base case.
    // If the size is 0 or the last docid of the posting list is smaller
    // than the current docid, return -1.
    int lastDocid =
        getDocidByOffset(postingList, term, postingList.get(size - 1));
    if (size == 0 || lastDocid <= docid) {
      return -1;
    }

    // If first docid of the posting list is larger than the current docid, just
    // return the first docid.
    int firstDocid = getDocidByOffset(postingList, term, postingList.get(0));
    if (firstDocid > docid) {
      return firstDocid;
    }

    // Use binary search to get the docid right after the current {@code docid}
    int low = 0;
    int high = postingList.size() - 1;

    while (high - low > 1) {
      int mid = low + (high - low) / 2;
      int midDocid = getDocidByOffset(postingList, term, postingList.get(mid));
      if (midDocid <= docid) {
        low = mid;
      } else {
        high = mid;
      }
    }

    return getDocidByOffset(postingList, term, postingList.get(high));
  }

  /**
   * Check if the docid exists in the term's posting list.
   *
   * @param term  The term...
   * @param docid The document ID
   * @return true if the docid exists in the term's posting list, otherwise false
   */
  private boolean hasDocid(String term, int docid) {
    return getDocidOffset(term, docid) != -1;
  }

  /**
   * Get the docid offset of the posting list for the term
   *
   * @param term  The term...
   * @param docid The document ID
   * @return the docid offset of the posting list.
   */
  private int getDocidOffset(String term, int docid) {
    List<Byte> docidUncompressedList = postingListOffsetMap.get(term);
    List<Integer> postingList = vByteDecodingList(docidUncompressedList);

    int size = postingList.size();

    int lastDocid =
        getDocidByOffset(postingList, term, postingList.get(size - 1)) ;
    if (size == 0 || lastDocid < docid) {
      return -1;
    }

    // Use binary search to find if the {@code docid} exists in the list
    int low = 0;
    int high = postingList.size() - 1;

    while (low <= high) {
      int mid = low + (high - low) / 2;
      int midDocid = getDocidByOffset(postingList, term, postingList.get(mid));
      if (midDocid == docid) {
        return mid;
      } else if (midDocid < docid) {
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }

    return -1;
  }

  /**
   * Get the docid from the term's posting list by the offset.
   *
   * @param term   The term...
   * @param offset The offset of the docid
   * @return the docid
   */
  private int getDocidByOffset(List<Integer> docidList, String term, int offset) {
    List<Byte> byteList = new ArrayList<Byte>();
    int docid = 0;
    int count = 0;
    int i = docidList.get(count);

    // Get all previous docid delta to calculate the docid
    while (i <= offset) {
      while (!isEndOfNum(invertedIndex.get(term).get(i))) {
        byteList.add(invertedIndex.get(term).get(i++));
      }
      byteList.add(invertedIndex.get(term).get(i));
      docid += vByteDecoding(byteList);

      count++;
      if (count == docidList.size()) {
        break;
      }
      byteList.clear();
      i = docidList.get(count);
    }
    return docid;
  }

  /**
   * Return the next position for a term after {@code pos} in a document.
   *
   * @param term  The term...
   * @param docid The document ID
   * @param pos   The position of the term in the document
   * @return the next position for the term in the document. If no more term in the
   * next, return -1.
   */
  public int nextPos(String term, int docid, int pos) {
    List<Byte> postingList = invertedIndex.get(term);
    List<Byte> tmpList = new ArrayList<Byte>();
    int offset = getDocidOffset(term, docid);
    int occur = -1;
    int currPos = -1;

    // Skip the doc id first
    while (!isEndOfNum(postingList.get(offset++))) {
    }

    // Get the occurs
    while (!isEndOfNum(postingList.get(offset))) {
      tmpList.add(postingList.get(offset++));
    }
    tmpList.add(postingList.get(offset++));
    occur = vByteDecoding(tmpList);
    tmpList.clear();

    for (int i = 0; i < occur; i++) {
      // Get the occurs
      while (!isEndOfNum(postingList.get(offset))) {
        tmpList.add(postingList.get(offset++));
      }
      tmpList.add(postingList.get(offset++));
      currPos = vByteDecoding(tmpList);
      tmpList.clear();

      if (currPos > pos) {
        return currPos;
      }
    }

    // No more term...
    return currPos;
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