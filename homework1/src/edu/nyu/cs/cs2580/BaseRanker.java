package edu.nyu.cs.cs2580;

import java.util.Vector;

public interface BaseRanker {
  Vector<ScoredDocument> runQuery(String query);
}
