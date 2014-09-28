package edu.nyu.cs.cs2580;

import java.util.Vector;

public interface RankingAlgorithm {
  double scoreDocument(Vector<String> quertVector, Vector<String> documentVector);
}
