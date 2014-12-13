package edu.nyu.cs.cs2580.handler;

import com.google.gson.annotations.SerializedName;

/**
 * Created by youlongli on 12/12/14.
 */
public class SearchPrfResult {
  @SerializedName("suggestionQuery")
  private String suggestionQuery;

  public SearchPrfResult(String suggestionQuery){
    this.suggestionQuery = suggestionQuery;
  }
}
