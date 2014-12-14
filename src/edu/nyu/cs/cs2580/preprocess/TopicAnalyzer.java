package edu.nyu.cs.cs2580.preprocess;

import com.google.gson.Gson;
import edu.nyu.cs.cs2580.crawler.News;
import edu.nyu.cs.cs2580.utils.WriteFile;

import java.io.*;
import java.util.*;

/**
 * Created by tanis on 11/14/14.
 */
public class TopicAnalyzer {
  private static int topicNum;
  @SuppressWarnings ( "deprecation" )
  public static final Date[] dates = {
          new Date (114,10,29),
          new Date (114,11,1),
          new Date (114,11,2),
          new Date (114,11,3),
          new Date (114,11,4),
          new Date (114,11,5),
          new Date (114,11,6),
          new Date (114,11,7),
          new Date (114,11,8),
          new Date (114,11,9),
          new Date (114,11,10),
          new Date (114,11,11),
          new Date (114,11,12),
          new Date (114,11,13),
          new Date (114,11,14)
  };
  private static Map<Integer, Topic> topics;

  public static void main(String[] args) throws IOException {
    long start = System.currentTimeMillis();

    File dtFile = new File("data/news/topic/doc_topics");
    File newsFile = new File("data/news/inter/news.txt");
    File topicKey = new File("data/news/topic/topic_key");

    topicNum = FilePreprocess.countLines(topicKey);
    topics= categorizeDocs(dtFile);
    integrateTime(newsFile);
    addTerms(topicKey);
    String timeDis = "topic/timeDistribution.txt";
    String numDis = "topic/numDistribution.txt";
    outputDis(timeDis,numDis);

    Gson gson = new Gson();
    String json = gson.toJson(topics);

    long elapsedTime = System.currentTimeMillis() - start;
    int min = (int) elapsedTime / (60 * 1000);
    int sec = (int) (elapsedTime - min * (60 * 1000)) / 1000;
    System.out.println("Total time: " + min + " min " + sec + " sec.");
  }

  public static void integrateTime(File file) throws IOException {
    Gson gson = new Gson();
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
    String s;
    Integer newsID = 0;
    Map<Integer, Date> docPubDate = new HashMap<Integer, Date>();

    while ((s = br.readLine()) != null) {
      News news = gson.fromJson(s,News.class);
      Date time = news.getPubDate();
      docPubDate.put(newsID++,time);
    }
    br.close();
    for (Topic topic : topics.values()){
      for (Integer docID : topic.getDocList()){
        Date time = docPubDate.get(docID);
//        System.out.println(time);
//        System.out.println(dates[1]);
        if (time.compareTo(dates[0])<0){
          topic.addToTimeSlots(0);
        }else{
          for (int i=1; i<dates.length; i++){
            if (time.compareTo(dates[i])<0){
              topic.addToTimeSlots(i-1);
              break;
            }
          }
        }
      }
    }
    System.out.println("Filled up topics' time slots.");
  }

  public static Map<Integer, Topic> categorizeDocs(File dtFile) throws IOException{
    Map<Integer, Topic> topicDocs = new TreeMap<Integer, Topic>();

    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dtFile)));
    String line;

    while ((line = br.readLine()) != null) {
      if (line.matches("\\#.*")) {
        continue;
      }
      String[] str = line.split("\t");
      int docID = Integer.parseInt(str[1]);
      int topicID = Integer.parseInt(str[2]);

      if (topicDocs.containsKey(topicID)){
        List<Integer> docs = topicDocs.get(topicID).getDocList();
        docs.add(docID);
      }else {
        Topic topic = new Topic(topicID);
        topic.getDocList().add(docID);
        topicDocs.put(topicID,topic);
      }
    }
    br.close();
    System.out.println("Read in topics.");
    return topicDocs;
  }

  private static void addTerms(File tokeKey) throws IOException {
    Map<Integer, Topic> topicDocs = new TreeMap<Integer, Topic>();

    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tokeKey)));
    String line;

    while ((line = br.readLine()) != null) {
      String[] str = line.split("\t");
      Integer topicID = Integer.parseInt(str[0]);
      Topic topic = topics.get(topicID);
      String[] termStr = str[2].split(" ");
      List<String> terms = topic.getTerms();
      for (String term : termStr){
        terms.add(term);
      }
    }
    br.close();
    System.out.println("Added top keys into topics.");
  }

  public static void outputDis(String timeFile, String numFile) throws IOException {
    StringBuilder sb = new StringBuilder();

    int[] topicDocsCount = new int[topicNum];

    for (Topic topic : topics.values()){
      topicDocsCount[topic.getTopicID()] = topic.getDocList().size();
      sb.append("{visible:false,name:'topic-").append(topic.getTopicID());
      List<String> terms = topic.getTerms();
      for (int i=0; i<5; i++){
        sb.append(" ").append(terms.get(i));
      }
      sb.append("',data:[");
      int[] timeSlots = topic.getTimeSlots();
      for (int i=0; i<timeSlots.length; i++){
        sb.append("[").append(i).append(",").append(timeSlots[i]).append("],");
      }
      sb.append("]},");
    }
    WriteFile.WriteToFile(sb.toString(),timeFile, false);
    System.out.println("Written result in data/news/"+timeFile+".");
    sb.setLength(0);
    for (int i=0; i<topicDocsCount.length; i++){
      sb.append(topicDocsCount[i]).append(", ");
    }
    WriteFile.WriteToFile(sb.substring(0,sb.length()-2),numFile, false);
    System.out.println("Written result in data/news/"+numFile+".");
  }
}
