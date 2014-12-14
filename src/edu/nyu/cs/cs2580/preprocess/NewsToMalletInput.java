package edu.nyu.cs.cs2580.preprocess;

import com.google.gson.Gson;
import edu.nyu.cs.cs2580.crawler.News;
import edu.nyu.cs.cs2580.utils.WriteFile;


import java.io.*;
import java.util.Date;

/**
 * Created by tanis on 12/14/14.
 */
public class NewsToMalletInput {
  public static void main (String[] args) throws IOException {
    long start = System.currentTimeMillis();

    String outputPath = "inter/news.txt";

    int newsCount = 0;
    String myDirectoryPath = "data/news/feed";
    File dir = new File(myDirectoryPath);
    FilenameFilter filenameFilter = new FilenameFilter() {
      @Override
      public boolean accept(File file, String name) {
        return !name.startsWith(".");
      }
    };
    File[] directoryListing = dir.listFiles(filenameFilter);
    for (File input : directoryListing){
      System.out.println("Process file: "+input.toString());
      newsCount+=FilterFeedMsg.filter(input,outputPath);
    }
    System.out.println("Total news: " + newsCount + " entry.");

    String malletInputPath = "inter/malletInput.txt";

    covertToMalletInput(outputPath,malletInputPath);

    long elapsedTime = System.currentTimeMillis() - start;
    int min = (int) elapsedTime / (60 * 1000);
    int sec = (int) (elapsedTime - min * (60 * 1000)) / 1000;
    System.out.println("Total time: " + min + " min " + sec + " sec.");
  }

  public static void covertToMalletInput(String inputPath, String outputPath) throws IOException {
    Gson gson = new Gson();
    StringBuilder sb = new StringBuilder();

    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("data/news/"+inputPath)));
    String s;
    int count = 0;

    while ((s = in.readLine()) != null) {
      News news = gson.fromJson(s,News.class);
//      sb.append((count++)+" X "+news.getContent()).append('\n');
      sb.append((count++)+" X "+news.getRawContent()).append('\n');
    }
    WriteFile.WriteToFile(sb.toString(), outputPath, false);
  }

}
