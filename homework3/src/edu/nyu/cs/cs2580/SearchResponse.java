package edu.nyu.cs.cs2580;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by wei shuai on 12/1/2014.
 */
public class SearchResponse {

    @SerializedName("QueryText")
    private String _queryText;

    @SerializedName("Results")
    private ArrayList<SearchResult> _results;

    @SerializedName("Status")
    private SearchStatus _status;

    public SearchResponse(String queryText, ArrayList<SearchResult> results, SearchStatus status){
        _queryText = queryText;
        _results = results;
        _status = status;
    }
}
