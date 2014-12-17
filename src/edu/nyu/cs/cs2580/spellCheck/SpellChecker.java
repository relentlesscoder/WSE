package edu.nyu.cs.cs2580.spellCheck;

import edu.nyu.cs.cs2580.query.Query;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface SpellChecker {
  /**
   * Return a query string where all misspelled words are replaced with most possible correct word.
   */
  public CorrectedQuery getCorrectedQuery(Query query);

  public String getMostPossibleCorrectWord(String term);

  public List<String> getMostPossibleCorrectWord(String term, int size);
}
