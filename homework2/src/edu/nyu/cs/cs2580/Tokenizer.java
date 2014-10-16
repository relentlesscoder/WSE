package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.Reader;

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

  // TODO:add String Porter Stemming methods, etc
}
