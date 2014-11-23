package edu.nyu.cs.cs2580;

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
  public static final int INTERNAL_LINK = 8;
  public static final int EXTERNAL_LINK = 8;

  public static final HashSet<String> STOP_WORDS_SET;

  static {
    final List<String> stopWords = Arrays.asList("a", "an", "and", "are", "as",
        "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no",
        "not", "of", "on", "or", "such", "that", "the", "their", "then",
        "there", "these", "they", "this", "to", "was", "will", "with");
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
