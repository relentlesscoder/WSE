package edu.nyu.cs.cs2580.handler;

import com.google.gson.annotations.SerializedName;

/**
 * Created by wei shuai on 12/1/2014.
 */
public class NewsSearchResult{
  @SerializedName("title")
  private String _title;

  @SerializedName("url")
  private String _url;

  @SerializedName("score")
  private double _score;

  @SerializedName("pubDate")
  private long _time;

  @SerializedName("source")
  private String _source;

  @SerializedName("description")
  private String _description;

  public NewsSearchResult(String title, String url, double score, long time, String source, String description){
    _title = title;
    _url = url;
    _score = score;
    _time = time;
    _source = source;
    _description = description;
  }
}
