package edu.nyu.cs.cs2580.preprocess;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tanis on 12/14/14.
 */
public class Topic {

  private int topicID;
  private List<DocTopicPr> docList;
  private List<String> terms;
//  private int mostDocNum;
//  private int leastDocNum;
  private int[] timeSlots;
  public final static int timeSpan = 14;
  private double score;

  public Topic(int topicID) {
    this.topicID=topicID;
    timeSlots = new int[timeSpan];
    terms = new ArrayList<String>();
    docList = new ArrayList<DocTopicPr>();
    score = 0.0;
  };

  public int getTopicID() {
    return topicID;
  }

  public void setTopicID(int topicID) {
    this.topicID = topicID;
  }

  public int[] getTimeSlots() {
    return timeSlots;
  }

  public void addToTimeSlots(int num) {
    this.timeSlots[num]++;
  }

  public List<String> getTerms() {
    return terms;
  }

  public List<DocTopicPr> getDocList() {
    return docList;
  }

  public Double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

}

class DocTopicPr{
  private Integer docID;
  private Double proportion;

  DocTopicPr (int docid, double proportion){
    this.docID = docid;
    this.proportion = proportion;
  }

  public Integer getDocID() {
    return docID;
  }

  public Double getProportion() {
    return proportion;
  }

  @Override
  public String toString(){
    return docID+"\t"+proportion;
  }
}