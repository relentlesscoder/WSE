package edu.nyu.cs.cs2580.spellCheck;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.Vector;

public class CorrectedQuery {
  @SerializedName("isCorrect")
  private boolean isSpellCorrect;

  @SerializedName("query")
  private String query;

  public CorrectedQuery(boolean isSpellCorrect, String query){
    this.isSpellCorrect = isSpellCorrect;

    this.query = query;
  }

  public boolean isSpellCorrect() {
    return isSpellCorrect;
  }

  public Vector<String> getQuery() {
    Vector<String> vector = new Vector<String>();
    String[] terms = query.split(" ");
    vector.addAll(Arrays.asList(terms));
    return vector;
  }
}
