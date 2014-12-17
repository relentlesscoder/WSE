package edu.nyu.cs.cs2580.ngram;

import com.google.gson.annotations.SerializedName;

public class SpellCheckCorrection{
  @SerializedName("isCorrect")
  private boolean _isSpellCorrect;

  @SerializedName("spellCorrection")
  private String _spellCorrection;

  public SpellCheckCorrection(boolean isSpellCorrect, String spellCorrection){
    this._isSpellCorrect = isSpellCorrect;

    this._spellCorrection = spellCorrection;
  }
}
