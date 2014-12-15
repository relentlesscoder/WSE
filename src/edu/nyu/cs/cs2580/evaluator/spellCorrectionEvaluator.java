package edu.nyu.cs.cs2580.evaluator;

import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.io.Files;
import edu.nyu.cs.cs2580.spellCheck.BKTree.BKTree;
import edu.nyu.cs.cs2580.spellCheck.BKTree.DamerauLevenshteinAlgorithm;
import edu.nyu.cs.cs2580.spellCheck.BKTree.DistanceAlgo;
import edu.nyu.cs.cs2580.spellCheck.BKTree.MisspellDataSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This evaluation is base on the following web page: http://norvig.com/spell-correct.html.
 * This including the corpus and two test sets.
 */

public class SpellCorrectionEvaluator {
  // Dictionary of the test corpus
  static BiMap<String, Integer> dictionary = HashBiMap.create();
  // Term frequency of the test corpus
  static Multiset<String> termFrequency = HashMultiset.create();
  // The misspell data set...
  static MisspellDataSet misspellDataSet = new MisspellDataSet();

  public static void main(String[] args) throws IOException {
    // Test corpus file
    File testCorpusFile = new File("data/spellCheckTestData/big.text");

    // Original files in order to construct the map
    File test1File = new File("data/spellCheckTestData/test1.text");
    File test2File = new File("data/spellCheckTestData/test2.text");

    // Common misspell data corpora file
    File aspellFile = new File("data/spellCheckTestData/aspell.dat");
    File misspFile = new File("data/spellCheckTestData/missp.dat");
    File wikipediaFile = new File("data/spellCheckTestData/wikipedia.dat");

    misspellDataSet.addData(aspellFile);
    misspellDataSet.addData(misspFile);
    misspellDataSet.addData(wikipediaFile);

    // Two sets of test map
    // Key: misspelled word
    // Value: correct word
    Map<String, String> test1Map = new HashMap<String, String>();
    Map<String, String> test2Map = new HashMap<String, String>();

    constructDictionary(testCorpusFile);

    test1Map = constructTestMap(test1File);
    test2Map = constructTestMap(test2File);

    System.out.println("***** This is the test for BKTree with test set 1");
    testBKTree(test1Map);
    System.out.println("***** This is the test for BKTree with test set 2");
    testBKTree(test2Map);


    int i = 0;
  }

  private static void testBKTree(Map<String, String> testMap) {
    /********** Spell check implemented with BK Tree and Damerau Levenshitein algorithm ***********/
    DistanceAlgo<String> distanceAlgo = new DamerauLevenshteinAlgorithm<String>();
    BKTree<String> bkTree = new BKTree<String>(distanceAlgo, termFrequency);
    bkTree.addAll(dictionary.keySet());
    bkTree.addMisspellDataSet(misspellDataSet);

    int correctCount = 0;
    int notFoundCount = 0;
    int totalCount = testMap.size();

    for (String misspellWord : testMap.keySet()) {
      Optional<String> correctWord = bkTree.getMostPossibleElement(misspellWord);
      if (correctWord.isPresent()) {
        if (testMap.get(misspellWord).equals(correctWord.get())) {
          correctCount++;
        }
      } else {
        notFoundCount++;
      }
    }

    double correctPercentage = (double) correctCount / (double) totalCount;
    double notFoundPercentage = (double) notFoundCount / (double) totalCount;

    String correctPercentageStr = String.format("Correct rate is %,.3f.", correctPercentage);
    String notFoundPercentageStr = String.format("Correct rate is %,.3f.", notFoundPercentage);

    System.out.println(correctPercentageStr);
    System.out.println(notFoundPercentageStr);
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
