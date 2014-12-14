package edu.nyu.cs.cs2580.crawler;

/**
 * Created by tanis on 12/14/14.
 */

import edu.nyu.cs.cs2580.crawler.rss.FeedMessage;
import edu.nyu.cs.cs2580.preprocess.FilePreprocess;
import edu.nyu.cs.cs2580.tokenizer.Tokenizer;

import java.io.StringReader;
import java.util.Date;

public class News {
  private String source;
  private Date pubDate;
  private String title;
  private String description;
  private String fullDoc;
  private String content;
  private String link;

  public News() {
    content = null;
  };

  public News(FeedMessage message){
    this.source = message.getPublisher();
    this.pubDate = FilePreprocess.toData(message.getPubDate());
    this.title = message.getTitle();
    this.description = message.getDescription();
    this.link = message.getLink();
    this.content = null;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }


  public String getContent() {
    if (content == null) {
      return rawContentToContent();
    } else {
      return content;
    }
  }

  public String getSource() {
    return source;
  }

  public Date getPubDate() {
    return pubDate;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public void setPubDate(Date time) {
    this.pubDate = time;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public String getRawContent(){
    return this.title + " "+ this.description;
  }

  public String rawContentToContent() {
    String rawContent = this.title + " "+ this.description;
    StringBuilder sb = new StringBuilder();
    Tokenizer tokenizer = new Tokenizer(new StringReader(rawContent));

    // Add all terms appearing in content
    while (tokenizer.hasNext()) {
      String term = Tokenizer.lowercaseFilter(tokenizer.getText());
      // Delete the stop words for normal query terms
      term = Tokenizer.stopwordFilter(term);
      if (term != null) {
//        term = Tokenizer.krovetzStemmerFilter(term);
        sb.append(term).append(" ");
      }
    }
    return sb.toString();
  }
}