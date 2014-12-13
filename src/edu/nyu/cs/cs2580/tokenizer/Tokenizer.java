package edu.nyu.cs.cs2580.tokenizer;

import edu.nyu.cs.cs2580.KStemmer.KStemmer;
import edu.nyu.cs.cs2580.Snowball.SnowballStemmer;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Tokenizer {

  /**
   * constants used for JFlex analyzer
   */
  public static final int ALPHANUM = 1;
  public static final int NUM = 2;
  public static final int SOUTHEAST_ASIAN = 3;
  public static final int IDEOGRAPHIC = 4;
  public static final int HIRAGANA = 5;
  public static final int KATAKANA = 6;
  public static final int HANGUL = 7;

  public static final HashSet<String> STOP_WORDS_SET;

  static {
    final List<String> stopWords = Arrays.asList("a", "about", "above", "after",
        "again", "against", "all", "am", "an", "and", "any", "are", "aren't",
        "as", "at", "be", "because", "been", "before", "being", "below",
        "between", "both", "but", "by", "can't", "cannot", "could", "couldn't",
        "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down",
        "during", "each", "few", "for", "from", "further", "had", "hadn't", "has",
        "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her",
        "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's",
        "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it",
        "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my",
        "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or",
        "other", "ought", "our", "ours	ourselves", "out", "over", "own", "same",
        "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so",
        "some", "such", "than", "that", "that's", "the", "their", "theirs", "them",
        "themselves", "then", "there", "there's", "these", "they", "they'd", "they'll",
        "they're", "they've", "this", "those", "through", "to", "too", "under", "until",
        "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were",
        "weren't", "what", "what's", "when", "when's", "where", "where's", "which",
        "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would",
        "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours",
        "yourself", "yourselves");
    STOP_WORDS_SET = new HashSet<String>(stopWords);
  }

  private DefaultScanner scanner;

  public Tokenizer(Reader reader) {
    this.scanner = new DefaultScanner(reader);
  }

  /**
   * Stemming text
   *
   * @param input    Original text
   * @param language Language type, e.g. english
   * @return Stemmed text
   */
  public static String porterStemmerFilter(String input, String language) {
    String output = input;

    try {
      Class<? extends SnowballStemmer> stemClass = Class.forName(
          "edu.nyu.cs.cs2580.Snowball." + language + "Stemmer").asSubclass(
          SnowballStemmer.class);
      SnowballStemmer stemmer = stemClass.newInstance();

      stemmer.setCurrent(input);
      if (stemmer.stem()) {
        output = stemmer.getCurrent();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return output;
  }

  /**
   * Stemming text
   *
   * @param input Original text
   * @return Stemmed text
   */
  public static String krovetzStemmerFilter(String input) {
    if (input == null || input.isEmpty()) {
      return null;
    }
    KStemmer kStemmer = new KStemmer();
    return kStemmer.stem(input);
  }

  public static String lowercaseFilter(String input) {
    if (input == null || input.isEmpty()) {
      return null;
    }
    return (input != null && !input.isEmpty()) ? input.toLowerCase() : null;
  }

  public static String stopwordFilter(String input) {
    if (input == null || input.isEmpty()) {
      return null;
    }
    if (STOP_WORDS_SET.contains(input)) {
      return null;
    }
    return input;
  }

  public static String stripSingleCharacterFilter(String input) {
    if (input == null || input.isEmpty()) {
      return null;
    }
    if (input.length() == 1) {
      char c = input.charAt(0);
      if ((c >= 'b' && c <= 'z')) {
        return null;
      }
    }
    return input;
  }

  public String getText() {
    return scanner.getText();
  }

  public boolean hasNext() {
    boolean hasNext = false;

    try {
      int tokenType = scanner.getNextToken();
      if (tokenType != DefaultScanner.YYEOF) {
        hasNext = true;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return hasNext;
  }
}
