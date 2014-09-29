package edu.nyu.cs.cs2580;

import java.util.Vector;

public class VectorSpaceModel implements BaseRanker {

  private Index _index;

  public VectorSpaceModel(String indexSource) {
    _index = new Index(indexSource);
  }

  @Override
  public Vector<ScoredDocument> runQuery(String query) {
    // TODO Auto-generated method stub
    return null;
  }

}
