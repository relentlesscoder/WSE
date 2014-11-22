package edu.nyu.cs.cs2580;

import java.io.Serializable;

public class MetaPair implements Serializable {
  private static final long serialVersionUID = 1L;

  private long startPos;
  private int length;

  public MetaPair(long startPos, int length) {
    this.startPos = startPos;
    this.length = length;
  }

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public long getStartPos() {
    return startPos;
  }

  public void setStartPos(long startPos) {
    this.startPos = startPos;
  }
}