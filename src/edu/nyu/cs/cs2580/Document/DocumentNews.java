package edu.nyu.cs.cs2580.Document;

import edu.nyu.cs.cs2580.Index.Indexer;

import java.util.Date;
import java.util.Vector;

/**
 * Created by tanis on 12/8/14.
 */
public class DocumentNews extends DocumentIndexed{

  private Date time;

  public DocumentNews(int docid, Indexer indexer, Date time) {
    super(docid);
    this.time = time;
  }


  public Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }

}
