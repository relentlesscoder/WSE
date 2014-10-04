package edu.nyu.cs.cs2580;

// @CS2580: this class should not be changed.
class ScoredDocument {

  private int docId;
  private String title;
  private double score;

  ScoredDocument(int did, String title, double score) {
    docId = did;
    this.title = title;
    this.score = score;
  }

  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  @Override
  public String toString() {
    return docId + "\t" + title + "\t" + score;
  }
}
