package edu.nyu.cs.cs2580.evaluator;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.io.Files;
import com.sun.javafx.binding.StringFormatter;
import edu.nyu.cs.cs2580.BKTree.BKTree;
import edu.nyu.cs.cs2580.BKTree.DamerauLevenshteinAlgorithm;
import edu.nyu.cs.cs2580.BKTree.DistanceAlgo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpellCorrectionEvaluator {
  static BiMap<String, Integer> dictionary = HashBiMap.create();
  static Multiset<String> termFrequency = HashMultiset.create();
  static Map<String, String> test1Map = new HashMap<String, String>();
  static Map<String, String> test2Map = new HashMap<String, String>();

  public static void main(String[] args) throws IOException {
    // Test corpus file
    File testCorpusFile = new File("data/spellCheckTestData/big.text");

    // Original files in order to construct the map
    File test1File = new File("data/spellCheckTestData/test1.text");
    File test2File = new File("data/spellCheckTestData/test2.text");

    constructDictionary(testCorpusFile);

    // Two sets of test map
    // Key: misspelled word
    // Value: correct word
    test1Map = constructTestMap(test1File);
    test2Map = constructTestMap(test2File);

    testBKTree();
  }

  private static void testBKTree() {
    /********** Spell check implemented with BK Tree and Damerau Levenshitein algorithm ***********/
    DistanceAlgo<String> distanceAlgo = new DamerauLevenshteinAlgorithm<String>();
    BKTree<String> bkTree = new BKTree<String>(distanceAlgo, termFrequency);
    bkTree.addAll(dictionary.keySet());
    int correctCount = 0;
    int totalCount = test2Map.size();

    for (String misspellWord : test2Map.keySet()) {
      String correctWord = bkTree.getPossibleNodesForDistanceWithOrder(misspellWord, 2, 1).get(0);
      if (test2Map.get(misspellWord).equals(correctWord)) {
        correctCount++;
      }
    }

    double percent = (double) correctCount / (double) totalCount;

    String correctPercentage = String.format("Correct rate is %,.2f.", percent);

    System.out.println(correctPercentage);
  }

  /**
   * Construct a map from a text file for evaluation.
   * Key: misspelled word
   * Value: correct word
   */
  private static Map<String, String> constructTestMap(File file) throws IOException {
    Map<String, String> map = new HashMap<String, String>();
    List<String> pairs = new ArrayList<String>();
    String content = Files.toString(file, StandardCharsets.UTF_8);

    Matcher m = Pattern.compile("\\'([a-zA-Z ]*)\\':[ \\n]*\\'([a-zA-Z ]*)\\'").matcher(content);
    while (m.find()) {
      String correctWord = m.group(1);
      String[] misspellWords = m.group(2).split(" ");

      for (String misspellWord : misspellWords) {
        map.put(misspellWord, correctWord);
      }
    }

    return map;
  }

  /**
   * Construct the dictionary from a sample corpus
   */
  private static void constructDictionary(File testCorpusFile) {
    try {
      Scanner scanner = new Scanner(testCorpusFile);
      while (scanner.hasNextLine()) {
        processLineForDictionary(scanner.nextLine());
      }
      scanner.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  /**
   * Helper function.
   */
  private static void processLineForDictionary(String line) {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (Character.isLetter(c)) {
        sb.append(c);
      } else {
        if (sb.length() > 0) {
          processLineForDictionaryHelper(sb);
        }
      }
    }

    processLineForDictionaryHelper(sb);
  }

  /**
   * Helper function.
   */
  private static void processLineForDictionaryHelper(StringBuilder sb) {
    // Get a term
    String term = sb.toString().toLowerCase();
    // Update the term frequency
    termFrequency.add(term);
    // Update the dictionary
    if (!dictionary.containsKey(term)) {
      int termId = dictionary.size();
      dictionary.put(term, termId);
    }
    // Clean the string builder
    sb.setLength(0);
  }
}
