package edu.nyu.cs.cs2580;

import java.util.Comparator;

// @CS2580: this class should not be changed.
class ScoredDocument implements Comparable<ScoredDocument> {
  public int _did;
  public String _title;
  public double _score;

  ScoredDocument(int did, String title, double score){
    _did = did;
    _title = title;
    _score = score;
  }

  String asString(){
    return new String(
      Integer.toString(_did) + "\t" + _title + "\t" + Double.toString(_score));
  }

  @Override
  public int compareTo(ScoredDocument o) {
    double compareScore = o._score;

    return (int) (compareScore - this._score );
  }
}
