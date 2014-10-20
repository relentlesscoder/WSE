package edu.nyu.cs.cs2580;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class Util {
  protected static final long SIZE_PER_FILE_MAP_INTEGER = 250000;
  protected static final long SIZE_PER_FILE_MAP_Byte = 250000;
  protected static final long MAX_INVERTED_INDEX_SIZE = 10000000;
  private static final long SIZE_PER_FILE_INTEGER = 6000000;
  private static final long SIZE_PER_FILE_Byte = 6000000;

  public static String convertMillis(long timeStamp) {
    long hours, minutes, seconds, millis;
    millis = timeStamp % 1000;
    seconds = (timeStamp / 1000) % 60;
    minutes = (timeStamp / (1000 * 60)) % 60;
    hours = (timeStamp / (1000 * 60 * 60)) % 24;
    return String.format("%02d:%02d:%02d:%03d", hours, minutes, seconds, millis);
  }

  public static boolean hasReachThreshold(Multimap<String, Integer> invertedIndex) {
    Multiset<String> multiset = invertedIndex.keys();
    return multiset.size() > SIZE_PER_FILE_INTEGER;
  }

  public static boolean hasReachThresholdCompress(Multimap<String, Byte> invertedIndex) {
    Multiset<String> multiset = invertedIndex.keys();
    return multiset.size() > SIZE_PER_FILE_Byte;
  }

  public static void writePartialInvertedIndex(Multimap<String, Integer> invertedIndex, SearchEngine.Options _options, int count) throws IOException {
    String indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", count) + ".idx";
    Output output = new Output(new FileOutputStream(indexPartialFile));
    Kryo kryo = new Kryo();

    SortedSet<String> sortedSet = new TreeSet<String>();

    // Sort the keys alphabetically...
    sortedSet.addAll(invertedIndex.keySet());

    // Record the number of objects first...
    int numOfEntries = sortedSet.size();
    kryo.writeObject(output, numOfEntries);

    // Write the entries one by one...
    for (String term : sortedSet) {
      List<Integer> list = new ArrayList<Integer>(invertedIndex.get(term));
      kryo.writeObject(output, new String(term));
      kryo.writeObject(output, list);
    }
    output.close();
  }

  public static void writePartialInvertedIndexCompress(Multimap<String, Byte> invertedIndex, SearchEngine.Options _options, int count) throws IOException {
    String indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", count) + ".idx";
    Output output = new Output(new FileOutputStream(indexPartialFile));
    Kryo kryo = new Kryo();

    SortedSet<String> sortedSet = new TreeSet<String>();

    // Sort the keys alphabetically...
    sortedSet.addAll(invertedIndex.keySet());

    // Record the number of objects first...
    int numOfEntries = sortedSet.size();
    kryo.writeObject(output, numOfEntries);

    // Write the entries one by one...
    for (String term : sortedSet) {
      List<Byte> list = new ArrayList<Byte>(invertedIndex.get(term));
      kryo.writeObject(output, new String(term));
      kryo.writeObject(output, list);
    }
    output.close();
  }


//  public static void serializeCompressedInvertedIndex(Multimap<String, Byte> invertedIndex, SearchEngine.Options _options) throws IOException {
//    ListMultimap<String, Byte> invertedIndexBuffer = ArrayListMultimap.create();
//    long sizeOfPostingList = 0;
//    long sizeCount = 0;
//    long fileCount = 1;
//    String indexPartialFile = "";
//
//    // Count the size of the posting list first
//    for (String term : invertedIndex.keySet()) {
//      sizeOfPostingList += invertedIndex.keys().count(term);
//    }
//
//    for (String term : invertedIndex.keySet()) {
//      invertedIndexBuffer.get(term).addAll(invertedIndex.get(term));
//      sizeCount += invertedIndex.get(term).size();
//      if (sizeCount > SIZE_PER_FILE_INTEGER) {
//        indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", fileCount) + ".idx";
//        writeCompressedObject(invertedIndexBuffer, indexPartialFile);
//        invertedIndexBuffer.clear();
//        sizeCount = 0;
//        fileCount++;
//      }
//    }
//
//    indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", fileCount) + ".idx";
//    writeCompressedObject(invertedIndexBuffer, indexPartialFile);
//  }
//
//  public static void serializeInvertedIndex(Multimap<String, Integer> invertedIndex, SearchEngine.Options _options) throws IOException {
//    ListMultimap<String, Integer> invertedIndexBuffer = ArrayListMultimap.create();
//    long sizeOfPostingList = 0;
//    long sizeCount = 0;
//    long fileCount = 1;
//    String indexPartialFile = "";
//
//    // Count the size of the posting list first
//    for (String term : invertedIndex.keySet()) {
//      sizeOfPostingList += invertedIndex.keys().count(term);
//    }
//
//    for (String term : invertedIndex.keySet()) {
//      invertedIndexBuffer.get(term).addAll(invertedIndex.get(term));
//      sizeCount += invertedIndex.get(term).size();
//      if (sizeCount > SIZE_PER_FILE_INTEGER) {
//        indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", fileCount) + ".idx";
//        ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(indexPartialFile));
//        for (Map.Entry entry : invertedIndex.entries()) {
//          writer.writeUnshared(entry);
//          writer.reset();
//          writer.flush();
//        }
//        writer.close();
//        invertedIndexBuffer.clear();
//        sizeCount = 0;
//        fileCount++;
//      }
//    }
//
//    indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", fileCount) + ".idx";
//    ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(indexPartialFile));
//    for (Map.Entry entry : invertedIndex.entries()) {
//      writer.writeUnshared(entry);
//    }
//    writer.close();
//  }
//
//  private static void writeCompressedObject(ListMultimap<String, Byte> invertedIndexBuffer, String indexPartialFile) throws IOException {
//    System.out.println("Storing partial index to: " + indexPartialFile);
//    ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(indexPartialFile));
//    writer.writeUnshared(invertedIndexBuffer);
//    writer.reset();
//    writer.flush();
//    writer.close();
//  }
//
//  public static ListMultimap<String, Byte> deserializeCompressedInvertedIndex(File file) throws IOException, ClassNotFoundException {
//    ListMultimap<String, Byte> partialInvertedIndex = ArrayListMultimap.create();
//    String indexFile = file.getAbsolutePath();
//
//    if (file.getName().matches("^corpus[0-9]+\\.idx")) {
//      System.out.println("Load partial index from: " + indexFile);
//
//      ObjectInputStream reader = new ObjectInputStream(new FileInputStream(indexFile));
//      partialInvertedIndex = (ListMultimap<String, Byte>) reader.readUnshared();
//      reader.close();
//    }
//
//    return partialInvertedIndex;
//  }
//
//  public static ListMultimap<String, Integer> deserializeInvertedIndex(File file) throws IOException, ClassNotFoundException {
//    ListMultimap<String, Integer> partialInvertedIndex = ArrayListMultimap.create();
//    String indexFile = file.getAbsolutePath();
//
//    if (file.getName().matches("^corpus[0-9]+\\.idx")) {
//      System.out.println("Load partial index from: " + indexFile);
//
//      ObjectInputStream reader = new ObjectInputStream(new FileInputStream(indexFile));
//      partialInvertedIndex = (ListMultimap<String, Integer>) reader.readUnshared();
//      reader.close();
//    }
//
//    return partialInvertedIndex;
//  }

}
