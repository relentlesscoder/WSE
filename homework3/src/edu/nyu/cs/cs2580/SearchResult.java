package edu.nyu.cs.cs2580;

import com.google.gson.annotations.SerializedName;

/**
 * Created by wei shuai on 12/1/2014.
 */
public class SearchResult {
    @SerializedName("Title")
    private String _title;

    @SerializedName("Url")
    private String _url;

    @SerializedName("Score")
    private double _score;

    @SerializedName("PageRank")
    private double _pageRank;

    @SerializedName("NumViews")
    private int _numView;

    public SearchResult(String title, String url, double score, double pageRank, int numViews){
        _title = title;
        _url = url;
        _score = score;
        _pageRank = pageRank;
        _numView = numViews;
    }
}
