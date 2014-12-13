package edu.nyu.cs.cs2580.crawler.rss;

/**
 * Created by tanis on 11/20/14.
 */

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class RSSFeedParser {
  static final String TITLE = "title";
  static final String DESCRIPTION = "description";
  static final String CHANNEL = "channel";
  static final String LANGUAGE = "language";
  static final String COPYRIGHT = "copyright";
  static final String LINK = "link";
  static final String ALINK = "alink";
  static final String AUTHOR = "author";
  static final String ITEM = "item";
  static final String PUB_DATE = "pubDate";
  static final String GUID = "guid";

  final URL url;

  public RSSFeedParser(String feedUrl) {
    try {
      this.url = new URL(feedUrl);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public Feed readFeed() {
    Feed feed = null;
    try {
      boolean isFeedHeader = true;
      // Set header values intial to the empty string
      String description = "";
      String title = "";
      String link = "";
      String language = "";
      String copyright = "";
      String author = "";
      String pubdate = "";
      String guid = "";

      // First create a new XMLInputFactory
      XMLInputFactory inputFactory = XMLInputFactory.newInstance();
      // Setup a new eventReader
      InputStream in = read();
      if (in==null) return null;
      XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
      // read the XML document
      while (eventReader.hasNext()) {
        XMLEvent event = eventReader.nextEvent();
        if (event.isStartElement()) {
          String localPart = event.asStartElement().getName().getLocalPart();
          String namespace = event.asStartElement().getName().getNamespaceURI();
          switch (localPart) {
            case ITEM:
              if (isFeedHeader) {
                isFeedHeader = false;
                feed = new Feed(url.toString(), title, link, description, language,
                        copyright, pubdate);
              }
//              event = eventReader.nextEvent();
              break;
            case TITLE:
              title = getCharacterData(event, eventReader);
              break;
            case DESCRIPTION:
              if (namespace.equals("")){
                description = getCharacterData(event, eventReader);
              }
              break;
            case ALINK:
            case LINK:
              link = getCharacterData(event, eventReader);
              break;
            case GUID:
              guid = getCharacterData(event, eventReader);
              break;
            case LANGUAGE:
              language = getCharacterData(event, eventReader);
              break;
            case AUTHOR:
              author = getCharacterData(event, eventReader);
              break;
            case PUB_DATE:
              pubdate = getCharacterData(event, eventReader);
              break;
            case COPYRIGHT:
              copyright = getCharacterData(event, eventReader);
              break;
          }
        } else if (event.isEndElement()) {
          if (event.asEndElement().getName().getLocalPart() == (ITEM)) {
            FeedMessage message = new FeedMessage();
            message.setAuthor(author);
            message.setDescription(description);
            message.setGuid(guid);
            message.setLink(link);
            message.setTitle(title);
            message.setPubDate(pubdate);
            feed.getMessages().add(message);
            event = eventReader.nextEvent();
            continue;
          }
        }
      }
    } catch (XMLStreamException e) {
      System.err.println(e.toString());
    }
    return feed;
  }

  private String getCharacterData(XMLEvent event, XMLEventReader eventReader)
          throws XMLStreamException {
    String result = "";
//    event = eventReader.nextEvent();
//    if (event instanceof Characters) {
//      result = event.asCharacters().getData();
//    }
    Document doc = Jsoup.parseBodyFragment(eventReader.getElementText());
    result = doc.body().text();
    return result;
  }

  private InputStream read() {
    try {
      return url.openStream();
    } catch (IOException e) {
//      throw new RuntimeException(e);
      System.err.println(e.toString());
      return null;
    }
  }
}