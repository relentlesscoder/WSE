package edu.nyu.cs.cs2580.index;

import java.io.Serializable;

/**
 * This class contains the start position and end position of a document of a specific field such as
 * title. The start position is the first term of the field and the end position is 1 pass the last
 * term of that field.
 */
public class FieldPositionRange implements Serializable {
  private static final long serialVersionUID = 1L;

  private int startPos;
  private int endPos;

  public FieldPositionRange(int startPos, int endPos) {
    this.startPos = startPos;
    this.endPos = endPos;
  }

  public int getLength() {
    return endPos - startPos;
  }

  public int getStartPos() {
    return startPos;
  }

  public int getEndPos() {
    return endPos;
  }
}
