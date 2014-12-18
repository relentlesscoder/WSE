package edu.nyu.cs.cs2580.spellCheck;

import edu.nyu.cs.cs2580.query.Query;
import edu.nyu.cs.cs2580.spellCheck.BKTree.BKTree;
import edu.nyu.cs.cs2580.spellCheck.BKTree.DistanceAlgo;
import edu.nyu.cs.cs2580.spellCheck.CorrectedQuery;
import edu.nyu.cs.cs2580.spellCheck.MisspellDataSet;
import edu.nyu.cs.cs2580.spellCheck.SpellChecker;
import com.google.common.base.Optional;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class BKTreeSpellChecker implements SpellChecker {
  // Common misspell data corpora file, reference: http://www.dcs.bbk.ac.uk/~ROGER/corpora.html
  private static final File ASPELL_FILE = new File("spellCheckTestData/aspell.dat");
  private static final File MISSP_FILE = new File("spellCheckTestData/missp.dat");
  private static final File WIKIPEDIA_FILE = new File("spellCheckTestData/wikipedia.dat");

  private static final File DICTIONARY_FILE = new File("spellCheckTestData/words");

  private MisspellDataSet misspellDataSet = new MisspellDataSet();;

  private BKTree<String> bkTree;

  public BKTreeSpellChecker(DistanceAlgo distanceAlgo, Map<String, Integer> termFrequency) throws IOException {
    this.bkTree = new BKTree<String>(distanceAlgo, termFrequency);
    // Load dictionary
    this.bkTree.addDictionary(DICTIONARY_FILE);
    // Load error model sets
    this.misspellDataSet.addData(ASPELL_FILE);
    this.misspellDataSet.addData(MISSP_FILE);
    this.misspellDataSet.addData(WIKIPEDIA_FILE);
    this.bkTree.addMisspellDataSet(misspellDataSet);
  }

  @Override
  public CorrectedQuery getCorrectedQuery(Query query) {
    StringBuilder sb = new StringBuilder();
    boolean isQueryCorrect = true;
    for (String word : query._tokens) {
      if (bkTree.hasExist(word)) {
        sb.append(word).append(" ");
      } else {
        isQueryCorrect = false;
        Optional<String> correctWord = bkTree.getMostPossibleElement(word);
        if (correctWord.isPresent()) {
          sb.append(correctWord.get()).append(" ");
        } else {
          isQueryCorrect = true;
        }
      }
    }

    // Delete the last space
    sb.setLength(sb.length() - 1);
    return new CorrectedQuery(isQueryCorrect, sb.toString());
  }

  @Override
  public String getMostPossibleCorrectWord(String term) {
    Optional<String> res = bkTree.getMostPossibleElement(term);
    if (res.isPresent()) {
      return res.get();
    } else {
      return "";
    }
  }

  @Override
  public List<String> getMostPossibleCorrectWord(String term, int size) {
    return bkTree.getPossibleElementsWithOrder(term, size);
  }
}
