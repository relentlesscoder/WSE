package edu.nyu.cs.cs2580;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.google.common.collect.*;
import edu.nyu.cs.cs2580.SearchEngine.Options;
import org.jsoup.Jsoup;

import java.io.*;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class IndexerInvertedOccurrence extends Indexer implements Serializable {

  private static final long serialVersionUID = 1L;

  // Inverted index.
  // Key is the term and value is the compressed posting list.
  private ListMultimap<String, Integer> invertedIndex = ArrayListMultimap
      .create();

  // Term frequency across whole corpus.
  // key is the term and value is the frequency of the term across the whole
  // corpus.
  private Multiset<String> _termCorpusFrequency = HashMultiset.create();

  private List<DocumentIndexed> documents = new ArrayList<DocumentIndexed>();
  private Map<String, Integer> docUrlMap = new HashMap<String, Integer>();

  private List<String> partialMergerFileOffset = new ArrayList<String>();

  // Provided for serialization
  public IndexerInvertedOccurrence() {
  }

  public IndexerInvertedOccurrence(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {
    long totalStartTimeStamp = System.currentTimeMillis();
    ProgressBar progressBar = new ProgressBar();
    File folder = new File(_options._corpusPrefix);
    File[] files = folder.listFiles();
    int fileCount = 0;
    long startTimeStamp, duration;

    checkNotNull(files, "No files found in: %s", folder.getPath());

    // Empty the target folder first
    File outputFolder = new File(_options._indexPrefix);
    for (File file : outputFolder.listFiles()) {
      file.delete();
    }

    /**************************************************************************
     * Indexing....
     *************************************************************************/
    startTimeStamp = System.currentTimeMillis();
    System.out.println("Start indexing...");

    // Process file/document one by one and assign each of them a unique docid
    for (int docid = 0; docid < files.length; docid++) {
      checkNotNull(files[docid], "File can not be null!");
      // Update the progress bar first :)
      progressBar.update(docid, files.length);
      processDocument(files[docid], docid);

      if (Util.hasReachThreshold(invertedIndex)) {
        // Memory not enough, write to file first...
        Util.writePartialInvertedIndex(invertedIndex, _options, ++fileCount);
        invertedIndex.clear();
      }
    }

    //Write the rest partial inverted index...
    Util.writePartialInvertedIndex(invertedIndex, _options, ++fileCount);
    invertedIndex.clear();

    // Get the number of documents
    numDocs = documents.size();

    duration = System.currentTimeMillis() - startTimeStamp;

    System.out.println("Complete indexing");
    System.out.println("Indexing time: " + Util.convertMillis(duration));
    System.out.println("Indexed " + Integer.toString(numDocs) + " docs with "
        + Long.toString(_totalTermFrequency) + " terms.");

    /**************************************************************************
     * Merging....
     *************************************************************************/
    startTimeStamp = System.currentTimeMillis();
    System.out.println("merging");

    try {
      merge();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    duration = System.currentTimeMillis() - startTimeStamp;
    System.out.println("Complete merging");
    System.out.println("Merging time: " + Util.convertMillis(duration));

    /**************************************************************************
     * Serializing the rest...
     *************************************************************************/
    startTimeStamp = System.currentTimeMillis();
    System.out.println("Serializing...");

//    Serialize the whole object :)
    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Storing index to: " + _options._indexPrefix);
    ObjectOutputStream writer = new ObjectOutputStream(new BufferedOutputStream(new
        FileOutputStream(indexFile)));
    writer.writeObject(this);
    writer.close();

    duration = System.currentTimeMillis() - startTimeStamp;
    System.out.println("Compete serializing");
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
    doc.setUrl(file.getAbsolutePath());
    documents.add(doc);
    docUrlMap.put(doc.getUrl(), docid);

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

      if (term != null) {
        // Update the termCorpusFrequency
        _termCorpusFrequency.add(term);

        if (invertedIndex.containsKey(term)) {
          // The token exists in the index
          invertedIndex.get(term).add(docid);
          invertedIndex.get(term).add(position);
        } else {
          // The token does not exist in the index, add it first, then add the
          // docid and the token's position
          invertedIndex.get(term).add(docid);
          invertedIndex.get(term).add(position);
        }

        _totalTermFrequency++;
        position++;
      }
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

        ObjectInputStream reader = new ObjectInputStream(new FileInputStream(
            indexFile));
        loaded = (IndexerInvertedOccurrence) reader.readObject();

        this.documents = loaded.documents;

        // Compute numDocs and totalTermFrequency b/c Indexer is not
        // serializable.
        this.numDocs = documents.size();
        this._totalTermFrequency = loaded._termCorpusFrequency.size();

        this.invertedIndex = loaded.invertedIndex;
        this._termCorpusFrequency = loaded._termCorpusFrequency;
        this.docUrlMap = loaded.docUrlMap;
        this.partialMergerFileOffset = loaded.partialMergerFileOffset;
        reader.close();
        break;
      }
    }

    System.out.println(Integer.toString(numDocs) + " documents loaded "
        + "with " + Long.toString(_totalTermFrequency) + " terms!");
  }

  @Override
  public Document getDoc(int docid) {
    return documents.get(docid);
  }

  @Override
  public Document nextDoc(Query query, int docid) {
    checkNotNull(docid, "docid can not be null!");
    Vector<String> queryTerms = query._tokens;

    //TODO
    try {
      dynamicLoading(queryTerms);
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
  private int nextCandidateDocid(Vector<String> queryTerms, int docid) {
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
    List<Integer> docidList = invertedIndex.get(term);
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
    List<Integer> docidList = invertedIndex.get(term);
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
    List<Integer> postingList = invertedIndex.get(term);
    int size = postingList.size();
    // Get the start offset for the term of {@code docid}
    int docidOffset = firstDocidOffset(term, docid);

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
   * @param term  The term...
   * @param docid The document ID
   * @return
   */
  private int firstDocidOffset(String term, int docid) {
    List<Integer> postingList = invertedIndex.get(term);
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
    if (invertedIndex.containsKey(term)) {
      return invertedIndex.get(term).size();
    } else {
      return 0;
    }
  }

  @Override
  public int corpusTermFrequency(String term) {
    return _termCorpusFrequency.count(term);
  }

  @Override
  public int documentTermFrequency(String term, String url) {
    if (!docUrlMap.containsKey(url)) {
      // TODO: TEMP
      return 0;
    }

    int docTermFrequency = 0;
    int docid = docUrlMap.get(url);
    int offset = firstDocidOffset(term, docid);

    if (offset == -1) {
      return 0;
    }

    List<Integer> postingList = invertedIndex.get(term);

    while (offset < postingList.size() && postingList.get(offset) == docid) {
      docTermFrequency++;
      offset += 2;
    }

    return docTermFrequency;
  }

  /**
   * Merge all partial list together :)
   *
   * @throws IOException
   * @throws ClassNotFoundException
   */
  private void merge() throws IOException, ClassNotFoundException {
    Multimap<String, Integer> bufferMap = ArrayListMultimap.create();
    String firstTermOfPartialFile = "";
    boolean hasFirstTerm = false;
    long currentSize = 0;
    int partialFileCount = 0;

    File folder = new File(_options._indexPrefix);

    int numOfIndex = 0;
    for (File f : folder.listFiles()) {
      if (f.getName().matches("^corpus[0-9]+\\.idx")) {
        numOfIndex++;
      }
    }

    File[] files = new File[numOfIndex];
    int[] numOfEntries = new int[numOfIndex];
    String[] terms = new String[numOfIndex];

    Kryo kryo = new Kryo();
    Input[] inputs = new Input[numOfIndex];

    for (int i = 0; i < numOfIndex; i++) {
      for (File file : folder.listFiles()) {
        if (file.getName().matches(
            "^corpus" + String.format("%03d", i + 1) + "\\.idx")) {
          files[i] = file;
          inputs[i] = new Input(new FileInputStream(file.getAbsolutePath()));
          break;
        }
      }
    }

    // Initialize...
    for (int i = 0; i < numOfIndex; i++) {
      numOfEntries[i] = kryo.readObject(inputs[i], Integer.class);
      terms[i] = "";
    }

    while (hasEntries(numOfEntries)) {
      for (int i = 0; i < numOfIndex; i++) {
        if (terms[i].equals("") && numOfEntries[i] > 0) {
          numOfEntries[i] -= 1;
          terms[i] = kryo.readObject(inputs[i], String.class);
        }
      }

      SortedSetMultimap<String, Integer> sortedSetMultimap = TreeMultimap
          .create(Ordering.natural(), Ordering.natural());
      for (int i = 0; i < numOfIndex; i++) {
        if (!terms[i].equals("")) {
          sortedSetMultimap.put(terms[i], i);
        }
      }

      Multimap<String, Integer> output = ArrayListMultimap.create();
      for (Map.Entry entry : sortedSetMultimap.entries()) {
        String term = (String) entry.getKey();
        for (int i : sortedSetMultimap.asMap().get(term)) {
          output.get(term).addAll(kryo.readObject(inputs[i], ArrayList.class));
          terms[i] = "";
        }

        break;
      }

      for (Map.Entry entry : output.asMap().entrySet()) {
        String term = (String) entry.getKey();
        List<Integer> list = new ArrayList<Integer>(
            (java.util.Collection<? extends Integer>) entry.getValue());

        currentSize += list.size();
        bufferMap.get(term).addAll(list);

        if (!hasFirstTerm) {
          firstTermOfPartialFile = term;
          hasFirstTerm = true;
        }

        if (currentSize > Util.SIZE_PER_MAP_INTEGER) {
          partialFileCount++;
          currentSize = 0;
          hasFirstTerm = false;

          String fileName = "/corpus_merged_partial_" + String.format("%03d", partialFileCount) + ".idx";
          partialMergerFileOffset.add(firstTermOfPartialFile);
          partialMergerFileOffset.add(fileName);
          String indexPartialMergedFile = _options._indexPrefix + fileName;
          ObjectOutputStream writer = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
              indexPartialMergedFile)));

          writer.writeObject(bufferMap);
          writer.flush();
          writer.reset();
          writer.close();
          bufferMap.clear();
        }
      }
      output.clear();
    }

    partialFileCount++;
    String fileName = "/corpus_merged_partial_" + String.format("%03d", partialFileCount) + ".idx";
    partialMergerFileOffset.add(firstTermOfPartialFile);
    partialMergerFileOffset.add(fileName);
    String indexPartialMergedFile = _options._indexPrefix + fileName;
    ObjectOutputStream writer = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
        indexPartialMergedFile)));

    writer.writeObject(bufferMap);
    writer.flush();
    writer.reset();
    writer.close();
    bufferMap.clear();

//    clean up
    for (File f : folder.listFiles()) {
      if (f.getName().matches("^corpus[0-9]+\\.idx")) {
        f.delete();
      }
    }
  }

  private boolean hasEntries(int[] numOfEntries) {
    for (int i : numOfEntries) {
      if (i > 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Dynamically load partial invertial index at run time
   *
   * @param query the query terms
   * @throws IOException
   * @throws ClassNotFoundException
   */
  private void dynamicLoading(List<String> query) throws IOException, ClassNotFoundException {
    // First check if dynamic loading is need.
    boolean needLoading = false;

    for (String term : query) {
      if (!invertedIndex.containsKey(term)) {
        needLoading = true;
      }
    }

    if (!needLoading) {
      // We got all we need, return~
      return;
    }

    System.out.println("Start dynamic loading...");
    long startTimeStamp = System.currentTimeMillis();
    int count = 0;

    File folder = new File(_options._indexPrefix);
    File[] files = folder.listFiles();

    // Clean if not enough memory...
    if (invertedIndex.keys().size() > Util.MAX_INVERTED_INDEX_SIZE) {
      invertedIndex.clear();
    }

    for (String term : query) {
      if (invertedIndex.containsKey(term)) {
        continue;
      }

      int index = 0;
      for (int i = 1; i < partialMergerFileOffset.size() / 2; i++) {
        if (term.compareTo(partialMergerFileOffset.get(i * 2)) >= 0) {
          index = 2 * i;
        } else {
          break;
        }
      }

      String indexFile = _options._indexPrefix + "/" + partialMergerFileOffset.get(index + 1);
      ObjectInputStream reader = new ObjectInputStream(new FileInputStream(indexFile));
      Multimap<String, Integer> tmpPartialIndex = (Multimap<String, Integer>) reader.readObject();
      for (String s : query) {
        if (!invertedIndex.containsKey(term) && tmpPartialIndex.containsKey(s)) {
          // Load!
          invertedIndex.get(s).addAll(tmpPartialIndex.get(s));
          count++;
        }
      }
      tmpPartialIndex.clear();
    }

    long duration = System.currentTimeMillis() - startTimeStamp;
    System.out.println("Compete dynamic loading. Loads " + count + " posting lists and takes time " + Util.convertMillis(duration));
  }
}