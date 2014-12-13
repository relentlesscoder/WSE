package edu.nyu.cs.cs2580.crawler.rss;

/**
 * Created by tanis on 11/20/14.
 */
import java.util.ArrayList;
import java.util.List;

/*
 * Stores an RSS feed
 */
public class Feed {

  final String title;
  final String link;
  final String description;
  final String language;
  final String copyright;
  final String pubDate;
  final String url;
  private String publisher;


  final List<FeedMessage> entries = new ArrayList<FeedMessage>();

  public Feed(String url, String title, String link, String description, String language,
              String copyright, String pubDate) {
    this.url = url;
    this.title = title;
    this.link = link;
    this.description = description;
    this.language = language;
    this.copyright = copyright;
    this.pubDate = pubDate;
  }

  public String getPublisher() {
    return publisher;
  }

  public void setPublisher(String publisher) {
    this.publisher = publisher;
  }

  public List<FeedMessage> getMessages() {
    return entries;
  }

  public String getTitle() {
    return title;
  }

  public String getLink() {
    return link;
  }

  public String getDescription() {
    return description;
  }

  public String getLanguage() {
    return language;
  }

  public String getCopyright() {
    return copyright;
  }

  public String getPubDate() {
    return pubDate;
  }

  @Override
  public String toString() {
    return "Feed [copyright=" + copyright + ", description=" + description
            + ", language=" + language + ", link=" + link + ", pubDate="
            + pubDate + ", title=" + title + "]";
  }

}