package edu.nyu.cs.cs2580.Utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Bytes;
import edu.nyu.cs.cs2580.Index.Offsets;
import edu.nyu.cs.cs2580.SearchEngine;

import java.util.Map;
import java.util.Map.Entry;

import java.io.*;
import java.util.*;

public class Util {
  private static final long MEGABYTE = 1024L * 1024L;
  private static final long PARTIAL_FILE_SIZE = 6 * MEGABYTE;
  public static final long MAX_INVERTED_INDEX_SIZE = 12 * MEGABYTE;

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
   * @param fileName file name
   * @throws IOException
   */
  public static void writePartialInvertedIndexCompress(Multimap<Integer, Byte> invertedIndex, SearchEngine.Options _options, String fileName) throws IOException {
    String indexPartialFile = _options._indexPrefix + "/" + fileName;
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
   * @param fileName file name
   * @throws IOException
   */
  public static void writePartialDocuments(Map<Integer, Multiset<Integer>> docTermFrequency, SearchEngine.Options _options, String fileName) throws IOException {
    String indexPartialFile = _options._indexPrefix + "/" + fileName;
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

  // Utility for ignoring hidden files in the file system.
  protected static boolean isValidDocument(File file) {
    return !file.getName().startsWith(".");  // Remove hidden files.
  }
}
