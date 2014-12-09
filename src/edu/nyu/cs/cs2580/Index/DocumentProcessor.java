package edu.nyu.cs.cs2580.Index;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.*;
import edu.nyu.cs.cs2580.Document.Document;
import edu.nyu.cs.cs2580.Rankers.IndexerConstant;
import edu.nyu.cs.cs2580.SearchEngine;
import edu.nyu.cs.cs2580.Tokenizer;
import edu.nyu.cs.cs2580.Utils.ProgressBar;
import edu.nyu.cs.cs2580.Utils.VByteUtil;

import java.io.*;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This is the abstract class of document processor.
 */
public abstract class DocumentProcessor {
  /**
   * Temporary data for constructing the index.
   */
  protected class ConstructTmpData {
    public int lastDocid;
    public int lastPostingListSize;
    public int lastSkipPointerOffset;

    public ConstructTmpData() {
      lastDocid = -1;
      lastPostingListSize = -1;
      lastSkipPointerOffset = -1;
    }
  }

  // K is the length of interval for the skip pointer of the posting list.
  protected static final int K = 10000;



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

  // The offset of each docid of the posting list for each term.
  // Key: Term ID
  // Value: The offsets for each of docid in the posting list.
  protected ListMultimap<Integer, Byte> skipPointers;

  // Key: Term ID
  // Value: MetaData
  // MetaData {
  //   corpusTermFrequency: Term frequency across whole corpus.
  //   corpusDocFrequencyByTerm: Number of documents a term appeared, over the full corpus.
  //   postingListMetaData
  // }
  protected Map<Integer, MetaData> meta;

  protected List<Document> documents;

  protected SearchEngine.Options options;

  protected ProgressBar progressBar;

  // Files in the corpus folder
  protected File[] files;

  // Temporary data structure
  protected Map<Integer, ConstructTmpData> constructTmpDataMap;

  protected long totalTermFrequency;

  protected int partialFileCount;

  protected DocumentProcessor(File[] files, SearchEngine.Options options) {
    this.dictionary = HashBiMap.create();

    this.invertedIndex = ArrayListMultimap.create();

    this.docTermFrequency = new HashMap<Integer, Multiset<Integer>>();

    this.skipPointers = ArrayListMultimap.create();

    this.meta = new HashMap<Integer, MetaData>();

    this.documents = new ArrayList<Document>();

    this.constructTmpDataMap = new HashMap<Integer, ConstructTmpData>();
    ;

    this.progressBar = new ProgressBar();

    this.partialFileCount = 0;

    this.totalTermFrequency = 0;

    this.files = files;

    this.options = options;
  }

  /**
   * Process the corpus documents and generate files for index, document term frequency, dictionary.
   * It also generate some meta data for the indexer.
   * <p>
   * For each of the document, call {@link populateInvertedIndex} in order to populate the inverted index.
   */
  public abstract void processDocuments() throws IOException;

  /**
   * Populate the inverted index provided by a document's content and its ID.
   *
   * @param content The content of the document.
   * @param docid   The document ID.
   */
  protected void populateInvertedIndex(String content, int docid) {
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
      // TODO:
//      _totalTermFrequency++;
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

  ;

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
   * Check if the inverted index has reach the memory threshold
   *
   * @param invertedIndex inverted index
   * @return true if the threshold has met
   */
  public boolean hasReachThresholdCompress() {
    Multiset<Integer> multiset = invertedIndex.keys();
    return multiset.size() > IndexerConstant.PARTIAL_FILE_SIZE;
  }

  public ListMultimap<Integer, Byte> getSkipPointers() {
    return skipPointers;
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
}