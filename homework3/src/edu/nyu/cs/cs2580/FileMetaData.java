package edu.nyu.cs.cs2580;

/**
 * Created by Wei Shuai on 11/20/2014.
 */
public class FileMetaData {

    private boolean _isRedirectPage = false;

    private String _redirectUrl = null;

    private int _docId = 0;

    public void setIsRedirectPage(boolean isRedirectPage){
        _isRedirectPage = isRedirectPage;
    }

    public boolean getIsRedirectPage(){
        return _isRedirectPage;
    }

    public void setRedirectUrl(String redirectUrl){
       _redirectUrl = redirectUrl;
    }

    public String getRedirectUrl(){
        return _redirectUrl;
    }

    public void setDocId(int docId){
        _docId = docId;
    }

    public int getDocId(){
        return _docId;
    }
}
