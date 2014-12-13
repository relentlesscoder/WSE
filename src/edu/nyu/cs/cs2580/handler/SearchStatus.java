package edu.nyu.cs.cs2580.handler;

import com.google.gson.annotations.SerializedName;

/**
 * Created by wei shuai on 12/1/2014.
 */
public class SearchStatus {

    @SerializedName("StatusCode")
    private int _statusCode;

    @SerializedName("Message")
    private String _message;

    public SearchStatus(int statusCode, String message){
        _statusCode = statusCode;
        _message = message;
    }
}
