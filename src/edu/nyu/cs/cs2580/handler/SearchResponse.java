package edu.nyu.cs.cs2580.handler;

import com.google.gson.annotations.SerializedName;
import edu.nyu.cs.cs2580.ngram.SpellCheckCorrection;
import edu.nyu.cs.cs2580.ngram.SpellCheckResult;

import java.util.ArrayList;

/**
 * Created by wei shuai on 12/1/2014.
 */
public class SearchResponse {

    @SerializedName("queryText")
    private String _queryText;

    @SerializedName("results")
    private ArrayList<SearchResult> _results;

    @SerializedName("status")
    private SearchStatus _status;

    @SerializedName("spellCheck")
    private SpellCheckResult _spellCheck;

    public SearchResponse(String queryText, ArrayList<SearchResult> results, SearchStatus status, SpellCheckResult spellCheck){
        _queryText = queryText;
        _results = results;
        _status = status;
        _spellCheck = spellCheck;
    }
}
