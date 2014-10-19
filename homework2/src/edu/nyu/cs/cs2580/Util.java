package edu.nyu.cs.cs2580;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import java.io.*;

public class Util {
  private static final long SIZE_PER_FILE = 10000000;

  public static String convertMillis(long timeStamp) {
    long hours, minutes, seconds, millis;
    millis = timeStamp % 1000;
    seconds = (timeStamp / 1000) % 60;
    minutes = (timeStamp / (1000 * 60)) % 60;
    hours = (timeStamp / (1000 * 60 * 60)) % 24;
    return String.format("%02d:%02d:%02d:%03d", hours, minutes, seconds, millis);
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
      if (sizeCount > SIZE_PER_FILE) {
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
      if (sizeCount > SIZE_PER_FILE) {
        indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", fileCount) + ".idx";
        writeObject(invertedIndexBuffer, indexPartialFile);
        invertedIndexBuffer.clear();
        sizeCount = 0;
        fileCount++;
      }
    }

    indexPartialFile = _options._indexPrefix + "/corpus" + String.format("%03d", fileCount) + ".idx";
    writeObject(invertedIndexBuffer, indexPartialFile);
  }


  private static void writeCompressedObject(ListMultimap<String, Byte> invertedIndexBuffer, String indexPartialFile) throws IOException {
    System.out.println("Storing partial index to: " + indexPartialFile);
    ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(indexPartialFile));
    writer.writeUnshared(invertedIndexBuffer);
    writer.reset();
    writer.flush();
    writer.close();
  }

  private static void writeObject(ListMultimap<String, Integer> invertedIndexBuffer, String indexPartialFile) throws IOException {
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

      ObjectInputStream reader = new ObjectInputStream(new FileInputStream(
          indexFile));
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

      ObjectInputStream reader = new ObjectInputStream(new FileInputStream(
          indexFile));
      partialInvertedIndex = (ListMultimap<String, Integer>) reader.readUnshared();
      reader.close();
    }

    return partialInvertedIndex;
  }

}
