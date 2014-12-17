package edu.nyu.cs.cs2580.spellCheck;

import edu.nyu.cs.cs2580.query.Query;

import java.io.File;
import java.io.IOException;

/**
 * Created by Wei on 12/17/2014.
 */
public abstract class SpellChecker {

  public abstract SpellCheckResult getSpellCheckResults(Query query);

  public abstract void addDictionary(File file) throws IOException;
}
