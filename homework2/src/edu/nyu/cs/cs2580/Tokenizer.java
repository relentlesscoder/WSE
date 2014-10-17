package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.Reader;

import edu.nyu.cs.cs2580.Snowball.SnowballStemmer;

public class Tokenizer {

  private DefaultScanner scanner;

  public Tokenizer(Reader reader) {
    this.scanner = new DefaultScanner(reader);
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

  public static String lowercaseFilter(String input) {
    return (input != null && !input.isEmpty()) ? input.toLowerCase() : "";
  }
}
