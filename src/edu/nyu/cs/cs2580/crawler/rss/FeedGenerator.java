package edu.nyu.cs.cs2580.crawler.rss;

import com.google.gson.Gson;
import edu.nyu.cs.cs2580.Utils.WriteFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by tanis on 11/20/14.
 */
public class FeedGenerator extends TimerTask {
  @Override
  public void run() {
    Date date = Calendar.getInstance().getTime();
    long time = Calendar.getInstance().getTime().getTime();
    long start = System.currentTimeMillis();
    try {
      String outputData = "data/data_"+time+".json";
      String outputFeed = "feed/feedMsg_"+time+".json";
      String urlSetPath = "urlSet.json";

      String inputPaths = "src/rss_sources";
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(inputPaths))));
      String s;
      StringBuilder feedSB = new StringBuilder();
      StringBuilder dataSB = new StringBuilder();
      Gson gson = new Gson();
      int msgCount = 0;
      int feedCount = 0;
      String source = "";
      int round = 0;

      Set<String> urlSet = gson.fromJson(new InputStreamReader(new FileInputStream(new File("result/"+urlSetPath))),Set.class);
      if (urlSet == null) {
        urlSet = new HashSet<String>();
      }


      while ((s = in.readLine()) != null) {
        if (s.matches("\\#.*")) {
          source = s.substring(2);
          continue;
        }
        if (s.matches("\\s*") || s.matches("\\/\\/.*")) continue;
        System.out.println(s);
        RSSFeedParser parser = new RSSFeedParser(s);
        Feed feed = parser.readFeed();
        if (feed == null){
          continue;
        }else {
          feed.setPublisher(source);
        }
        feedCount++;
        for (FeedMessage message : feed.getMessages()) {
          String url = message.link;
          if (message.pubDate.equals("")||source.equals("Xinhua")){
            DateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
            message.setPubDate(df.format(date));
          }
          if (urlSet.contains(url)){
            continue;
          }else {
            urlSet.add(url);
          }
        }
        String json = gson.toJson(feed);
        feedSB.append(json).append('\n');

        if (feedCount==100){
          round ++;
          feedCount = 0;
          WriteFile.WriteToFile(feedSB.substring(0,feedSB.length()-1), outputFeed, true);
          feedSB.setLength(0);
          System.out.println("Written in "+outputFeed);
          if (dataSB.length()>0){
            WriteFile.WriteToFile(dataSB.substring(0,dataSB.length()-1), outputData, true);
            dataSB.setLength(0);
            System.out.println("Written in "+outputData);
          }
        }
      }
      System.out.println("Total "+(round*100+feedCount)+" feeds.");
      in.close();

      WriteFile.WriteToFile(feedSB.substring(0,feedSB.length()-1), outputFeed, true);
      if (dataSB.length()>0){
        WriteFile.WriteToFile(dataSB.substring(0,dataSB.length()-1), outputData, true);
        dataSB.setLength(0);
        System.out.println("Written in "+outputData);
      }

      WriteFile.WriteToFile(gson.toJson(urlSet), urlSetPath, false);

    }catch (Exception e){
      System.err.println(e.toString());
    }
    long elapsedTime = System.currentTimeMillis()-start;
    int min = (int)elapsedTime/(60*1000);
    int sec = (int)(elapsedTime-min*(60*1000))/1000;
    System.out.println("Started crawling at: "+ date);
    System.out.println("Total time: "+min+" min "+sec+" sec.");
  }
}

class MainApplication {
  public static void main(String[] args) {
    Timer timer = new Timer();
    Calendar date = Calendar.getInstance();
    date.set(
            Calendar.DAY_OF_WEEK,
            Calendar.SUNDAY
    );
    date.set(Calendar.HOUR, 0);
    date.set(Calendar.MINUTE, 0);
    date.set(Calendar.SECOND, 0);
    date.set(Calendar.MILLISECOND, 0);
    // Schedule to run every Sunday in midnight
    timer.schedule(
            new FeedGenerator(),
            date.getTime(),
            1000 * 60 * 60 * 2
    );
  }//Main method ends
}//MainApplication ends
