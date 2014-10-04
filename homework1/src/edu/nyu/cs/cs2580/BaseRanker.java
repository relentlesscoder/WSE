package edu.nyu.cs.cs2580;

import java.util.List;

public interface BaseRanker {
  List<ScoredDocument> runQuery(String query);
}
