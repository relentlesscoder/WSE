package edu.nyu.cs.cs2580;

/**
 * Created by Wei Shuai on 11/20/2014.
 */
public class FileMetaData {
  private boolean _isRedirectPage;
  private String _redirectUrl;
  private int _docid;

  public FileMetaData(int docid) {
    _isRedirectPage = false;
    _redirectUrl = "";
    _docid = docid;
  }

  public boolean isRedirectPage() {
    return _isRedirectPage;
  }

  public void setRedirectUrl(String redirectUrl) {
    _isRedirectPage = true;
    _redirectUrl = redirectUrl;
  }

  public String getRedirectUrl() {
    return _redirectUrl;
  }

  public int getDocid() {
    return _docid;
  }
}
