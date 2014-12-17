package edu.nyu.cs.cs2580.spellCheck;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class SpellCheckResult {

  @SerializedName("result")
  private ArrayList<SpellCheckCorrection> _results;

  public SpellCheckResult(ArrayList<SpellCheckCorrection> results){
    this._results = results;
  }
}


