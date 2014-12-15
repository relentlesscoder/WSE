package edu.nyu.cs.cs2580.index;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.google.common.collect.*;
import com.google.common.primitives.Bytes;
import edu.nyu.cs.cs2580.ngram.NGramSpellChecker;
import edu.nyu.cs.cs2580.ngram.SpellCheckResult;
import edu.nyu.cs.cs2580.query.Query;
import edu.nyu.cs.cs2580.rankers.IndexerConstant;
import edu.nyu.cs.cs2580.SearchEngine;
import edu.nyu.cs.cs2580.SearchEngine.Options;
import edu.nyu.cs.cs2580.utils.Util;
import edu.nyu.cs.cs2580.utils.VByteUtil;
import edu.nyu.cs.cs2580.document.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the compressed inverted indexer...
 */
public class IndexerInvertedCompressed extends Indexer implements Serializable {
  private static final long serialVersionUID = 1L;
  // K is the length of interval for the skip pointer of the posting list.
  private static final int K = 10000;

  // Compressed inverted index, dynamically loaded per term at run time
  // Key: Term ID
  // Value: Compressed posting list.
  private ListMultimap<Integer, Byte> invertedIndex;

  // Term frequency of each document, dynamically loaded per doc at run time
  // Key: Docid
  // Value: Key: Term ID
  // Value: Value: Term frequency
  private Map<Integer, Multiset<Integer>> docTermFrequency;

  // Dictionary
  // Key: Term
  // Value: Term ID
  private ImmutableBiMap<String, Integer> dictionary;

  // The offset of each docid of the posting list for each term.
  // Key: Term ID
  // Value: The offsets for each of docid in the posting list.
  private ListMultimap<Integer, Byte> skipPointers;

  // Key: Term ID
  // Value: MetaData
  // MetaData {
  //   corpusTermFrequency: Term frequency across whole corpus.
  //   corpusDocFrequencyByTerm: Number of documents a term appeared, over the full corpus.
  //   postingListMetaData
  // }
  private ImmutableMap<Integer, MetaData> meta;

  // Key: document ID
  // Value: Term document frequency meta info
  private ImmutableMap<Integer, Offsets> docTermFreqMeta;

  private ImmutableList<Document> documents;

  private Map<Integer, ExtentList> extentListMap;

  private SearchEngine.CORPUS_TYPE corpusType;

  private NGramSpellChecker spellChecker;

  private String CORPUS_INDEX;
  private String DOCUMENTS;
  private String META;

  private long totalTermFrequency = 0;
  private long totalNumViews = 0;

  private static final  String SPELL_CHECK_INDEX = "spell_check";

  // Provided for serialization
  public IndexerInvertedCompressed() {
  }

  public IndexerInvertedCompressed(Options options, SearchEngine.CORPUS_TYPE corpusType) {
    super(options);

    this.invertedIndex = ArrayListMultimap.create();
    this.docTermFrequency = new HashMap<Integer, Multiset<Integer>>();
    this.skipPointers = ArrayListMultimap.create();

    this.corpusType = corpusType;
    switch (corpusType) {
      case WEB_PAGE_CORPUS:
        CORPUS_INDEX = IndexerConstant.HTML_CORPUS_INDEX;
        DOCUMENTS = IndexerConstant.HTML_DOCUMENTS;
        META = IndexerConstant.HTML_META;
        break;
      case NEWS_FEED_CORPUS:
        CORPUS_INDEX = IndexerConstant.NEWS_FEED_CORPUS_INDEX;
        DOCUMENTS = IndexerConstant.NEWS_FEED_DOCUMENTS;
        META = IndexerConstant.NEWS_FEED_META;
        break;
      default:
        throw new IllegalArgumentException("Illegal corpus type!!!");
    }

    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {

    long totalStartTimeStamp = System.currentTimeMillis();
    long startTimeStamp, duration;

    // Get the corpus folder
    File folder;
    if (corpusType == SearchEngine.CORPUS_TYPE.WEB_PAGE_CORPUS) {
      folder = new File(_options._corpusPrefix);
    } else {
      folder = new File(_options._newsPrefix);
    }

    //add filter to exclude hidden files
    FilenameFilter filenameFilter = new FilenameFilter() {
      @Override
      public boolean accept(File file, String name) {
        return !name.startsWith(".");
      }
    };
    File[] files = folder.listFiles(filenameFilter);


    /**************************************************************************
     * First delete the old index files in the target folder....
     *************************************************************************/
    File outputFolder = new File(_options._indexPrefix);

    //TODO: If index folder does not exist, null pointer exception will be thrown...

    for (File file : outputFolder.listFiles()) {
      if (file.getName().matches("^.*" + CORPUS_INDEX + "[0-9]*" + IndexerConstant.EXTENSION_IDX) ||
          file.getName().matches("^.*" + DOCUMENTS + "[0-9]*" + IndexerConstant.EXTENSION_IDX) ||
          file.getName().matches("^.*" + META + IndexerConstant.EXTENSION_IDX)) {
        file.delete();
      }
    }

    /**************************************************************************
     * Start indexing....
     *************************************************************************/
    startTimeStamp = System.currentTimeMillis();
    System.out.println("Start indexing...");

    DocumentProcessor documentProcessor = null;
    // Process file/document one by one and assign each of them a unique docid
    if (corpusType == SearchEngine.CORPUS_TYPE.WEB_PAGE_CORPUS) {
      documentProcessor = new HtmlDocumentProcessor(files, _options);
    } else if (corpusType == SearchEngine.CORPUS_TYPE.NEWS_FEED_CORPUS){
      documentProcessor = new NewsDocumentProcessor(files, _options);
    } else {
      // ...
    }

    // Start to process documents
    documentProcessor.processDocuments();

    // Get necessary data back from the document processor.
    extentListMap = documentProcessor.getExtentListMap();
    meta = ImmutableMap.copyOf(documentProcessor.getMeta());
    documents = ImmutableList.copyOf(documentProcessor.getDocuments());
    dictionary = ImmutableBiMap.copyOf(documentProcessor.getDictionary());

    _totalTermFrequency = documentProcessor.getTotalTermFrequency();
    totalTermFrequency = documentProcessor.getTotalTermFrequency();

    documentProcessor = null;

    // Get the number of documents
    _numDocs = documents.size();

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
      mergeDocumentTermFrequency();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    duration = System.currentTimeMillis() - startTimeStamp;
    System.out.println("Complete merging...");
    System.out.println("Merging time: " + Util.convertMillis(duration));

    /**************************************************************************
     * First delete the old spell index files in the target folder....
     *************************************************************************/

    File spellIndexFolder = new File(_options._indexSpell);
    for (File file : spellIndexFolder.listFiles()) {
      file.delete();
    }

    /**************************************************************************
     * Start indexing....
     *************************************************************************/
    startTimeStamp = System.currentTimeMillis();
    System.out.println("Start spell check indexing...");

    spellChecker = new NGramSpellChecker(spellIndexFolder, dictionary);
    spellChecker.buildIndex();

    duration = System.currentTimeMillis() - startTimeStamp;

    System.out.println("Complete spell check indexing...");
    System.out.println("Indexing time: " + Util.convertMillis(duration));

    /**************************************************************************
     * Populating document addition properties for web page corpus
     *************************************************************************/

    if (corpusType == SearchEngine.CORPUS_TYPE.WEB_PAGE_CORPUS) {
      // Load the page ranks.
      // Key: document ID
      // Value: Page rank score
      File pageRankFile = new File(_options._pagerankPrefix + "/pageRanks.g6");
      Map<Integer, Double> pageRanks = null;
      if (pageRankFile.exists()) {
        pageRanks = (Map<Integer, Double>) _corpusAnalyzer.load();
      }

      // Load the number views
      // Key: document ID
      // Value: Number of views
      File numViewsFile = new File(_options._numviewPrefix + "/numViews.g6");
      Map<Integer, Integer> docNumView = null;
      if (numViewsFile.exists()) {
        docNumView = (Map<Integer, Integer>) _logMiner.load();
      }

      totalNumViews = 0;
      for (Document document : documents) {
        int docid = document._docid;
        // Update page rank score
        if (pageRanks != null && pageRanks.containsKey(docid)) {
          document.setPageRank(pageRanks.get(docid));
        } else {
          document.setPageRank(0.0);
        }
        // Update number of views
        if (docNumView != null && docNumView.containsKey(docid)) {
          totalNumViews += docNumView.get(docid);
          document.setNumViews(docNumView.get(docid));
        } else {
          document.setNumViews(0);
        }
      }
    }

    /**************************************************************************
     * Serializing the rest...
     *************************************************************************/
    startTimeStamp = System.currentTimeMillis();
    System.out.println("Start serializing...");

    // Serialize the whole object :)
    String indexFile = _options._indexPrefix + "/" + META + IndexerConstant.EXTENSION_IDX;
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

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
    IndexerInvertedCompressed loaded;
    File folder = new File(_options._indexPrefix);
    File[] files = folder.listFiles();

    /**************************************************************************
     * Load the class file first
     *************************************************************************/
    for (File file : files) {
      if (file.getName().equals(META + IndexerConstant.EXTENSION_IDX)) {
        String indexFile = _options._indexPrefix + "/" + META + IndexerConstant.EXTENSION_IDX;
        System.out.println("Load index from: " + indexFile);

        ObjectInputStream reader = new ObjectInputStream(new BufferedInputStream(new FileInputStream(indexFile)));
        loaded = (IndexerInvertedCompressed) reader.readObject();

        this.documents = loaded.documents;

        // Compute numDocs and totalTermFrequency b/c Indexer is not
        // serializable.
        this._numDocs = documents.size();
        this._totalTermFrequency = loaded.totalTermFrequency;
        this.totalNumViews = loaded.totalNumViews;

        this.skipPointers = loaded.skipPointers;
        this.extentListMap = loaded.extentListMap;

        this.meta = loaded.meta;
        this.docTermFreqMeta = loaded.docTermFreqMeta;

        this.dictionary = loaded.dictionary;

        this.spellChecker = loaded.spellChecker;
        reader.close();
      }
    }

    System.out.println(Integer.toString(_numDocs) + " documents loaded "
        + "with " + Long.toString(_totalTermFrequency) + " terms!");
  }

  @Override
  public Document getDoc(int docid) {
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
   * @return document
   */
  @Deprecated
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

  public SpellCheckResult getSpellCheckResults(Query query){
    return spellChecker.getSpellCheckResults(query);
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
   * dictionary, return an empty String instead.
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
   * the dictionary, return -1.
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
   *
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
    String invertedIndexFileName = _options._indexPrefix + "/" + CORPUS_INDEX + IndexerConstant.EXTENSION_IDX;
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
      if (f.getName().matches("^" + CORPUS_INDEX + "[0-9]+" + IndexerConstant.EXTENSION_IDX)) {
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
        if (file.getName().matches("^" + CORPUS_INDEX + String.format("%03d", i) + "\\" + IndexerConstant.EXTENSION_IDX)) {
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

      List<Integer> postingList = VByteUtil.vByteDecodingList(outputPostingList);

      /**************************************************************************
       * Generate skip pointers and convert docid to deltas
       *************************************************************************/
      outputPostingList = convertDeltaDocid(outputTermID, postingList);

      currentPos = raf.length();
      raf.seek(currentPos);
      raf.write(Bytes.toArray(outputPostingList));

      // Assume the posting list will not be too big...
      length = (int) (raf.length() - currentPos);
      meta.get(outputTermID).setPostingListMetaData(new Offsets(currentPos, length));
    }

    raf.close();

    /**************************************************************************
     * Wrapping up... Delete all partial index file
     *************************************************************************/
    for (File f : folder.listFiles()) {
      if (f.getName().matches("^" + CORPUS_INDEX + "[0-9]+\\" + IndexerConstant.EXTENSION_IDX)) {
        f.delete();
      }
    }
  }

  private List<Byte> convertDeltaDocid(int termId, List<Integer> postingList) {
    List<Integer> skipPointer = new ArrayList<Integer>();

    int i = 0;
    int docid = 0;
    int prevDocid = 0;

    int byteLength = 0;
    int prevByteLength = 0;

    while (i < postingList.size()) {
      docid = postingList.get(i);
      int deltaDocid = docid - prevDocid;

      postingList.set(i++, deltaDocid);

      byteLength += VByteUtil.getByteLength(deltaDocid);

      int occurrence = postingList.get(i++);
      byteLength += VByteUtil.getByteLength(occurrence);

      int endIndex = i + occurrence;
      for (; i < endIndex; i++) {
        byteLength += VByteUtil.getByteLength(postingList.get(i));
      }

      if (byteLength - prevByteLength > K) {
        skipPointer.add(prevDocid);
        skipPointer.add(byteLength);
        prevByteLength = byteLength;
      }

      prevDocid = docid;
    }

    if (skipPointer.size() > 0) {
      skipPointers.putAll(termId, VByteUtil.vByteEncodingList(skipPointer));
    }

    return VByteUtil.vByteEncodingList(postingList);
  }

  /**
   * Merge all partial doc term frequency file...
   */
  public void mergeDocumentTermFrequency() throws IOException {
    String invertedIndexFileName = _options._indexPrefix + "/" + DOCUMENTS + IndexerConstant.EXTENSION_IDX;
    Map<Integer, Offsets> tmpDocTermFreqMeta = new HashMap<Integer, Offsets>();

    RandomAccessFile raf = new RandomAccessFile(invertedIndexFileName, "rw");
    long currentPos = 0;
    int length = 0;

    /**************************************************************************
     * Prepare merging...
     *************************************************************************/
    File folder = new File(_options._indexPrefix);
    int numOfPartialIndex = 0;

    // Get the number of partial documents file
    for (File f : folder.listFiles()) {
      if (f.getName().matches("^" + DOCUMENTS + "[0-9]+" + IndexerConstant.EXTENSION_IDX)) {
        numOfPartialIndex++;
      }
    }

    Kryo kryo = new Kryo();
    File[] files = new File[numOfPartialIndex];
    Input[] inputs = new Input[numOfPartialIndex];

    // Initialize the files, inputs and
    // Then get the quantity of the posting list for each partial file
    for (int i = 0; i < numOfPartialIndex; i++) {
      for (File file : folder.listFiles()) {
        if (file.getName().matches(
            "^" + DOCUMENTS + String.format("%03d", i) + "\\" + IndexerConstant.EXTENSION_IDX)) {
          files[i] = file;
          inputs[i] = new Input(new FileInputStream(file.getAbsolutePath()));
          break;
        }
      }
    }

    /**************************************************************************
     * Start merging...
     *************************************************************************/
    int docCount = 0;
    for (int i = 0; i < files.length; i++) {
      int numOfEntries = kryo.readObject(inputs[i], Integer.class);

      for (int j = 0; j < numOfEntries; j++) {
        int docid = kryo.readObject(inputs[i], Integer.class);

        List<Byte> termIdAndFrequency = kryo.readObject(inputs[i], ArrayList.class);
        List<Integer> test = VByteUtil.vByteDecodingList(termIdAndFrequency);

        currentPos = raf.length();
        raf.seek(currentPos);
        raf.write(Bytes.toArray(termIdAndFrequency));

        // Assume the posting list will not be too big...
        length = (int) (raf.length() - currentPos);
        tmpDocTermFreqMeta.put(docid, new Offsets(currentPos, length));

        docCount++;
      }

      inputs[i].close();
    }
    System.out.println("Merging docs...: " + docCount);

    docTermFreqMeta = ImmutableMap.copyOf(tmpDocTermFreqMeta);

    /**************************************************************************
     * Wrapping up...
     *************************************************************************/
    for (File f : folder.listFiles()) {
      if (f.getName().matches("^" + DOCUMENTS + "[0-9]+\\" + IndexerConstant.EXTENSION_IDX)) {
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
    String invertedIndexFileName;
    invertedIndexFileName = _options._indexPrefix + "/" + CORPUS_INDEX + IndexerConstant.EXTENSION_IDX;

    boolean hasAlready = true;
    Offsets offsets;
    int count = 0;

    // Clean if not enough memory...
    if (invertedIndex.keys().size() > IndexerConstant.MAX_INVERTED_INDEX_SIZE) {
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
      if (meta.containsKey(termId) && !invertedIndex.containsKey(termId)) {
        offsets = meta.get(termId).getPostingListMetaData();
        raf.seek(offsets.getStartPos());
        byte[] postingListBytes = new byte[offsets.getLength()];
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
      String documentTermFrequencyFileName;
      documentTermFrequencyFileName = _options._indexPrefix + "/" + DOCUMENTS + IndexerConstant.EXTENSION_IDX;

      RandomAccessFile raf = null;
      try {
        raf = new RandomAccessFile(documentTermFrequencyFileName, "r");
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }

      // Clean if not enough memory...
      // TODO: Fix later
      if (docTermFrequency.size() > 1000) {
        docTermFrequency.clear();
      }

      Offsets offsets = docTermFreqMeta.get(docid);
      try {
        raf.seek(offsets.getStartPos());
        byte[] docTermFrequencyByte = new byte[offsets.getLength()];
        raf.readFully(docTermFrequencyByte);
        List<Integer> termIdAndFrequency = VByteUtil.vByteDecodingList(docTermFrequencyByte);
        Multiset<Integer> docTermFrequency = HashMultiset.create();

        for (int i = 0; i < termIdAndFrequency.size() / 2; i++) {
          docTermFrequency.setCount(termIdAndFrequency.get(2 * i), termIdAndFrequency.get(2 * i + 1));
        }

        this.docTermFrequency.put(docid, docTermFrequency);
        raf.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * If all query terms exist in the document title, return true.
   *
   * @param query query
   * @param docid document ID
   * @return boolean...
   */
  public boolean isQueryInTitle(Query query, int docid) {
    List<String> queryTerms = new ArrayList<String>(query._tokens);
    ExtentList extentList = extentListMap.get(docid);
    FieldPositionRange fieldPositionRange = extentList.getFieldPositionRange(ExtentList.DocumentField.TITLE);

    for (String term : queryTerms) {
      int firstPos = nextPos(term, docid, -1);
      if (fieldPositionRange.getEndPos() <= firstPos) {
        return false;
      }
    }

    return true;
  }
}