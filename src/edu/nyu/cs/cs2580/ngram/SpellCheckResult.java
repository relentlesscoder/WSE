package edu.nyu.cs.cs2580.ngram;

import java.util.ArrayList;
import java.util.HashMap;

public class SpellCheckResult {
  private HashMap<String, ArrayList<String>> results;

  public SpellCheckResult(HashMap<String, ArrayList<String>> results){
    this.results = results;
  }

  public HashMap<String, ArrayList<String>> getResults(){
    return results;
  }
}
