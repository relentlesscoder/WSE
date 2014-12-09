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
