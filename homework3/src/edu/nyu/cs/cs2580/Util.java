package edu.nyu.cs.cs2580;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import java.util.Map;
import java.util.Map.Entry;

import java.io.*;
import java.util.*;

public class Util {
  private static final long MEGABYTE = 1024L * 1024L;
  private static final long PARTIAL_FILE_SIZE = 6 * MEGABYTE;
  protected static final long MAX_INVERTED_INDEX_SIZE = 12 * MEGABYTE;

  /**
   * Convert the time stamp to a proper time format.
   * @param timeStamp time stamp represented in millis seconds
   * @return proper time format
   */
  public static String convertMillis(long timeStamp) {
    long hours, minutes, seconds, millis;
    millis = timeStamp % 1000;
    seconds = (timeStamp / 1000) % 60;
    minutes = (timeStamp / (1000 * 60)) % 60;
    hours = (timeStamp / (1000 * 60 * 60)) % 24;
    return String.format("%02d:%02d:%02d:%03d", hours, minutes, seconds, millis);
  }

  /**
   * Serialize the object.
   * @param obj object
   * @return array of bytes
   * @throws IOException
   */
  public static byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream os = new ObjectOutputStream(out);
    os.writeObject(obj);
    return out.toByteArray();
  }

  /**
   * Deserialize the object
   * @param data object represented in array of bytes
   * @return object
   * @throws IOException
   * @throws ClassNotFoundException
   */
  public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
    ByteArrayInputStream in = new ByteArrayInputStream(data);
    ObjectInputStream is = new ObjectInputStream(in);
    return is.readObject();
  }

  /**
   * Check if the inverted index has reach the memory threshold
   * @param invertedIndex inverted index
   * @return true if the threshold has met
   */
  public static boolean hasReachThreshold(Multimap<Integer, Integer> invertedIndex) {
    Multiset<Integer> multiset = invertedIndex.keys();
    return multiset.size() > PARTIAL_FILE_SIZE;
  }

  /**
   * Check if the inverted index has reach the memory threshold
   * @param invertedIndex inverted index
   * @return true if the threshold has met
   */
  public static boolean hasReachThresholdCompress(Multimap<Integer, Byte> invertedIndex) {
    Multiset<Integer> multiset = invertedIndex.keys();
    return multiset.size() > PARTIAL_FILE_SIZE;
  }

  /**
   * Write partial inverted index to a file
   * @param invertedIndex inverted index
   * @param _options configs
   * @param fileNum file number
   * @throws IOException
   */
  public static void writePartialInvertedIndex(Multimap<Integer, Integer> invertedIndex, SearchEngine.Options _options, int fileNum) throws IOException {
    String indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", fileNum) + ".idx";
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
      kryo.writeObject(output, new ArrayList<Integer>(invertedIndex.get(termId)));
    }

    output.close();
  }

  /**
   * Write partial compressed inverted index to a file
   * @param invertedIndex inverted index
   * @param _options configs
   * @param fileNum file number
   * @throws IOException
   */
  public static void writePartialInvertedIndexCompress(Multimap<Integer, Byte> invertedIndex, SearchEngine.Options _options, int fileNum) throws IOException {
    String indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", fileNum) + ".idx";
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
   * @param docTermFrequency document term frequency
   * @param _options configs
   * @param fileNum file number
   * @throws IOException
   */
  public static void writePartialDocuments(Map<Integer, Multiset<Integer>> docTermFrequency, SearchEngine.Options _options, int fileNum) throws IOException {
    String indexPartialFile = _options._indexPrefix + "/documents" + String.format("%03d", fileNum) + ".idx";
    Kryo kryo = new Kryo();
    Output output = new Output(new FileOutputStream(indexPartialFile));

    int numOfEntries = docTermFrequency.keySet().size();
    kryo.writeObject(output, numOfEntries);

    for (int docId : docTermFrequency.keySet()) {
      Map<Integer, Integer> tmpMap = new HashMap<Integer, Integer>();
      Multiset<Integer> tmpMultiset = docTermFrequency.get(docId);

      for (Multiset.Entry entry : tmpMultiset.entrySet()) {
        int termId = (Integer) entry.getElement();
        int count = (Integer) entry.getCount();
        tmpMap.put(termId, count);
      }

      kryo.writeObject(output, docId);
      kryo.writeObject(output, tmpMap);
    }

    output.close();
  }

  /**
   * Merge all partial doc term frequency file...
   * @param docMetaData meta data
   * @param _options configs
   * @throws IOException
   */
  public static void mergeDocumentTermFrequency(Map<Integer, MetaPair> docMetaData, SearchEngine.Options _options) throws IOException {
    String invertedIndexFileName = _options._indexPrefix + "/documents.idx";
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
      if (f.getName().matches("^documents[0-9]+\\.idx")) {
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
            "^documents" + String.format("%03d", i) + "\\.idx")) {
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
        Multiset<Integer> tmpMultiset = HashMultiset.create();
        int docid = kryo.readObject(inputs[i], Integer.class);
        Map<Integer, Integer> tmpMap = kryo.readObject(inputs[i], HashMap.class);

        for (Map.Entry entry : tmpMap.entrySet()) {
          int termId = (Integer) entry.getKey();
          int count = (Integer) entry.getValue();
          tmpMultiset.setCount(termId, count);
        }

        currentPos = raf.length();
        raf.seek(currentPos);
        raf.write(Util.serialize(tmpMultiset));

        // Assume the posting list will not be too big...
        length = (int) (raf.length() - currentPos);
        docMetaData.put(docid, new MetaPair(currentPos, length));

        docCount++;
      }

      inputs[i].close();
    }
    System.out.println("Merging docs...: " + docCount);

    /**************************************************************************
     * Wrapping up...
     *************************************************************************/
    for (File f : folder.listFiles()) {
      if (f.getName().matches("^documents[0-9]+\\.idx")) {
        // Delete all partial index file
        f.delete();
      }
    }
  }

  /**
   * Simple function to check if a string is a number...
   * @param str string
   * @return true if it is.
   */
  public static boolean isNumber(String str) {
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (!Character.isDigit(c)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Prints {@code msg} and exits the program if {@code condition} is false.
   */
  public static void Check(boolean condition, String msg) {
    if (!condition) {
      System.err.println("Fatal error: " + msg);
      System.exit(-1);
    }
  }

  public static <K extends Comparable,V extends Comparable> LinkedHashMap<K,V> sortHashMapByValues(Map<K,V> map, final boolean descending){
    List<Map.Entry<K,V>> entries = new LinkedList<Map.Entry<K,V>>(map.entrySet());
    Collections.sort(entries, new Comparator<Map.Entry<K,V>>() {

      @Override
      public int compare(Entry<K, V> o1, Entry<K, V> o2) {
        int order = descending ? -1 : 1;
        int result = o1.getValue().compareTo(o2.getValue());
        // if two metrics are the same, use docid(url) to break the tie
        if(result == 0){
          result = o1.getKey().compareTo(o2.getKey());
        }
        return result * order;
      }
    });

    LinkedHashMap<K,V> sortedMap = new LinkedHashMap<K,V>();
    for(Map.Entry<K,V> entry: entries){
      sortedMap.put(entry.getKey(), entry.getValue());
    }
    return sortedMap;
  }
}
