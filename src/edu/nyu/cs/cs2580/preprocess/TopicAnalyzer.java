package edu.nyu.cs.cs2580.preprocess;

import com.google.gson.Gson;
import edu.nyu.cs.cs2580.crawler.News;
import edu.nyu.cs.cs2580.utils.WriteFile;

import java.io.*;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Created by tanis on 11/14/14.
 */
public class TopicAnalyzer {
  private static int topicNum;
  private static Map<Integer, Topic> topics;
  private static double avgDocNum;
  private static int minDocNum;
  private static int maxDocNum;

  public static void main(String[] args) throws IOException {
    long start = System.currentTimeMillis();

    File dtFile = new File("data/news/topic_400/doc_topics");
    File newsFile = new File("data/news/inter/news.txt");
    File topicKey = new File("data/news/topic_400/topic_key");

    topicNum = FilePreprocess.countLines(topicKey);
    // Load Topics from document_topic file
    topics= categorizeDocs(dtFile);
    // Integrate with time slots info
    integrateTime(newsFile);
    // Adding topic keys to Topics
    addTerms(topicKey);
    String timeDis = "topic_400/timeDistribution.txt";
    String numDis = "topic_400/numDistribution.txt";
    String topicRankPath = "topic_400/topicRank.txt";
    // Output visualization data
    outputNumDis(numDis);

    Queue<Topic> scoredTopics= new PriorityQueue<Topic>(new Comparator<Topic>() {
      @Override
      public int compare(Topic o1, Topic o2) {
        return o2.getScore().compareTo(o1.getScore());
      }
    });
    for (Topic topic : topics.values()){
      computeMetrics(topic, 14);
      scoredTopics.add(topic);
    }

    outputRankedTopic(topicRankPath,timeDis,scoredTopics);

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
//        System.out.println(FilePreprocess.dates[1]);
        if (time.compareTo(FilePreprocess.dates[0])<0){
          topic.addToTimeSlots(0);
        }else{
          for (int i=1; i<FilePreprocess.dates.length; i++){
            if (time.compareTo(FilePreprocess.dates[i])<0){
              topic.addToTimeSlots(i-1);
              break;
            }
            if (i==FilePreprocess.dates.length-1){
              topic.addToTimeSlots(FilePreprocess.dates.length-2);
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

  public static void outputNumDis(String numFile) throws IOException {
    StringBuilder sb = new StringBuilder();
    int[] topicDocsCount = new int[topicNum];

    for (Topic topic : topics.values()){
      topicDocsCount[topic.getTopicID()] = topic.getDocList().size();
    }
    minDocNum = Integer.MAX_VALUE;
    maxDocNum = 0;
    for (int i=0; i<topicDocsCount.length; i++){
      int docNum = topicDocsCount[i];
      sb.append(docNum).append(", ");
      if (docNum>maxDocNum){
        maxDocNum = docNum;
      }
      if (docNum<minDocNum){
        minDocNum = docNum;
      }
    }
    int sum = IntStream.of(topicDocsCount).sum();
    avgDocNum = (double)sum/topicNum;
    WriteFile.WriteToFile(sb.substring(0,sb.length()-2),numFile, false);
    System.out.println("Written result in data/news/"+numFile+".");
  }

  public static void outputRankedTopic(String topicRankPath, String timeFile, Queue<Topic> scoredTopics){
    StringBuilder timeSB = new StringBuilder();

    StringBuilder rankSB = new StringBuilder();
    while (!scoredTopics.isEmpty()){
      Topic topic = scoredTopics.poll();
      rankSB.append(topic.getTopicID()).append(":");
      timeSB.append("{visible:false,name:'topic-").append(topic.getTopicID());
      List<String> terms = topic.getTerms();
      for (int i=0; i<5; i++){
        rankSB.append(" ").append(terms.get(i));
        timeSB.append(" ").append(terms.get(i));
      }
      rankSB.append('\t').append(topic.getScore()).append('\n');
      timeSB.append("',data:[");
      int[] timeSlots = topic.getTimeSlots();
      for (int i=0; i<timeSlots.length; i++){
        timeSB.append("[").append(i).append(",").append(timeSlots[i]).append("],");
      }
      timeSB.append("]},");
    }
    System.out.print(rankSB.toString());
    WriteFile.WriteToFile(rankSB.toString(),topicRankPath,false);
    System.out.println("Written result in data/news/"+topicRankPath+".");
    WriteFile.WriteToFile(timeSB.toString(),timeFile, false);
    System.out.println("Written result in data/news/"+timeFile+".");
  }

  public static String computeMetrics(Topic topic, int size){
    StringBuilder sb = new StringBuilder();
    sb.append(topic.getTopicID()).append(":");
    List<String> terms = topic.getTerms();
    for (int i=0; i<5; i++){
      sb.append(" ").append(terms.get(i));
    }
    double maxZScore = 0.0;
    double minZScore = 0.0;
    int[] population = topic.getTimeSlots();
    int sum = 0;
    int max = 0;
    int min = Integer.MAX_VALUE;
    int maxInd = 1;
    int minInd = 1;
    int derivativeSum = 0;
    int docNum = topic.getDocList().size();
    for (int i=1; i<size+1; i++){
      int value = population[population.length-i];
      if (value>max){
        max = value;
        maxInd = i;
      }
      if (value<min){
        min = value;
        maxInd = i;
      }
      sum += value;
      if (i!=size){
        derivativeSum += value - population[population.length-i-1];
      }
    }
    double average = (double)sum/size;
    double inter = 0.0;
    for (int c : population){
      inter += Math.pow(c - average, 2);
    }
    double std = Math.sqrt(inter / size);
    maxZScore = (max-average)/std/1/(Math.log(1.95+maxInd*.05)/Math.log(2));
//    minZScore = (min-average)/std/(Math.log(1.95+minInd*.05)/Math.log(2))*0.9;
    double norm = (double)(docNum-minDocNum)/(maxDocNum-minDocNum);
    sb.append(" max=").append(max)
            .append(", avg=").append(average)
            .append(", STD=").append(std)
            .append(", norm=").append(norm)
            .append(", ZScoreDiff=").append(maxZScore+minZScore).append(", new:").append(norm*maxZScore);
    topic.setScore(norm*maxZScore);
    return sb.toString();
  }
}
