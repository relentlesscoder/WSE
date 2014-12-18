package edu.nyu.cs.cs2580.preprocess;

import com.google.gson.Gson;
import edu.nyu.cs.cs2580.crawler.News;
import edu.nyu.cs.cs2580.utils.WriteFile;
import edu.nyu.cs.cs2580.crawler.rss.Feed;
import edu.nyu.cs.cs2580.crawler.rss.FeedMessage;

import java.io.*;
import java.util.*;

/**
 * Created by tanis on 12/8/14.
 */
public class FilterFeedMsg {

  public static int filter(File inputPath, String outputPath) throws FileNotFoundException {
    int round = 0, count = 0;
    int dupCount = 0, idCount = 0;
    Boolean finish = false;

    String urlSetPath = "inter/urlSet.json";

    String s;
    Gson gson = new Gson();
    StringBuilder sb = new StringBuilder();
    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath)));

    Set<String> urlSet = gson.fromJson(new InputStreamReader(new FileInputStream(new File("data/news/"+urlSetPath))),Set.class);
    if (urlSet == null) {
      urlSet = new HashSet<String>();
    }
    Date max = FilePreprocess.start;
    while (!finish) {
      try {
        while ((s = in.readLine()) != null) {
          Feed feed = gson.fromJson(s, Feed.class);
          String publisher = feed.getPublisher();
          count++;
          for (FeedMessage message : feed.getMessages()){
            String url = message.getLink();
            Date time = FilePreprocess.toData(message.getPubDate());
            if (!urlSet.contains(url)&&time.compareTo(FilePreprocess.dates[0])>=0){
              if (time.compareTo(max)>0){
                max = time;
              }
              idCount ++;
              urlSet.add(url);
              message.setPublisher(publisher);
              News news = new News(message);
//              System.out.println(message.getPubDate());
              String json = gson.toJson(news);
              sb.append(json).append('\n');
            }else{
              dupCount ++;
//              System.out.println("Duplicated: "+ url);
              continue;
            }
          }

          if (count == 100&&sb.length()>0) {
            round++;
            count = 0;
            WriteFile.WriteToFile(sb.toString(), outputPath, true);
            sb.setLength(0);
          }
        }
        finish = true;
        in.close();
        System.out.println(max);
        if (sb.length()>0){
          WriteFile.WriteToFile(sb.toString(), outputPath, true);
        }
        System.out.println("Written in " + outputPath);
        System.out.println("Identical:  "+idCount+", Duplicate: "+dupCount);
        WriteFile.WriteToFile(gson.toJson(urlSet), urlSetPath, false);

      } catch (Exception e) {
        System.err.println("Corpus line " + (round * 100 + count + 1));
        System.err.println(e.toString());
      }
    }
    return idCount;
  }
}
