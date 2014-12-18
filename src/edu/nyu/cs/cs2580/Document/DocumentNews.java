package edu.nyu.cs.cs2580.document;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by tanis on 12/8/14.
 */
public class DocumentNews extends DocumentIndexed implements Serializable{
  private static final long serialVersionUID = 1L;

  private Date time;
  private String description;
  private String source;

  public DocumentNews(int docid, Date time) {
    super(docid);
    this.time = time;
  }


  public Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }
}
