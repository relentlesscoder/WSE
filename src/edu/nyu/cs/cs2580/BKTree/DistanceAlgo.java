package edu.nyu.cs.cs2580.BKTree;

/**
 * This is the interface for algorithms which calculates the distance
 * between two strings.
 */
public interface DistanceAlgo<E> {
  public int getDistance(E element1, E element2);
}
