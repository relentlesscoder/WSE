package edu.nyu.cs.cs2580;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.google.common.collect.*;
import edu.nyu.cs.cs2580.SearchEngine.Options;
import org.jsoup.Jsoup;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class IndexerInvertedOccurrence extends Indexer implements Serializable {
  private static final long serialVersionUID = 1L;

  // Dictionary
  // Key: Term
  // Value: Term ID
  private BiMap<String, Integer> dictionary = HashBiMap.create();

  // Inverted index.
  // Key: Term ID
  // Value: Compressed posting list.
  private ListMultimap<Integer, Integer> invertedIndex = ArrayListMultimap.create();

  // Term frequency of each document
  // Key: Docid
  // Value: Key: Term ID
  // Value: Value: Term frequency
  private Map<Integer, Multiset<Integer>> docTermFrequency = new HashMap<Integer, Multiset<Integer>>();

  // Key: Term ID
  // Value: MetaData
  // MetaData {
  //   corpusTermFrequency: Term frequency across whole corpus.
  //   corpusDocFrequencyByTerm: Number of documents a term appeared, over the full corpus.
  //   postingListMetaData
  private Map<Integer, MetaData> meta = new HashMap<Integer, MetaData>();

  // Key: Document ID
  // Value: Term document frequency meta info
  private Map<Integer, MetaPair> docMetaData = new HashMap<Integer, MetaPair>();

  private List<DocumentIndexed> documents = new ArrayList<DocumentIndexed>();
//  private Map<String, Integer> docUrlMap = new HashMap<String, Integer>();

  long totalTermFrequency = 0;

//	// Term frequency across whole corpus.
//	// key is the term and value is the frequency of the term across the whole
//	// corpus.
//	private Multiset<String> _termCorpusFrequency = HashMultiset.create();

//	private Map<String, MetaPair> metaData = new HashMap<String, MetaPair>();

  // Provided for serialization
  public IndexerInvertedOccurrence() {
  }

  public IndexerInvertedOccurrence(Options options) {
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
    long startTimeStamp, duration;
    startTimeStamp = System.currentTimeMillis();

    Util.writePartialInvertedIndex(invertedIndex, _options, fileCount);
    Util.writePartialDocuments(docTermFrequency, _options, fileCount);
    invertedIndex.clear();
    docTermFrequency.clear();

    duration = System.currentTimeMillis() - startTimeStamp;
    System.out.println(Util.convertMillis(duration));
  }

  @Override
  public void constructIndex() throws IOException {
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
      // Update the progress bar first :)
      progressBar.update(docid, files.length);
      // Now process the document
      processDocument(files[docid], docid);
      // Write to a file if memory usage has reach the memory threshold
      if (Util.hasReachThreshold(invertedIndex)) {
        writePartialFile(partialFileCount);
        partialFileCount++;
      }
    }

    // Write the rest partial inverted index...
    writePartialFile(partialFileCount);

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
      Util.mergeDocumentTermFrequency(docMetaData, _options);
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
  private void processDocument(File file, int docid) throws IOException {
    org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(file, "UTF-8");

    String bodyText = jsoupDoc.body().text();
    String title = jsoupDoc.title();

    // Create the document and store it.
    DocumentIndexed doc = new DocumentIndexed(docid, this);
    doc.setTitle(title);
    doc.setUrl(file.getName());

    documents.add(doc);
//    docUrlMap.put(doc.getUrl(), docid);

    // Populate the inverted index
    populateInvertedIndex(title + " " + bodyText, docid);

    // TODO: Deal with all the links...
    // Elements links = jsoupDoc.select("a[href]");
  }

  /**
   * Populate the inverted index.
   *
   * @param content The text content of the html document.
   * @param docid   The document ID.
   */
  private void populateInvertedIndex(String content, int docid) {
    Tokenizer tokenizer = new Tokenizer(new StringReader(content));
    int position = 0;

    /**************************************************************************
     * Start to process the document one term at a time.
     *************************************************************************/
    while (tokenizer.hasNext()) {
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

      if (invertedIndex.containsKey(term)) {
        // The token exists in the index
        invertedIndex.get(termId).add(docid);
        invertedIndex.get(termId).add(position);
      } else {
        // The token does not exist in the index, add it first, then add the
        // docid and the token's position
        invertedIndex.get(termId).add(docid);
        invertedIndex.get(termId).add(position);
      }

      if (invertedIndex.get(termId).get(invertedIndex.get(termId).size() - 2) != docid) {
        // Update the term frequency across the whole corpus.
        int corpusDocFrequencyByTerm = meta.get(termId).getCorpusDocFrequencyByTerm();
        meta.get(termId).setCorpusDocFrequencyByTerm(corpusDocFrequencyByTerm + 1);
      }

      position++;
    }

    documents.get(docid).setTotalDocTerms(++position);
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
    IndexerInvertedOccurrence loaded;
    File folder = new File(_options._indexPrefix);
    File[] files = folder.listFiles();

    // Load the class file first
    for (File file : files) {
      if (file.getName().equals("corpus.idx")) {
        String indexFile = _options._indexPrefix + "/corpus.idx";
        System.out.println("Load index from: " + indexFile);

        ObjectInputStream reader = new ObjectInputStream(new BufferedInputStream(new FileInputStream(indexFile)));
        loaded = (IndexerInvertedOccurrence) reader.readObject();

        this.documents = loaded.documents;

        // Compute numDocs and totalTermFrequency b/c Indexer is not
        // serializable.
        this._numDocs = documents.size();
        this._totalTermFrequency = loaded.totalTermFrequency;

        this.invertedIndex = loaded.invertedIndex;
        this.docTermFrequency = loaded.docTermFrequency;

//        this.docUrlMap = loaded.docUrlMap;
        this.meta = loaded.meta;
        this.docMetaData = loaded.docMetaData;
        this.dictionary = loaded.dictionary;
        reader.close();
        break;
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
    checkNotNull(docid, "docid can not be null!");
    List<String> queryTerms = new ArrayList<String>(query._tokens);

    // TODO
    try {
      loadPostingList(queryTerms);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    int nextDocid = nextCandidateDocid(queryTerms, docid);

    if (nextDocid != -1) {
      return documents.get(nextDocid);
    } else {
      return null;
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
        // The next document ID does not exist... so no next document will be
        // available.
        return -1;
      }
      largestDocid = Math.max(largestDocid, nextDocid);
    }

    // Check if the largest document ID satisfy all query terms.
    for (String term : queryTerms) {
      if (!hasDocid(term, largestDocid)) {
        // This document ID does not satisfy one of the query term...
        // Check the next...
        return nextCandidateDocid(queryTerms, largestDocid);
      }
    }

    // If the satisfied document ID has been found, return it.
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
    List<Integer> docidList = invertedIndex.get(termId);
    int size = docidList.size();

    // Base case
    if (size == 0 || docidList.get(size - 2) <= docid) {
      return -1;
    }

    if (docidList.get(0) > docid) {
      return docidList.get(0);
    }

    // Use binary search for the next document ID right after {@code docid}
    int low = 0;
    int high = docidList.size() / 2 - 1;

    while (high - low > 1) {
      int mid = low + (high - low) / 2;
      int midDocid = docidList.get(mid * 2);
      if (midDocid <= docid) {
        low = mid;
      } else {
        high = mid;
      }
    }

    return docidList.get(high * 2);
  }

  /**
   * Check if the docid exists in the term's posting list.
   *
   * @param term  The term...
   * @param docid The document ID
   * @return true if the docid exists in the term's posting list, otherwise
   * false
   */
  private boolean hasDocid(String term, int docid) {
    int termId = dictionary.get(term);
    List<Integer> docidList = invertedIndex.get(termId);
    int size = docidList.size();

    if (size == 0 || docidList.get(size - 2) < docid) {
      return false;
    }

    // Use binary search to find if the {@code docid} exists in the list
    int low = 0;
    int high = (docidList.size() - 1) / 2;

    while (low <= high) {
      int mid = low + (high - low) / 2;
      if (docidList.get(mid * 2) == docid) {
        return true;
      } else if (docidList.get(mid * 2) < docid) {
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }

    return false;
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
    List<Integer> postingList = invertedIndex.get(termId);

    // Get the start offset for the term of {@code docid}
    int docidOffset = firstDocidOffset(termId, docid);

    // Find the position right after current one
    while (docidOffset < postingList.size()
        && postingList.get(docidOffset) == docid
        && postingList.get(docidOffset + 1) <= pos) {
      docidOffset += 2;
    }

    if (docidOffset < postingList.size()
        && postingList.get(docidOffset) == docid
        && postingList.get(docidOffset + 1) > pos) {
      // Found the next position
      return postingList.get(docidOffset + 1);
    } else {
      // No more...
      return -1;
    }
  }

  /**
   * Find the first docid offset
   *
   * @param termId The term ID...
   * @param docid  The document ID
   * @return first doc offset
   */
  private int firstDocidOffset(int termId, int docid) {
    List<Integer> postingList = invertedIndex.get(termId);
    int size = postingList.size();
    int res = -1;

    // Use binary search for the next document ID right after {@code docid}
    int low = 0;
    int high = (size) / 2 - 1;

    while (low <= high) {
      int mid = low + (high - low) / 2;
      int midDocid = postingList.get(mid * 2);
      if (midDocid == docid) {
        res = mid * 2;
        high = mid - 1;
      } else if (docid < midDocid) {
        high = mid - 1;
      } else {
        low = mid + 1;
      }
    }

    return res;
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
    int termId = dictionary.get(term);
    int docTermFrequency = 0;
    int offset = firstDocidOffset(termId, docid);

    if (offset == -1) {
      return 0;
    }

    List<Integer> postingList = invertedIndex.get(termId);

    while (offset < postingList.size() && postingList.get(offset) == docid) {
      docTermFrequency++;
      offset += 2;
    }

    return docTermFrequency;
  }

  /**
   * Get the docTermFrequency of a specific document
   * @param docid document id
   * @return the term frequency
   */
  public Multiset<Integer> getDocidTermFrequency(int docid) {
    if (!docMetaData.containsKey(docid)) {
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
      List<Integer> outputPostingList = new ArrayList<Integer>();

      // First read the next term from each of the partial index file if it has
      // more.
      for (int i = 0; i < numOfPartialIndex; i++) {
        if (termIds[i] == -1 && numOfPostingList[i] > 0) {
          numOfPostingList[i]--;
          termIds[i] = kryo.readObject(inputs[i], Integer.class);
        }
      }

      // Sort all next terms by alphabetical order
      // For two same terms, sort their file number to
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
      System.out.println("Start serializing... " + outputPostingList.size());
      raf.write(Util.serialize(outputPostingList));

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
      // Also check if it exists in the metaData, load it only if it exists.
      int termId = dictionary.get(term);
      if (meta.containsKey(termId)) {
        metaPair = meta.get(termId).getPostingListMetaData();
        raf.seek(metaPair.getStartPos());
        byte[] postingListBytes = new byte[metaPair.getLength()];
        raf.readFully(postingListBytes);
        List<Integer> postingList = (List<Integer>) Util
            .deserialize(postingListBytes);
        invertedIndex.get(termId).addAll(postingList);
        count++;
      }
    }

    raf.close();
    long duration = System.currentTimeMillis() - startTimeStamp;
    System.out.println("Compete dynamic loading. Loads " + count
        + " posting lists and takes time " + Util.convertMillis(duration));
  }


  private void loadTermDocFrequency(int docid) {
    if (!docTermFrequency.containsKey(docid) && docMetaData.containsKey(docid)) {
      String documentTermFrequencyFileName = _options._indexPrefix + "/documents.idx";
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

      MetaPair metaPair = docMetaData.get(docid);
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