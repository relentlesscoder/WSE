package edu.nyu.cs.cs2580.spellCheck.BKTree;

import edu.nyu.cs.cs2580.SearchEngine;
import edu.nyu.cs.cs2580.query.Query;
import edu.nyu.cs.cs2580.spellCheck.SpellCheckCorrection;
import edu.nyu.cs.cs2580.spellCheck.SpellCheckResult;
import edu.nyu.cs.cs2580.spellCheck.SpellChecker;
import com.google.common.base.Optional;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Wei on 12/17/2014.
 */
public class BKTreeSpellChecker extends SpellChecker {

  private BKTree<String> _bkTree;

  public BKTreeSpellChecker(DistanceAlgo distanceAlgo){
    _bkTree = new BKTree<String>(distanceAlgo);
  }

  @Override
  public SpellCheckResult getSpellCheckResults(Query query) {

    if(query == null){
      return null;
    }

    ArrayList<SpellCheckCorrection> results = new ArrayList<>();

    for(String term : query._tokens){
      Optional<String> suggestions = _bkTree.getMostPossibleElement(term);
      String spellCorrection = (suggestions.isPresent()) ? suggestions.get() : "";
      boolean isCorrect = (spellCorrection.equals(term));
      results.add(new SpellCheckCorrection(isCorrect, spellCorrection));
    }

    return new SpellCheckResult(results);
  }

  @Override
  public void addDictionary(File file) throws IOException {
    _bkTree.addDictionary(file);
  }

}
