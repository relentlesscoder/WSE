package edu.nyu.cs.cs2580;

import com.google.common.collect.*;

import java.io.*;
import java.util.*;

public class Util {
  private static final long SIZE_PER_FILE_Integer = 6000000;
  private static final long SIZE_PER_FILE_Byte = 3000000;

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
    return multiset.size() > SIZE_PER_FILE_Integer;
  }

  public static boolean hasReachThresholdCompress(Multimap<String, Byte> invertedIndex) {
    Multiset<String> multiset = invertedIndex.keys();
    return multiset.size() > SIZE_PER_FILE_Byte;
  }

  public static void writePartialInvertedIndex(Multimap<String, Integer> invertedIndex, SearchEngine.Options _options, int count) throws IOException {
    String indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", count) + ".idx";
    ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(indexPartialFile));
    SortedSet<String> sortedSet = new TreeSet<String>();

    // Sort the keys alphabetically...
    sortedSet.addAll(invertedIndex.keySet());

    // Record the number of objects first...
    int numOfEntries = sortedSet.size();
    writer.writeInt(numOfEntries);

    // Write the entries one by one...
    for (String term : sortedSet) {
      List<Integer> list = new ArrayList<Integer>(invertedIndex.get(term));
      writer.writeUTF(term);
      writer.writeObject(list);
      writer.reset();
      writer.flush();
    }

    writer.close();
  }

  public static void writePartialInvertedIndexCompress(Multimap<String, Byte> invertedIndex, SearchEngine.Options _options, int count) throws IOException {
    String indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", count) + ".idx";
    ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(indexPartialFile));
    SortedSetMultimap<String, Byte> sortedSetMultimap = TreeMultimap.create(Ordering.natural(), Ordering.arbitrary());

    // Sort the keys alphabetically...
    sortedSetMultimap.putAll(invertedIndex);
    invertedIndex.clear();

    // Record the number of objects first...
    int numOfEntries = sortedSetMultimap.keySet().size();
    writer.writeInt(numOfEntries);

    // Write the entries one by one...
    for (Map.Entry entry : sortedSetMultimap.asMap().entrySet()) {
      String term = (String) entry.getKey();
      List<Byte> list = new ArrayList<Byte>((java.util.Collection<? extends Byte>) entry.getValue());
      writer.writeUTF(term);
      writer.writeObject(list);
      writer.reset();
      writer.flush();
    }

    writer.close();
  }








  public static void serializeCompressedInvertedIndex(Multimap<String, Byte> invertedIndex, SearchEngine.Options _options) throws IOException {
    ListMultimap<String, Byte> invertedIndexBuffer = ArrayListMultimap.create();
    long sizeOfPostingList = 0;
    long sizeCount = 0;
    long fileCount = 1;
    String indexPartialFile = "";

    // Count the size of the posting list first
    for (String term : invertedIndex.keySet()) {
      sizeOfPostingList += invertedIndex.keys().count(term);
    }

    for (String term : invertedIndex.keySet()) {
      invertedIndexBuffer.get(term).addAll(invertedIndex.get(term));
      sizeCount += invertedIndex.get(term).size();
      if (sizeCount > SIZE_PER_FILE_Integer) {
        indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", fileCount) + ".idx";
        writeCompressedObject(invertedIndexBuffer, indexPartialFile);
        invertedIndexBuffer.clear();
        sizeCount = 0;
        fileCount++;
      }
    }

    indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", fileCount) + ".idx";
    writeCompressedObject(invertedIndexBuffer, indexPartialFile);
  }

  public static void serializeInvertedIndex(Multimap<String, Integer> invertedIndex, SearchEngine.Options _options) throws IOException {
    ListMultimap<String, Integer> invertedIndexBuffer = ArrayListMultimap.create();
    long sizeOfPostingList = 0;
    long sizeCount = 0;
    long fileCount = 1;
    String indexPartialFile = "";

    // Count the size of the posting list first
    for (String term : invertedIndex.keySet()) {
      sizeOfPostingList += invertedIndex.keys().count(term);
    }

    for (String term : invertedIndex.keySet()) {
      invertedIndexBuffer.get(term).addAll(invertedIndex.get(term));
      sizeCount += invertedIndex.get(term).size();
      if (sizeCount > SIZE_PER_FILE_Integer) {
        indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", fileCount) + ".idx";
        ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(indexPartialFile));
        for (Map.Entry entry : invertedIndex.entries()) {
          writer.writeUnshared(entry);
          writer.reset();
          writer.flush();
        }
        writer.close();
        invertedIndexBuffer.clear();
        sizeCount = 0;
        fileCount++;
      }
    }

    indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", fileCount) + ".idx";
    ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(indexPartialFile));
    for (Map.Entry entry : invertedIndex.entries()) {
      writer.writeUnshared(entry);
    }
    writer.close();
  }

  private static void writeCompressedObject(ListMultimap<String, Byte> invertedIndexBuffer, String indexPartialFile) throws IOException {
    System.out.println("Storing partial index to: " + indexPartialFile);
    ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(indexPartialFile));
    writer.writeUnshared(invertedIndexBuffer);
    writer.reset();
    writer.flush();
    writer.close();
  }

  public static ListMultimap<String, Byte> deserializeCompressedInvertedIndex(File file) throws IOException, ClassNotFoundException {
    ListMultimap<String, Byte> partialInvertedIndex = ArrayListMultimap.create();
    String indexFile = file.getAbsolutePath();

    if (file.getName().matches("^corpus[0-9]+\\.idx")) {
      System.out.println("Load partial index from: " + indexFile);

      ObjectInputStream reader = new ObjectInputStream(new FileInputStream(indexFile));
      partialInvertedIndex = (ListMultimap<String, Byte>) reader.readUnshared();
      reader.close();
    }

    return partialInvertedIndex;
  }

  public static ListMultimap<String, Integer> deserializeInvertedIndex(File file) throws IOException, ClassNotFoundException {
    ListMultimap<String, Integer> partialInvertedIndex = ArrayListMultimap.create();
    String indexFile = file.getAbsolutePath();

    if (file.getName().matches("^corpus[0-9]+\\.idx")) {
      System.out.println("Load partial index from: " + indexFile);

      ObjectInputStream reader = new ObjectInputStream(new FileInputStream(indexFile));
      partialInvertedIndex = (ListMultimap<String, Integer>) reader.readUnshared();
      reader.close();
    }

    return partialInvertedIndex;
  }

}
