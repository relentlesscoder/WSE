package edu.nyu.cs.cs2580.evaluator;

import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.io.Files;
import edu.nyu.cs.cs2580.ngram.NGramSpellChecker;
import edu.nyu.cs.cs2580.rankers.IndexerConstant;
import edu.nyu.cs.cs2580.spellCheck.BKTree.BKTree;
import edu.nyu.cs.cs2580.spellCheck.BKTree.DamerauLevenshteinAlgorithm;
import edu.nyu.cs.cs2580.spellCheck.BKTree.DistanceAlgo;
import edu.nyu.cs.cs2580.spellCheck.MisspellDataSet;

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
  // Common misspell data corpora file, reference: http://www.dcs.bbk.ac.uk/~ROGER/corpora.html
  private static final File ASPELL_FILE = new File("data/spellCheckTestData/aspell.dat");
  private static final File MISSP_FILE = new File("data/spellCheckTestData/missp.dat");
  private static final File WIKIPEDIA_FILE = new File("data/spellCheckTestData/wikipedia.dat");

  private static final File DICTIONARY_FILE = new File("data/spellCheckTestData/words");

  // Dictionary of the test corpus
  static BiMap<String, Integer> dictionary = HashBiMap.create();
  // Term frequency of the test corpus
  static Multiset<String> termFrequency = HashMultiset.create();
  // The misspell data set...
  static MisspellDataSet misspellDataSet = new MisspellDataSet();

  static HashMap<String, Integer> termFrequencyForNGram = new HashMap<>();

  public static void main(String[] args) throws IOException {
    // Test corpus file
    File testCorpusFile = new File("data/spellCheckTestData/big.text");

    // Original files in order to construct the map
    File test1File = new File("data/spellCheckTestData/test1.text");
    File test2File = new File("data/spellCheckTestData/test2.text");

    misspellDataSet.addData(ASPELL_FILE);
    misspellDataSet.addData(MISSP_FILE);
    misspellDataSet.addData(WIKIPEDIA_FILE);

    // Two sets of test map
    // Key: misspelled word
    // Value: correct word
    Map<String, String> test1Map = new HashMap<String, String>();
    Map<String, String> test2Map = new HashMap<String, String>();

    constructDictionary(testCorpusFile);

    test1Map = constructTestMap(test1File);
    test2Map = constructTestMap(test2File);

    /*********************************************************************************
     * This is the start test for BKTree & Damerau Levenshitein
     *********************************************************************************/

    System.out.println("                   Test set 1");
    testBKTree(test1Map);
    System.out.println("------------------------------------------------");
    System.out.println("                   Test set 2");
    testBKTree(test2Map);

    /*********************************************************************************
     * This is the end test for BKTree & Damerau Levenshitein
     *********************************************************************************/

    /*********************************************************************************
     * This is the start test for NGram & Damerau Levenshitein
     *********************************************************************************/

    System.out.println("                   Test set 1");
    testNGram(test1Map);
    System.out.println("------------------------------------------------");
    System.out.println("                   Test set 2");
    testNGram(test2Map);

    /*********************************************************************************
     * This is the end test for NGram & Damerau Levenshitein
     *********************************************************************************/
  }

  private static void testNGram(Map<String, String> testMap){
    try
    {
      DistanceAlgo<String> distanceAlgo = new DamerauLevenshteinAlgorithm<String>();
      NGramSpellChecker spellChecker = new NGramSpellChecker(dictionary, termFrequencyForNGram, distanceAlgo, misspellDataSet);
      spellChecker.buildIndex();
      System.out.println("################################################");
      System.out.println("# This is the NGram...");
      System.out.println("################################################");
      System.out.println("## Distance 1");
      testNGramWithDistance(spellChecker, testMap, 1);
      System.out.println("## Distance 2");
      testNGramWithDistance(spellChecker, testMap, 2);
      System.out.println("## System default");
      testNGramWithDistance(spellChecker, testMap, -1);
      System.out.println("################################################");
      System.out.println("# This is the NGram...");
      System.out.println("################################################");
      System.out.println("## Distance 1 - suggested list contains correct word");
      testNGramWithDistanceContain(spellChecker, testMap, 1);
      System.out.println("## Distance 2 - suggested list contains correct word");
      testNGramWithDistanceContain(spellChecker, testMap, 2);
      System.out.println("## System default - suggested list contains correct word");
      testNGramWithDistanceContain(spellChecker, testMap, -1);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * ******************************************************************************
   * This is the start test for BKTree & Damerau Levenshitein
   * *******************************************************************************
   */

  private static void testBKTree(Map<String, String> testMap) throws IOException {
    /********** Spell check implemented with BK Tree and Damerau Levenshitein algorithm ***********/
    DistanceAlgo<String> distanceAlgo = new DamerauLevenshteinAlgorithm<String>();
    BKTree<String> bkTree = new BKTree<String>(distanceAlgo, termFrequency);
    bkTree.addDictionary(DICTIONARY_FILE);
//    bkTree.addAll(dictionary.keySet());

    System.out.println("################################################");
    System.out.println("# This is the BKTree test without error model...");
    System.out.println("################################################");
    System.out.println("## Distance 1");
    testBKTreeWithDistance(bkTree, testMap, 1);
    System.out.println("## Distance 2");
    testBKTreeWithDistance(bkTree, testMap, 2);
    System.out.println("## Distance 1 and 2");
    testBKTreeWithDistanceOneAndTwo(bkTree, testMap);

    System.out.println("################################################");
    System.out.println("# This is the BKTree test with error model...");
    System.out.println("################################################");
    bkTree.addMisspellDataSet(misspellDataSet);
    System.out.println("## Distance 1");
    testBKTreeWithDistance(bkTree, testMap, 1);
    System.out.println("## Distance 2");
    testBKTreeWithDistance(bkTree, testMap, 2);
    System.out.println("## Distance 1 and 2");
    testBKTreeWithDistanceOneAndTwo(bkTree, testMap);
  }

  private static void testBKTreeWithDistance(BKTree<String> bkTree, Map<String, String> testMap, int expectedDistance) {
    int correctCount = 0;
    int notFoundCount = 0;
    int totalCount = testMap.size();
    double startTimeStamp = System.currentTimeMillis();

    for (String misspellWord : testMap.keySet()) {
      Optional<String> correctWord = bkTree.getMostPossibleElementsForDistance(misspellWord, expectedDistance);
      if (correctWord.isPresent()) {
        if (testMap.get(misspellWord).equals(correctWord.get())) {
          correctCount++;
        }
      } else {
        notFoundCount++;
      }
    }

    // Output
    testBKTreeOutput(correctCount, notFoundCount, totalCount, System.currentTimeMillis() - startTimeStamp);
  }

  private static void testBKTreeWithDistanceOneAndTwo(BKTree<String> bkTree, Map<String, String> testMap) {
    /********** Spell check implemented with BK Tree and Damerau Levenshitein algorithm ***********/
    bkTree.addMisspellDataSet(misspellDataSet);

    int correctCount = 0;
    int notFoundCount = 0;
    int totalCount = testMap.size();
    double startTimeStamp = System.currentTimeMillis();

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

    // Output
    testBKTreeOutput(correctCount, notFoundCount, totalCount, System.currentTimeMillis() - startTimeStamp);
  }

  // Output the correct rate and not found rate...
  private static void testBKTreeOutput(double correctCount, double notFoundCount, double totalCount, double duration) {
    double correctPercentage = (double) correctCount / (double) totalCount;
    double notFoundPercentage = (double) notFoundCount / (double) totalCount;
    double termPerSecond = totalCount / (duration / 1000);

    String correctPercentageStr = String.format("#### Correct rate is %,.3f.", correctPercentage);
    String notFoundPercentageStr = String.format("#### Not found rate is %,.3f.", notFoundPercentage);
    String termPerSecondStr = String.format("#### Process %,.3f. terms per second.", termPerSecond);

    System.out.println(correctPercentageStr);
    System.out.println(notFoundPercentageStr);
    System.out.println(termPerSecondStr);
  }

  /*********************************************************************************
   * This is the end test for BKTree & Damerau Levenshitein
   *********************************************************************************/

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
    // Update the term frequency for NGram
    if(termFrequencyForNGram.containsKey(term)){
      termFrequencyForNGram.put(term, termFrequencyForNGram.get(term) + 1);
    }
    else{
      termFrequencyForNGram.put(term, 1);
    }
    // Update the dictionary
    if (!dictionary.containsKey(term)) {
      int termId = dictionary.size();
      dictionary.put(term, termId);
    }
    // Clean the string builder
    sb.setLength(0);
  }

  private static void testNGramWithDistance(NGramSpellChecker spellChecker, Map<String, String> testMap, int expectedDistance) {
    int correctCount = 0;
    int notFoundCount = 0;
    int totalCount = testMap.size();
    double startTimeStamp = System.currentTimeMillis();

    for (String misspellWord : testMap.keySet()) {
      ArrayList<String> correctWord = spellChecker.getSuggestion(misspellWord, expectedDistance);
      if (correctWord != null && correctWord.size() > 0) {
        if (testMap.get(misspellWord).equals(correctWord.get(0))) {
          correctCount++;
        }
      } else {
        notFoundCount++;
      }
    }

    // Output
    testNGramOutput(correctCount, notFoundCount, totalCount, System.currentTimeMillis() - startTimeStamp);
  }

  private static void testNGramWithDistanceContain(NGramSpellChecker spellChecker, Map<String, String> testMap, int expectedDistance) {
    int correctCount = 0;
    int notFoundCount = 0;
    int totalCount = testMap.size();
    double startTimeStamp = System.currentTimeMillis();

    for (String misspellWord : testMap.keySet()) {
      ArrayList<String> correctWord = spellChecker.getSuggestion(misspellWord, expectedDistance);
      if (correctWord != null && correctWord.size() > 0) {
        if (correctWord.contains(testMap.get(misspellWord))) {
          correctCount++;
        }
      } else {
        notFoundCount++;
      }
    }

    // Output
    testNGramOutput(correctCount, notFoundCount, totalCount, System.currentTimeMillis() - startTimeStamp);
  }

  // Output the correct rate and not found rate...
  private static void testNGramOutput(double correctCount, double notFoundCount, double totalCount, double duration) {
    double correctPercentage = (double) correctCount / (double) totalCount;
    double notFoundPercentage = (double) notFoundCount / (double) totalCount;
    double termPerSecond = totalCount / (duration / 1000);

    String correctPercentageStr = String.format("#### Correct rate is %,.3f.", correctPercentage);
    String notFoundPercentageStr = String.format("#### Not found rate is %,.3f.", notFoundPercentage);
    String termPerSecondStr = String.format("#### Process %,.3f. terms per second.", termPerSecond);

    System.out.println(correctPercentageStr);
    System.out.println(notFoundPercentageStr);
    System.out.println(termPerSecondStr);
  }
}
