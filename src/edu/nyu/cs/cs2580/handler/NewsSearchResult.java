package edu.nyu.cs.cs2580.handler;

import com.google.gson.annotations.SerializedName;

/**
 * Created by wei shuai on 12/1/2014.
 */
public class NewsSearchResult {
  @SerializedName("Title")
  private String _title;

  @SerializedName("Url")
  private String _url;

  @SerializedName("Score")
  private double _score;

  public NewsSearchResult(String title, String url, double score){
    _title = title;
    _url = url;
    _score = score;
  }
}
