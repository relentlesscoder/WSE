package edu.nyu.cs.cs2580;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class Util {
  protected static final long MAX_INVERTED_INDEX_SIZE = 10000000;
  private static final long PARTIAL_INDEX_SIZE = 10000000;

  public static String convertMillis(long timeStamp) {
    long hours, minutes, seconds, millis;
    millis = timeStamp % 1000;
    seconds = (timeStamp / 1000) % 60;
    minutes = (timeStamp / (1000 * 60)) % 60;
    hours = (timeStamp / (1000 * 60 * 60)) % 24;
    return String.format("%02d:%02d:%02d:%03d", hours, minutes, seconds, millis);
  }

  public static byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream os = new ObjectOutputStream(out);
    os.writeObject(obj);
    return out.toByteArray();
  }

  public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
    ByteArrayInputStream in = new ByteArrayInputStream(data);
    ObjectInputStream is = new ObjectInputStream(in);
    return is.readObject();
  }

  public static boolean hasReachThreshold(Multimap<String, Integer> invertedIndex) {
    Multiset<String> multiset = invertedIndex.keys();
    return multiset.size() > PARTIAL_INDEX_SIZE;
  }

  public static boolean hasReachThresholdCompress(Multimap<String, Byte> invertedIndex) {
    Multiset<String> multiset = invertedIndex.keys();
    return multiset.size() > PARTIAL_INDEX_SIZE;
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
}
