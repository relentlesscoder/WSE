package edu.nyu.cs.cs2580.handler;

/**
 * Created by tanis on 11/20/14.
 */
public class TermPrf {
  public String term;
  private int frequency;
  public double prf;

  public TermPrf (String term, int frequency){
    this.term = term;
    this.frequency = frequency;
  }
  public TermPrf (String term, double prf){
    this.term = term;
    this.prf = prf;
  }
  public void computePrf (int count){
    prf = (double) frequency/count;
  }
}
