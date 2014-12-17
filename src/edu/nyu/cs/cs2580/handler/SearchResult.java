package edu.nyu.cs.cs2580.handler;

import com.google.gson.annotations.SerializedName;

/**
 * Created by wei shuai on 12/1/2014.
 */
public class SearchResult {
    @SerializedName("title")
    private String _title;

    @SerializedName("url")
    private String _url;

    @SerializedName("score")
    private double _score;

    @SerializedName("pageRank")
    private double _pageRank;

    @SerializedName("numViews")
    private int _numView;

    public SearchResult(String title, String url, double score, double pageRank, int numViews){
        _title = title;
        _url = url;
        _score = score;
        _pageRank = pageRank;
        _numView = numViews;
    }
}
