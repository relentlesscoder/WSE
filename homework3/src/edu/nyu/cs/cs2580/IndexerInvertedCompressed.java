package edu.nyu.cs.cs2580;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.google.common.collect.*;
import com.google.common.primitives.Bytes;
import edu.nyu.cs.cs2580.SearchEngine.Options;
import edu.nyu.cs.cs2580.VByteEncode.VByteUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This is the compressed inverted indexer...
 */
public class IndexerInvertedCompressed extends Indexer implements Serializable {

  private static final long serialVersionUID = 1L;
  // K is the length of interval for the skip pointer of the posting list.
  private static final int K = 5000;

  // Dictionary
  // Key: Term
  // Value: Term ID
  private BiMap<String, Integer> dictionary = HashBiMap.create();

  // Compressed inverted index, dynamically loaded per term at run time
  // Key: Term ID
  // Value: Compressed posting list.
  private ListMultimap<Integer, Byte> invertedIndex = ArrayListMultimap.create();

  // Term frequency of each document, dynamically loaded per doc at run time
  // Key: Docid
  // Value: Key: Term ID
  // Value: Value: Term frequency
  private Map<Integer, Multiset<Integer>> docTermFrequency = new HashMap<Integer, Multiset<Integer>>();

  // The offset of each docid of the posting list for each term.
  // Key: Term ID
  // Value: The offsets for each of docid in the posting list.
  private ListMultimap<Integer, Byte> skipPointers = ArrayListMultimap.create();

  // Key: Term ID
  // Value: MetaData
  // MetaData {
  //   corpusTermFrequency: Term frequency across whole corpus.
  //   corpusDocFrequencyByTerm: Number of documents a term appeared, over the full corpus.
  //   postingListMetaData
  // }
  private Map<Integer, MetaData> meta = new HashMap<Integer, MetaData>();

  // Key: Document ID
  // Value: Term document frequency meta info
  private Map<Integer, MetaPair> docTermFreqMeta = new HashMap<Integer, MetaPair>();

  private List<DocumentIndexed> documents = new ArrayList<DocumentIndexed>();

  long totalTermFrequency = 0;
  long totalNumViews = 0;

  // Provided for serialization
  public IndexerInvertedCompressed() {
  }

  public IndexerInvertedCompressed(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  /**
   * Write partial file
   *
   * @param fileCount the file count
   * @throws IOException
   */
  private void writePartialFile(int fileCount) throws IOException {
//    long startTimeStamp, duration;
//    startTimeStamp = System.currentTimeMillis();

    Util.writePartialInvertedIndexCompress(invertedIndex, _options, fileCount);
    Util.writePartialDocuments(docTermFrequency, _options, fileCount);
    invertedIndex.clear();
    docTermFrequency.clear();

//    duration = System.currentTimeMillis() - startTimeStamp;
//    System.out.println(Util.convertMillis(duration));
  }

  private class ConstructTmpData {
    public int lastDocid;
    public int lastPostingListSize;
    public int lastSkipPointerOffset;

    public ConstructTmpData() {
      lastDocid = -1;
      lastPostingListSize = -1;
      lastSkipPointerOffset = -1;
    }
  }

  @Override
  public void constructIndex() throws IOException {
    // Temporary data structure
    Map<Integer, ConstructTmpData> constructTmpDataMap = new HashMap<Integer, ConstructTmpData>();

    long totalStartTimeStamp = System.currentTimeMillis();
    long startTimeStamp, duration;

    ProgressBar progressBar = new ProgressBar();

    // Get the corpus folder
    File folder = new File(_options._corpusPrefix);
    //add filter to exclude hidden files
    FilenameFilter filenameFilter = new FilenameFilter() {
      @Override
      public boolean accept(File file, String name) {
        return !name.startsWith(".");
      }
    };
    File[] files = folder.listFiles(filenameFilter);
    int partialFileCount = 0;

    /**************************************************************************
     * First clean the folder....
     *************************************************************************/
    File outputFolder = new File(_options._indexPrefix);
    for (File file : outputFolder.listFiles()) {
      file.delete();
    }

    /**************************************************************************
     * Start indexing....
     *************************************************************************/
    startTimeStamp = System.currentTimeMillis();
    System.out.println("Start indexing...");

    // Process file/document one by one and assign each of them a unique docid
    for (int docid = 0; docid < files.length; docid++) {
      checkNotNull(files[docid], "File can not be null!");
      // Update the progress bar first :)
      progressBar.update(docid, files.length);
      // Now process the document
      processDocument(files[docid], docid, constructTmpDataMap);
      // Write to a file if memory usage has reach the memory threshold
      if (Util.hasReachThresholdCompress(invertedIndex)) {
        writePartialFile(partialFileCount);
        partialFileCount++;
      }
    }

    // Write the rest partial inverted index...
    writePartialFile(partialFileCount);

    // Get the number of documents
    _numDocs = files.length;

    duration = System.currentTimeMillis() - startTimeStamp;

    System.out.println("Complete indexing...");
    System.out.println("Indexing time: " + Util.convertMillis(duration));
    System.out.println("Indexed " + Integer.toString(_numDocs) + " docs with "
        + Long.toString(_totalTermFrequency) + " terms.");

    /**************************************************************************
     * Start merging....
     *************************************************************************/
    startTimeStamp = System.currentTimeMillis();
    System.out.println("Start merging...");

    try {
      mergePostingList();
      Util.mergeDocumentTermFrequency(docTermFreqMeta, _options);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    duration = System.currentTimeMillis() - startTimeStamp;
    System.out.println("Complete merging...");
    System.out.println("Merging time: " + Util.convertMillis(duration));

    /**************************************************************************
     * Serializing the rest...
     *************************************************************************/
    startTimeStamp = System.currentTimeMillis();
    System.out.println("Start serializing...");

    // Serialize the whole object :)
    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Storing inverted index to: " + _options._indexPrefix);

    ObjectOutputStream writer = new ObjectOutputStream(new BufferedOutputStream(new
        FileOutputStream(indexFile)));

    writer.writeObject(this);
    writer.close();

    duration = System.currentTimeMillis() - startTimeStamp;
    System.out.println("Complete serializing...");
    System.out.println("Serialization time: " + Util.convertMillis(duration));

    duration = System.currentTimeMillis() - totalStartTimeStamp;
    System.out.println("Mission complete :) 唉呀妈呀, 跑死我了... OTL");
    System.out.println("Total time: " + Util.convertMillis(duration));
  }

  /**
   * Process the document. First store the document, then populate the inverted
   * index.
   *
   * @param file  The file waiting to be indexed.
   * @param docid The file's document ID.
   * @throws IOException
   */
  private void processDocument(File file, int docid, Map<Integer, ConstructTmpData> constructTmpDataMap) throws IOException {
    org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(file, "UTF-8");

    if (file.getName().equals("Firestorm")) {
      int br = 0;
    }

    String bodyText = jsoupDoc.body().text();
    String title = jsoupDoc.title();

    // Create the document and store it.
    DocumentIndexed doc = new DocumentIndexed(docid, this);
    doc.setTitle(title);
    doc.setUrl(file.getName());

    documents.add(doc);

    // Populate the inverted index.
    populateInvertedIndex(title + " " + bodyText, docid, constructTmpDataMap);

    // TODO: Deal with all the links later...
    // Elements links = jsoupDoc.select("a[href]");
  }

  /**
   * Populate the inverted index.
   *
   * @param content The text content of the html document.
   * @param docid   The document ID.
   */
  private void populateInvertedIndex(String content, int docid, Map<Integer, ConstructTmpData> constructTmpDataMap) {
    // Uncompressed temporary inverted index.
    // Key is the term and value is the uncompressed posting list.
    ListMultimap<Integer, Integer> tmpInvertedIndex = ArrayListMultimap.create();
    Tokenizer tokenizer = new Tokenizer(new StringReader(content));

    int position = 0;

    /**************************************************************************
     * Start to process the document one term at a time.
     *************************************************************************/
    while (tokenizer.hasNext()) {
      // Tokenizer... Stemmer... Filter...
      String term = Tokenizer.lowercaseFilter(tokenizer.getText());
      term = Tokenizer.krovetzStemmerFilter(term);

      if (term == null) {
        continue;
      }

      // Update the total term frequency
      _totalTermFrequency++;
      totalTermFrequency++;

      // Update dictionary
      if (!dictionary.containsKey(term)) {
        int termId = dictionary.size();
        dictionary.put(term, termId);
        ConstructTmpData constructTmpData = new ConstructTmpData();
        constructTmpDataMap.put(termId, constructTmpData);
      }

      // Get the term ID for later use
      int termId = dictionary.get(term);

      // Update the meta data
      if (!meta.containsKey(termId)) {
        MetaData metaData = new MetaData();
        meta.put(termId, metaData);
      }

      // Update the term frequency across the whole corpus.
      long corpusTermFrequency = meta.get(termId).getCorpusTermFrequency();
      meta.get(termId).setCorpusTermFrequency(corpusTermFrequency + 1);

      // Update docTermFrequency
      if (!docTermFrequency.containsKey(docid)) {
        Multiset<Integer> termFrequencyOfDocid = HashMultiset.create();
        docTermFrequency.put(docid, termFrequencyOfDocid);
      }
      docTermFrequency.get(docid).add(termId);

      // Populate the temporary inverted index.
      if (tmpInvertedIndex.containsKey(termId)) {
        // The term has already been seen at least once in the document.
        int occurs = tmpInvertedIndex.get(termId).get(1);
        // Update the occurrence
        tmpInvertedIndex.get(termId).set(1, occurs + 1);
        // Add the position of this term
        tmpInvertedIndex.get(termId).add(position);
      } else {
        // This is the first time the term has been seen in the document
        if (constructTmpDataMap.get(termId).lastDocid != -1) {
          // The inverted index has already seen the term in previous
          // documents

          // Get the last/previous docid of the term's posting list
          int prevDocid = constructTmpDataMap.get(termId).lastDocid;
          int deltaDocid = docid - prevDocid;
          tmpInvertedIndex.get(termId).add(deltaDocid);

          if ((constructTmpDataMap.get(termId).lastPostingListSize - constructTmpDataMap.get(termId).lastSkipPointerOffset) > K) {
            skipPointers.get(termId).addAll(VByteUtil.vByteEncoding(prevDocid));
            skipPointers.get(termId).addAll(
                VByteUtil.vByteEncoding(constructTmpDataMap.get(termId).lastPostingListSize));

            constructTmpDataMap.get(termId).lastSkipPointerOffset
                = constructTmpDataMap.get(termId).lastPostingListSize;
          }
        } else {
          // The inverted index hasn't seen the term in previous documents
          // No need to calculate the delta since it's the first docid of
          // the posting list.
          tmpInvertedIndex.get(termId).add(docid);

          constructTmpDataMap.get(termId).lastDocid = 0;
          constructTmpDataMap.get(termId).lastPostingListSize = 0;
          constructTmpDataMap.get(termId).lastSkipPointerOffset = 0;
        }
        tmpInvertedIndex.get(termId).add(1);
        tmpInvertedIndex.get(termId).add(position);

        // Update the term frequency across the whole corpus.
        int corpusDocFrequencyByTerm = meta.get(termId).getCorpusDocFrequencyByTerm();
        meta.get(termId).setCorpusDocFrequencyByTerm(corpusDocFrequencyByTerm + 1);
      }

      // Move to the next position
      position++;
    }

    /**************************************************************************
     * Finish the process of all terms.
     *************************************************************************/
    documents.get(docid).setTotalDocTerms(++position);

    /**************************************************************************
     * Start to compress...
     *************************************************************************/

    // 1. Convert all positions of the posting list to deltas
    convertPositionToDelta(tmpInvertedIndex);

    // 2. Compress the temporary inverted index and populate the inverted index.
    for (int termId : tmpInvertedIndex.keySet()) {
      // Update lastDocid
      constructTmpDataMap.get(termId).lastDocid = docid;

      // Encode the posting list
      List<Byte> partialPostingList = VByteUtil.vByteEncodingList(tmpInvertedIndex
          .get(termId));

      // Append the posting list if one exists, otherwise create one first :)
      invertedIndex.get(termId).addAll(partialPostingList);
      constructTmpDataMap.get(termId).lastPostingListSize =
          constructTmpDataMap.get(termId).lastPostingListSize + partialPostingList.size();
    }
  }

  /**
   * Convert the offsets of each posting list to deltas. e.g. From: List: 0
   * (docid), 4 (occurrence), 5, 12, 18, 29 To: List: 0 (docid), 4 (occurrence),
   * 5, 7, 6, 11
   *
   * @param partialInvertedIndex The partial/temporary inverted index
   */
  private void convertPositionToDelta(
      ListMultimap<Integer, Integer> partialInvertedIndex) {
    for (int termId : partialInvertedIndex.keySet()) {
      List<Integer> list = partialInvertedIndex.get(termId);
      if (list.get(1) > 1) {
        for (int i = list.size() - 1; i > 2; i--) {
          int offset = list.get(i);
          int prevOffset = list.get(i - 1);
          list.set(i, offset - prevOffset);
        }
      }
    }
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
    IndexerInvertedCompressed loaded;
    File folder = new File(_options._indexPrefix);
    File[] files = folder.listFiles();

    /**************************************************************************
     * Load the class file first
     *************************************************************************/
    for (File file : files) {
      if (file.getName().equals("corpus.idx")) {
        String indexFile = _options._indexPrefix + "/corpus.idx";
        System.out.println("Load index from: " + indexFile);

        ObjectInputStream reader = new ObjectInputStream(new BufferedInputStream(new FileInputStream(indexFile)));
        loaded = (IndexerInvertedCompressed) reader.readObject();

        this.documents = loaded.documents;

        // Compute numDocs and totalTermFrequency b/c Indexer is not
        // serializable.
        this._numDocs = documents.size();
        this._totalTermFrequency = loaded.totalTermFrequency;
        this.totalNumViews = loaded.totalNumViews;

        this.invertedIndex = loaded.invertedIndex;
        this.docTermFrequency = loaded.docTermFrequency;

        this.skipPointers = loaded.skipPointers;

        this.meta = loaded.meta;
        this.docTermFreqMeta = loaded.docTermFreqMeta;
        this.dictionary = loaded.dictionary;
        reader.close();
        break;
      }
    }

    // Load the page ranks.
    // Key: Document ID
    // Value: Page rank score
    Map<Integer, Double> pageRanks = (Map<Integer, Double>) _corpusAnalyzer.load();

    // Load the number views
    // Key: Document ID
    // Value: Number of views
    Map<Integer, Integer> docNumView = (Map<Integer, Integer>) _logMiner.load();

    /**************************************************************************
     * Update the documents
     *************************************************************************/
    totalNumViews = 0;
    for (DocumentIndexed documentIndexed : documents) {
      int docid = documentIndexed._docid;
      // Update page rank score
      if (pageRanks.containsKey(docid)) {
        documentIndexed.setPageRank(pageRanks.get(docid));
      } else {
        documentIndexed.setPageRank(0.0);
      }
      // Update number of views
      if (docNumView.containsKey(docid)) {
        totalNumViews += docNumView.get(docid);
        documentIndexed.setNumViews(docNumView.get(docid));
      } else {
        documentIndexed.setNumViews(0);
      }
    }

    System.out.println(Integer.toString(_numDocs) + " documents loaded "
        + "with " + Long.toString(_totalTermFrequency) + " terms!");
  }

  @Override
  public DocumentIndexed getDoc(int docid) {
    return documents.get(docid);
  }

  @Override
  public Document nextDoc(Query query, int docid) {
    List<String> queryTerms = new ArrayList<String>(query._tokens);

    // Dynamic loading :)
    try {
      loadPostingList(queryTerms);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    // Get the next docid which satisfies the query terms
    int nextDocid = nextCandidateDocid(queryTerms, docid);

    return nextDocid == -1 ? null : documents.get(nextDocid);
  }

  /**
   * Return the next document which satisfy at least one of the term of the
   * query...
   *
   * @param query query
   * @param docid docid
   * @return Document
   */
  public Document nextDocLoose(Query query, int docid) {
    List<String> queryTerms = new ArrayList<String>(query._tokens);

    try {
      loadPostingList(queryTerms);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    int minDocid = Integer.MAX_VALUE;
    for (String term : queryTerms) {
      int nextDocid = nextDocid(term, docid);
      if (nextDocid != -1) {
        minDocid = Math.min(minDocid, nextDocid);
      }
    }

    if (minDocid == Integer.MAX_VALUE) {
      return null;
    } else {
      return documents.get(minDocid);
    }
  }

  /**
   * Return the next docid which satisfies the query terms, if none of such
   * docid can be found, return -1. This function uses document at a time
   * retrieval method.
   *
   * @param queryTerms A list of query terms
   * @param docid      The document ID
   * @return the next docid right after {@code docid} satisfying
   * {@code queryTerms} or -1 if no such document exists.
   */
  private int nextCandidateDocid(List<String> queryTerms, int docid) {
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
   * Return the next docid after the current one of the posting list, or -1 if
   * no such docid exists.
   *
   * @param term  The term...
   * @param docid The document ID
   * @return the next document ID after the current document or -1 if no such
   * document ID exists.
   */
  private int nextDocid(String term, int docid) {
    int termId = dictionary.get(term);
    List<Integer> partialSkipPointers = VByteUtil.vByteDecodingList(skipPointers
        .get(termId));

    int startOffsetOfPostingList = 0;
    int prevDocid = 0;

    if (docid >= 0) {
      // Get the start offset of the skip pointers...
      int startOffsetOfSkipPointers = getDocidPossibleSkipPointerStartOffset(
          partialSkipPointers, docid + 1);
      if (startOffsetOfSkipPointers >= 0) {
        // Skip...
        prevDocid = partialSkipPointers.get(startOffsetOfSkipPointers);
        startOffsetOfPostingList = partialSkipPointers
            .get(startOffsetOfSkipPointers + 1);
      }
    }

    return scanPostingListForNextDocid(term, docid, prevDocid,
        startOffsetOfPostingList);
  }

  /**
   * Check if the docid exists in the term's posting list.
   *
   * @param term  term
   * @param docid docid
   * @return true if the term exist in the doc with docid
   */
  private boolean hasDocid(String term, int docid) {
    int termId = dictionary.get(term);
    List<Integer> partialSkipPointers = VByteUtil.vByteDecodingList(skipPointers.get(termId));
    int startOffsetOfSkipPointers = getDocidPossibleSkipPointerStartOffset(
        partialSkipPointers, docid);
    int prevDocid = 0;
    int startOffsetOfPostingList = 0;

    if (startOffsetOfSkipPointers >= 0) {
      prevDocid = partialSkipPointers.get(startOffsetOfSkipPointers);
      startOffsetOfPostingList = partialSkipPointers
          .get(startOffsetOfSkipPointers + 1);
    }
    return scanPostingListForDocid(termId, docid, prevDocid,
        startOffsetOfPostingList);
  }

  /**
   * Search the skip pointers for the possible start offset for docid of the
   * posting list.
   *
   * @param docid docid
   * @return possible skip pointer start offset
   */
  private int getDocidPossibleSkipPointerStartOffset(
      List<Integer> partialSkipPointers, int docid) {
    int size = partialSkipPointers.size();

    if (size == 0 || docid <= partialSkipPointers.get(0)) {
      return -1;
    }

    if (docid > partialSkipPointers.get(size - 2)) {
      return size - 2;
    }

    // Use binary search to find if the {@code docid} exists in the list
    int low = 0;
    int high = (size - 1) / 2;

    while (low <= high) {
      int mid = low + (high - low) / 2;
      int midDocid = partialSkipPointers.get(mid * 2);
      if (midDocid == docid) {
        return (mid - 1) * 2;
      } else if (midDocid < docid) {
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }

    return high * 2;
  }

  /**
   * Scan the posting list for the next docid right after {@code targetDocid}.
   * It starts from the {@code startOffsetOfPostingList} with previous docid as
   * {@code prevDocid}
   *
   * @param term                     the term.
   * @param targetDocid              the target docid
   * @param prevDocid                docid before the start {@code startOffsetOfPostingList}
   * @param startOffsetOfPostingList the start offset of the posting list
   * @return the next docid or -1 if none exists
   */
  private int scanPostingListForNextDocid(String term, int targetDocid,
                                          int prevDocid, int startOffsetOfPostingList) {
    List<Byte> postingList = invertedIndex.get(dictionary.get(term));
    List<Byte> byteList = new ArrayList<Byte>();
    int nextDocid = prevDocid;
    int i = startOffsetOfPostingList;

    // Get the first docid from the posting list
    if (targetDocid == -1) {
      while (!VByteUtil.isEndOfNum(postingList.get(i))) {
        byteList.add(postingList.get(i++));
      }
      byteList.add(postingList.get(i));
      return VByteUtil.vByteDecoding(byteList);
    }

    while (true) {
      // No more docid in the posting list
      if (i >= postingList.size()) {
        return -1;
      }

      // Get the docid
      while (!VByteUtil.isEndOfNum(postingList.get(i))) {
        byteList.add(postingList.get(i++));
      }
      byteList.add(postingList.get(i++));
      nextDocid += VByteUtil.vByteDecoding(byteList);
      byteList.clear();

      // If the docid is larger than the targetDocid, then the next docid is
      // found
      if (nextDocid > targetDocid) {
        return nextDocid;
      }

      // Get the occurs
      while (!VByteUtil.isEndOfNum(postingList.get(i))) {
        byteList.add(postingList.get(i++));
      }
      byteList.add(postingList.get(i++));
      int occur = VByteUtil.vByteDecoding(byteList);
      byteList.clear();

      // Skip all position data...
      for (int j = 0; j < occur; j++) {
        // Get the occurs
        while (!VByteUtil.isEndOfNum(postingList.get(i))) {
          i++;
        }
        i++;
      }

      // Move on to the next docid...
    }
  }

  /**
   * Scan the posting list for the {@code targetDocid}. It starts from the
   * {@code startOffsetOfPostingList} with previous docid as {@code prevDocid}
   *
   * @param termId                   the termId.
   * @param targetDocid              the target docid
   * @param prevDocid                docid before the start {@code startOffsetOfPostingList}
   * @param startOffsetOfPostingList the start offset of the posting list
   * @return true if the docid exist, otherwise false
   */
  private boolean scanPostingListForDocid(int termId, int targetDocid,
                                          int prevDocid, int startOffsetOfPostingList) {
    List<Byte> postingList = invertedIndex.get(termId);
    List<Byte> byteList = new ArrayList<Byte>();
    int nextDocid = prevDocid;
    int i = startOffsetOfPostingList;

    while (true) {
      // No more docid in the posting list
      if (i >= postingList.size()) {
        return false;
      }

      // Get the docid
      while (!VByteUtil.isEndOfNum(postingList.get(i))) {
        byteList.add(postingList.get(i++));
      }
      byteList.add(postingList.get(i++));
      nextDocid += VByteUtil.vByteDecoding(byteList);
      byteList.clear();
      // If the docid is larger than the targetDocid, then the next docid is
      // found
      if (nextDocid == targetDocid) {
        return true;
      } else if (nextDocid > targetDocid) {
        return false;
      }

      // Get the occurs
      while (!VByteUtil.isEndOfNum(postingList.get(i))) {
        byteList.add(postingList.get(i++));
      }
      byteList.add(postingList.get(i++));
      int occur = VByteUtil.vByteDecoding(byteList);
      byteList.clear();

      // Skip all position data...
      for (int j = 0; j < occur; j++) {
        // Get the occurs
        while (!VByteUtil.isEndOfNum(postingList.get(i))) {
          i++;
        }
        i++;
      }

      // Move on to the next docid...
    }
  }

  /**
   * Scan the posting list for the {@code targetDocid} offset of the posting
   * list. It starts from the {@code startOffsetOfPostingList} with previous
   * docid as {@code prevDocid}
   *
   * @param termId                   the termId.
   * @param targetDocid              the target docid
   * @param prevDocid                docid before the start {@code startOffsetOfPostingList}
   * @param startOffsetOfPostingList the start offset of the posting list
   * @return the docid offset of the posting list
   */
  private int scanPostingListForDocidOffset(int termId, int targetDocid,
                                            int prevDocid, int startOffsetOfPostingList) {
    List<Byte> postingList = invertedIndex.get(termId);
    List<Byte> byteList = new ArrayList<Byte>();
    int offset;
    int nextDocid = prevDocid;
    int i = startOffsetOfPostingList;

    while (true) {
      // No more docid in the posting list
      if (i >= postingList.size()) {
        return -1;
      }

      offset = i;

      // Get the docid
      while (!VByteUtil.isEndOfNum(postingList.get(i))) {
        byteList.add(postingList.get(i++));
      }
      byteList.add(postingList.get(i++));
      nextDocid += VByteUtil.vByteDecoding(byteList);
      byteList.clear();

      // If the docid is larger than the targetDocid, then the next docid is
      // found
      if (nextDocid == targetDocid) {
        return offset;
      } else if (nextDocid > targetDocid) {
        return -1;
      }

      // Get the occurs
      while (!VByteUtil.isEndOfNum(postingList.get(i))) {
        byteList.add(postingList.get(i++));
      }
      byteList.add(postingList.get(i++));
      int occur = VByteUtil.vByteDecoding(byteList);
      byteList.clear();

      // Skip all position data...
      for (int j = 0; j < occur; j++) {
        // Get the occurs
        while (!VByteUtil.isEndOfNum(postingList.get(i))) {
          i++;
        }
        i++;
      }

      // Move on to the next docid...
    }
  }

  /**
   * Get the docid offset of the posting list
   *
   * @param termId the termId
   * @param docid  the docid
   * @return the docid offset of the posting list
   */
  private int getDocidOffset(int termId, int docid) {
    List<Integer> partialSkipPointers = VByteUtil.vByteDecodingList(skipPointers
        .get(termId));
    int startOffsetOfPostingList = 0;
    int prevDocid = 0;

    // Get the start offset of the skip pointers...
    int startOffsetOfSkipPointers = getDocidPossibleSkipPointerStartOffset(
        partialSkipPointers, docid);
    if (startOffsetOfSkipPointers >= 0) {
      // Skip...
      prevDocid = partialSkipPointers.get(startOffsetOfSkipPointers);
      startOffsetOfPostingList = partialSkipPointers
          .get(startOffsetOfSkipPointers + 1);
    }

    return scanPostingListForDocidOffset(termId, docid, prevDocid,
        startOffsetOfPostingList);
  }

  /**
   * Return the next position for a term after {@code pos} in a document.
   *
   * @param term  The term...
   * @param docid The document ID
   * @param pos   The position of the term in the document
   * @return the next position for the term in the document. If no more term in
   * the next, return -1.
   */
  public int nextPos(String term, int docid, int pos) {
    int termId = dictionary.get(term);

    if (!invertedIndex.containsKey(termId)) {
      return -1;
    }

    // Get the decompressed docidOffsetList
    List<Byte> postingList = invertedIndex.get(termId);
    List<Byte> tmpList = new ArrayList<Byte>();
    int offset = getDocidOffset(termId, docid);

    int occur;
    int currPos = 0;

    // Skip the doc id first
    while (!VByteUtil.isEndOfNum(postingList.get(offset++))) {
    }

    // Get the occurs
    while (!VByteUtil.isEndOfNum(postingList.get(offset))) {
      tmpList.add(postingList.get(offset++));
    }
    tmpList.add(postingList.get(offset++));
    occur = VByteUtil.vByteDecoding(tmpList);
    tmpList.clear();

    for (int i = 0; i < occur; i++) {
      // Get the occurs
      while (!VByteUtil.isEndOfNum(postingList.get(offset))) {
        tmpList.add(postingList.get(offset++));
      }
      tmpList.add(postingList.get(offset++));
      currPos += VByteUtil.vByteDecoding(tmpList);
      tmpList.clear();

      if (currPos > pos) {
        return currPos;
      }
    }

    // No more term...
    return -1;
  }

  @Override
  public int corpusDocFrequencyByTerm(String term) {
    if (meta.containsKey(dictionary.get(term))) {
      return meta.get(dictionary.get(term)).getCorpusDocFrequencyByTerm();
    } else {
      return 0;
    }
  }

  @Override
  public int corpusTermFrequency(String term) {
    if (meta.containsKey(dictionary.get(term))) {
      return (int) meta.get(dictionary.get(term)).getCorpusTermFrequency();
    } else {
      return 0;
    }
  }

  @Override
  public int documentTermFrequency(String term, int docid) {
    int docTermFrequency;
    int termId = dictionary.get(term);
    int offset = getDocidOffset(termId, docid);

    if (offset == -1) {
      return 0;
    }

    List<Byte> postingList = invertedIndex.get(termId);
    List<Byte> tmpList = new ArrayList<Byte>();

    // Skip the doc id first
    while (!VByteUtil.isEndOfNum(postingList.get(offset++))) {
    }

    // Get the occurs
    while (!VByteUtil.isEndOfNum(postingList.get(offset))) {
      tmpList.add(postingList.get(offset++));
    }
    tmpList.add(postingList.get(offset++));
    docTermFrequency = VByteUtil.vByteDecoding(tmpList);

    return docTermFrequency;
  }

  /**
   * Get the docTermFrequency of a specific document
   *
   * @param docid document id
   * @return the term frequency
   */
  public Multiset<Integer> getDocidTermFrequency(int docid) {
    if (!docTermFreqMeta.containsKey(docid)) {
      return HashMultiset.create();
    } else if (!docTermFrequency.containsKey(docid)) {
      loadTermDocFrequency(docid);
    }

    return docTermFrequency.get(docid);
  }

  /**
   * Get the term via term ID
   *
   * @param termId term ID
   * @return the term as a String, if the term does not exist in the
   *         dictionary, return an empty String instead.
   */
  public String getTermById(int termId) {
    if (dictionary.inverse().containsKey(termId)) {
      return dictionary.inverse().get(termId);
    } else {
      return "";
    }
  }

  /**
   * Get the term ID via term
   *
   * @param term term
   * @return the term ID as an Integer, if the term does not exists in
   *         the dictionary, return -1.
   */
  public int getTermId(String term) {
    if (dictionary.containsKey(term)) {
      return dictionary.get(term);
    } else {
      return -1;
    }
  }

  /**
   * Get the total number of views
   * @return {@code totalNumViews}
   */
  public long getTotalNumViews() {
    return totalNumViews;
  }

  /**
   * Merge all partial index files into a single file.
   *
   * @throws IOException
   * @throws ClassNotFoundException
   */
  private void mergePostingList() throws IOException, ClassNotFoundException {
    String invertedIndexFileName = _options._indexPrefix + "/main.idx";
    RandomAccessFile raf = new RandomAccessFile(invertedIndexFileName, "rw");
    long currentPos = 0;
    int length = 0;

    /**************************************************************************
     * Prepare merging...
     *************************************************************************/
    File folder = new File(_options._indexPrefix);
    int numOfPartialIndex = 0;

    // Get the number of partial index file
    for (File f : folder.listFiles()) {
      if (f.getName().matches("^corpus[0-9]+\\.idx")) {
        numOfPartialIndex++;
      }
    }

    Kryo kryo = new Kryo();
    File[] files = new File[numOfPartialIndex];
    Input[] inputs = new Input[numOfPartialIndex];
    int[] termIds = new int[numOfPartialIndex];
    int[] numOfPostingList = new int[numOfPartialIndex];

    // Initialize the files, inputs and
    // Then get the quantity of the posting list for each partial file
    for (int i = 0; i < numOfPartialIndex; i++) {
      for (File file : folder.listFiles()) {
        if (file.getName().matches(
            "^corpus" + String.format("%03d", i) + "\\.idx")) {
          termIds[i] = -1;
          files[i] = file;
          inputs[i] = new Input(new FileInputStream(file.getAbsolutePath()));
          numOfPostingList[i] = kryo.readObject(inputs[i], Integer.class);
          break;
        }
      }
    }

    /**************************************************************************
     * Start merging...
     *************************************************************************/
    while (hasMorePostingList(numOfPostingList)) {
      // Start to process the next posting list

      // Key: Term ID
      // Value: List of sorted file index
      SortedSetMultimap<Integer, Integer> sortedTermAndFileIndex = TreeMultimap
          .create(Ordering.natural(), Ordering.natural());

      int outputTermID = -1;
      List<Byte> outputPostingList = new ArrayList<Byte>();

      // First read the next term from each of the partial index file if it has
      // more.
      for (int i = 0; i < numOfPartialIndex; i++) {
        if (termIds[i] == -1 && numOfPostingList[i] > 0) {
          numOfPostingList[i]--;
          termIds[i] = kryo.readObject(inputs[i], Integer.class);
        }
      }

      // Sort all next termIds by alphabetical order
      // For two same termIds, sort their file number to
      for (int i = 0; i < numOfPartialIndex; i++) {
        if (termIds[i] != -1) {
          sortedTermAndFileIndex.put(termIds[i], i);
        }
      }

      // Retrieve the first term and posting list according to the sorted result
      for (Map.Entry entry : sortedTermAndFileIndex.entries()) {
        outputTermID = (Integer) entry.getKey();
        for (int i : sortedTermAndFileIndex.asMap().get(outputTermID)) {
          outputPostingList.addAll(kryo.readObject(inputs[i], ArrayList.class));
          termIds[i] = -1;
        }
        break;
      }

      currentPos = raf.length();
      raf.seek(currentPos);
      raf.write(Bytes.toArray(outputPostingList));

      // Assume the posting list will not be too big...
      length = (int) (raf.length() - currentPos);
      meta.get(outputTermID).setPostingListMetaData(new MetaPair(currentPos, length));
    }

    raf.close();

    /**************************************************************************
     * Wrapping up...
     *************************************************************************/
    for (File f : folder.listFiles()) {
      if (f.getName().matches("^corpus[0-9]+\\.idx")) {
        // Delete all partial index file
        f.delete();
      }
    }
  }

  /**
   * Check if there's still at least one posting list needed to be merged.
   *
   * @param numOfPostingList the number of posting list of each partial index file.
   * @return true if there's at least one posting list needed to be merged,
   * otherwise false.
   */
  private boolean hasMorePostingList(int[] numOfPostingList) {
    for (int i : numOfPostingList) {
      if (i > 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Dynamically load posting list at run time
   *
   * @param query the query terms
   * @throws IOException
   * @throws ClassNotFoundException
   */
  private void loadPostingList(List<String> query) throws IOException,
      ClassNotFoundException {
    String invertedIndexFileName = _options._indexPrefix + "/main.idx";
    boolean hasAlready = true;
    MetaPair metaPair;
    int count = 0;

    // Clean if not enough memory...
    if (invertedIndex.keys().size() > Util.MAX_INVERTED_INDEX_SIZE) {
      invertedIndex.clear();
    }

    for (String term : query) {
      int termId = dictionary.get(term);
      if (!invertedIndex.containsKey(termId)) {
        hasAlready = false;
      }
    }

    // All query terms are already loaded...
    if (hasAlready) {
      return;
    }

    System.out.println("Start dynamic loading...");
    long startTimeStamp = System.currentTimeMillis();

    RandomAccessFile raf = new RandomAccessFile(invertedIndexFileName, "r");

    for (String term : query) {
      // Load the posting list for a term if it's not already loaded.
      // Also check if it exists in the postingListMetaData, load it only if it exists.
      int termId = dictionary.get(term);
      if (meta.containsKey(termId)) {
        metaPair = meta.get(termId).getPostingListMetaData();
        raf.seek(metaPair.getStartPos());
        byte[] postingListBytes = new byte[metaPair.getLength()];
        raf.readFully(postingListBytes);
        List<Byte> postingList = Bytes.asList(postingListBytes);
        invertedIndex.get(termId).addAll(postingList);
        count++;
      }
    }

    raf.close();
    long duration = System.currentTimeMillis() - startTimeStamp;
    System.out.println("Complete loading " + count
        + " posting lists and takes time " + Util.convertMillis(duration));
  }

  private void loadTermDocFrequency(int docid) {
    if (!docTermFrequency.containsKey(docid) && docTermFreqMeta.containsKey(docid)) {
      String documentTermFrequencyFileName = _options._indexPrefix + "/documents.idx";
      RandomAccessFile raf = null;
      try {
        raf = new RandomAccessFile(documentTermFrequencyFileName, "r");
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }

      // Clean if not enough memory...
      // TODO: Fix later
      if (docTermFrequency.size() > 1000 ) {
        docTermFrequency.clear();
      }

      MetaPair metaPair = docTermFreqMeta.get(docid);
      try {
        raf.seek(metaPair.getStartPos());
        byte[] docTermFrequencyByte = new byte[metaPair.getLength()];
        raf.readFully(docTermFrequencyByte);
        Multiset<Integer> docTermFrequency = (Multiset<Integer>) Util.deserialize(docTermFrequencyByte);
        this.docTermFrequency.put(docid, docTermFrequency);
        raf.close();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
    }
  }
}