package edu.nyu.cs.cs2580.crawler;

import twitter4j.*;

/**
 * Created by tanis on 10/8/14.
 */
public class RecentCralwer {
  public static void main(String args[]) {
    TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
    TweetsListener listener = new TweetsListener();
    twitterStream.addListener(listener);
//        FilterQuery fq = new FilterQuery();
//        String[] lang = { "en" };
//        fq.language(lang);
    twitterStream.sample();
  }
}


