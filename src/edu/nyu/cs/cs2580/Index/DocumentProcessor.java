package edu.nyu.cs.cs2580.Index;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.*;
import edu.nyu.cs.cs2580.document.Document;
import edu.nyu.cs.cs2580.rankers.IndexerConstant;
import edu.nyu.cs.cs2580.SearchEngine;
import edu.nyu.cs.cs2580.tokenizer.Tokenizer;
import edu.nyu.cs.cs2580.utils.ProgressBar;
import edu.nyu.cs.cs2580.utils.VByteUtil;

import java.io.*;
import java.util.*;

/**
 * This is the abstract class of document processor.
 */
public abstract class DocumentProcessor implements Serializable {
  private static final long serialVersionUID = 1L;

  protected class DocumentFields {
    private String title;
    private String content;

    public DocumentFields(String title) {
      this.title = title;
    }

    public String getTitle() {
      return title;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }
  }

  // Progress bar
  protected ProgressBar progressBar;

  // Search options containing configuration details
  protected SearchEngine.Options options;

  // Files in the corpus folder
  protected File[] files;

  // Compressed inverted index, dynamically loaded per term at run time
  // Key: Term ID
  // Value: Compressed posting list.
  protected ListMultimap<Integer, Byte> invertedIndex;

  // Term frequency of each document, dynamically loaded per doc at run time
  // Key: Docid
  // Value: Key: Term ID
  // Value: Value: Term frequency
  protected Map<Integer, Multiset<Integer>> docTermFrequency;

  // Dictionary
  // Key: Term
  // Value: Term ID
  protected BiMap<String, Integer> dictionary;

  // Key: Term ID
  // Value: MetaData
  // MetaData {
  //   corpusTermFrequency: Term frequency across whole corpus.
  //   corpusDocFrequencyByTerm: Number of documents a term appeared, over the full corpus.
  //   postingListMetaData
  // }
  protected Map<Integer, MetaData> meta;

  // List of all documents containing some document properties for ranking
  protected List<Document> documents;

  // File number of the next file for splitting
  protected int splitFileNumber;

  // Total term frequency across the whole corpus
  protected long totalTermFrequency;

  // Extent list for different document fields which contains their position range
  protected Map<Integer, ExtentList> extentListMap;

  // Constructor
  protected DocumentProcessor(File[] files, SearchEngine.Options options) {
    this.progressBar = new ProgressBar();

    this.options = options;

    this.files = files;

    this.invertedIndex = ArrayListMultimap.create();

    this.docTermFrequency = new HashMap<Integer, Multiset<Integer>>();

    this.dictionary = HashBiMap.create();

    this.meta = new HashMap<Integer, MetaData>();

    this.documents = new ArrayList<Document>();

    this.splitFileNumber = 0;

    this.totalTermFrequency = 0;

    this.extentListMap = new HashMap<Integer, ExtentList>();
  }

  /**
   * Process the corpus documents and populate the inverted index. If the index size has reach a threshold, it will split
   * it and write into a file for later merge in the indexer.
   *
   * It also generate some meta data for the indexer.
   *
   * For each of the document, call {@link populateInvertedIndex} in order to populate the inverted index.
   */
  public abstract void processDocuments() throws IOException;

  /**
   * Populate the inverted index provided by a document's content and its ID.
   *
   * @param docFields The content of each of the document fields.
   * @param docid     The document ID.
   * @return End position, which is 1 pass the last term.
   */
  protected int populateInvertedIndex(DocumentFields docFields, int docid) {
    ExtentList extentList = new ExtentList();
    int position = 0;

    // Uncompressed temporary inverted index.
    // Key is the term and value is the uncompressed posting list.
    ListMultimap<Integer, Integer> tmpInvertedIndex = ArrayListMultimap.create();

    Multiset<Integer> termFrequencyOfDocid = HashMultiset.create();
    docTermFrequency.put(docid, termFrequencyOfDocid);

    // Populate the temporary index with the title first
    position = populateTmpInvertedIndex(tmpInvertedIndex, docFields.getTitle(), docid, position);
    // Update the extent list.
    extentList.addExtList(ExtentList.DocumentField.TITLE, 0, position);
    int titleEndPos = position;

    // Populate the temporary index with the main content first
    position = populateTmpInvertedIndex(tmpInvertedIndex, docFields.getContent(), docid, position);
    // Update the extent list.
    extentList.addExtList(ExtentList.DocumentField.CONTENT, titleEndPos, position);

    // Update the extent list map.
    extentListMap.put(docid, extentList);

    // Update the total document term of that document
    documents.get(docid).setTotalDocTerms(position);

    // Convert all positions of the posting list to deltas
    convertPositionToDelta(tmpInvertedIndex);

    // Compress the temporary inverted index and populate the inverted index.
    for (int termId : tmpInvertedIndex.keySet()) {
      // Encode the posting list
      List<Byte> partialPostingList = VByteUtil.vByteEncodingList(tmpInvertedIndex
          .get(termId));

      // Append the posting list if one exists, otherwise create one first :)
      invertedIndex.get(termId).addAll(partialPostingList);
    }

    return position;
  }

  /**
   *
   */
  private int populateTmpInvertedIndex(ListMultimap<Integer, Integer> tmpInvertedIndex, String str, int docid, int position) {
    Tokenizer tokenizer = new Tokenizer(new StringReader(str));

    /**************************************************************************
     * Start to process the content one term at a time.
     *************************************************************************/
    while (tokenizer.hasNext()) {
      // Get the lower case term
      String term = Tokenizer.lowercaseFilter(tokenizer.getText());
      // Stemming
      term = Tokenizer.krovetzStemmerFilter(term);

      // Update the total term frequency
      totalTermFrequency++;

      // Add the term into the dictionary and get its term ID
      int termId = addTermIntoDictionary(term);

      // Update the meta data
      if (!meta.containsKey(termId)) {
        MetaData metaData = new MetaData();
        meta.put(termId, metaData);
      }

      // Update the term frequency across the whole corpus.
      long corpusTermFrequency = meta.get(termId).getCorpusTermFrequency();
      meta.get(termId).setCorpusTermFrequency(corpusTermFrequency + 1);

      // Update docTermFrequency
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
        tmpInvertedIndex.get(termId).add(docid);
        tmpInvertedIndex.get(termId).add(1);
        tmpInvertedIndex.get(termId).add(position);

        // Update the term's corpus document frequency.
        int corpusDocFrequencyByTerm = meta.get(termId).getCorpusDocFrequencyByTerm();
        meta.get(termId).setCorpusDocFrequencyByTerm(corpusDocFrequencyByTerm + 1);
      }

      // Move to the next position
      position++;
    }

    return position;
  }

  /**
   * Convert the offsets of each posting list to deltas. e.g. From: List: 0
   * (docid), 4 (occurrence), 5, 12, 18, 29 To: List: 0 (docid), 4 (occurrence),
   * 5, 7, 6, 11
   *
   * @param partialInvertedIndex The partial/temporary inverted index
   */
  protected void convertPositionToDelta(
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

  /**
   * Split and save the inverted index and document term frequency.
   *
   * @param corpusName   the corpus file name.
   * @param documentName the document term frequency file name.
   * @param extension    the extension.
   * @param fileNum      the file number
   * @throws java.io.IOException
   */
  protected void split(String corpusName, String documentName, String extension, int fileNum) throws IOException {
    String corpusFileName = corpusName + String.format("%03d", fileNum) + extension;
    String documentFileName = documentName + String.format("%03d", fileNum) + extension;

    splitInvertedIndex(corpusFileName);
    splitDocTermFrequency(documentFileName);

    invertedIndex.clear();
    docTermFrequency.clear();
  }

  /**
   * Write partial compressed inverted index to a file
   *
   * @param fileName file name
   * @throws IOException
   */
  private void splitInvertedIndex(String fileName) throws IOException {
    String indexPartialFile = options._indexPrefix + "/" + fileName;
    Output output = new Output(new FileOutputStream(indexPartialFile));
    Kryo kryo = new Kryo();

    SortedSet<Integer> sortedSet = new TreeSet<Integer>();

    // Sort the keys alphabetically...
    sortedSet.addAll(invertedIndex.keySet());

    // Record the number of objects first...
    int numOfEntries = sortedSet.size();
    kryo.writeObject(output, numOfEntries);

    // Write the entries one by one...
    for (int termId : sortedSet) {
      kryo.writeObject(output, termId);
      kryo.writeObject(output, new ArrayList<Byte>(invertedIndex.get(termId)));
    }

    output.close();
  }

  /**
   * Write partial document term frequency to a file
   *
   * @param fileName file name
   * @throws IOException
   */
  private void splitDocTermFrequency(String fileName) throws IOException {
    String indexPartialFile = options._indexPrefix + "/" + fileName;
    Kryo kryo = new Kryo();
    Output output = new Output(new FileOutputStream(indexPartialFile));

    // First write the number of entries...
    int numOfEntries = docTermFrequency.keySet().size();
    kryo.writeObject(output, numOfEntries);

    // For each document, write all terms and corresponding frequency.
    for (int docId : docTermFrequency.keySet()) {
      Multiset<Integer> tmpMultiset = docTermFrequency.get(docId);

      // Write the document ID first
      kryo.writeObject(output, docId);

      List<Byte> termIdAndFrequency = new ArrayList<Byte>();

      for (int termId : tmpMultiset.elementSet()) {
        // Add the encoded termId and count/frequency to the list
        termIdAndFrequency.addAll(VByteUtil.vByteEncoding(termId));
        termIdAndFrequency.addAll(VByteUtil.vByteEncoding(tmpMultiset.count(termId)));
      }

      kryo.writeObject(output, termIdAndFrequency);
    }

    output.close();
  }

  /**
   * Add the term into the dictionary and return its term ID.
   */
  private int addTermIntoDictionary(String term) {
    if (dictionary.containsKey(term)) {
      return dictionary.get(term);
    } else {
      int termId = dictionary.size();
      dictionary.put(term, termId);
      return termId;
    }
  }

  /**
   * Check if the inverted index has reach the memory threshold
   *
   * @return true if the threshold has met
   */
  public boolean hasReachThresholdCompress() {
    Multiset<Integer> multiset = invertedIndex.keys();
    return multiset.size() > IndexerConstant.PARTIAL_FILE_SIZE;
  }

  public Map<Integer, MetaData> getMeta() {
    return meta;
  }

  public List<Document> getDocuments() {
    return documents;
  }

  public long getTotalTermFrequency() {
    return totalTermFrequency;
  }

  public BiMap<String, Integer> getDictionary() {
    return dictionary;
  }

  public Map<Integer, ExtentList> getExtentListMap() {
    return extentListMap;
  }
}
