package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class WSEUtil {
  static void sortScore(List<ScoredDocument> retrieval_results) {
    Collections.sort(retrieval_results, new Comparator<ScoredDocument>() {
      @Override
      public int compare(ScoredDocument o1, ScoredDocument o2) {
        if (o1._score > o2._score) {
          return -1;
        } else if (o1._score < o2._score) {
          return 1;
        } else {
          return 0;
        }
      }
    });
  }
}
