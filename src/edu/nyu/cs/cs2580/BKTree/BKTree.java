package edu.nyu.cs.cs2580.BKTree;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import edu.nyu.cs.cs2580.document.ScoredDocument;

import java.util.*;

public class BKTree<E> {
  /**
   * A simple class which stores the element and its score
   */
  private class ScoredResult<E> implements Comparable {
    public E elem;
    public double score;

    public ScoredResult(E elem, double score) {
      this.elem = elem;
      this.score = score;
    }

    /**
     * Sort in decreasing order...
     */
    @Override
    public int compareTo(Object o) {
      ScoredResult that = (ScoredResult) o;
      double diff = this.score - that.score;
      if (diff > 0) {
        return -1;
      } else if (diff < 0) {
        return 1;
      } else {
        return 0;
      }
    }
  }

  // Tolerance distance, when the distance between a source elem and the target node's elem
  // is computed, all the target node's children with distance between +-toleranceDistance is
  // pushed into the stack and wait for further assessment.
  private static int toleranceDistance = 1;

  private Node<E> root;
  private int elementCount;
  private DistanceAlgo distanceAlgo;
  private Multiset<E> elemFrequency;

  public BKTree(DistanceAlgo distanceAlgo) {
    this.distanceAlgo = distanceAlgo;
    this.elementCount = 0;
    this.elemFrequency = HashMultiset.create();
  }

  public BKTree(DistanceAlgo distanceAlgo, Map<E, Integer> elemFrequency) {
    this(distanceAlgo);
    for (E elem : elemFrequency.keySet()) {
      this.elemFrequency.setCount(elem, elemFrequency.get(elem));
    }
  }

  public BKTree(DistanceAlgo distanceAlgo, Multiset<E> elemFrequency) {
    this(distanceAlgo);
    this.elemFrequency = elemFrequency;
  }

  public void add(E element) {
    if (element == null) {
      throw new NullPointerException();
    }

    if (!isEnglishWord(element.toString())) {
      return;
    }

    if (root == null) {
      root = new Node<E>(element);
    } else {
      Node<E> node = root;
      int distance = distanceAlgo.getDistance(element, node.getElement());

      while (node.containsChild(distance)) {
        node = node.getChildNode(distance).get();
        distance = distanceAlgo.getDistance(element, node.getElement());
      }

      if (distance == 0) {
        return;
      }

      node.addChild(element, distance);
      elementCount++;
    }
  }

  public void addAll(Iterable<? extends E> elements) {
    if (elements == null) {
      throw new NullPointerException();
    }

    for (E element : elements) {
      add(element);
    }
  }

  public void addAll(E... elements) {
    if (elements == null) {
      throw new NullPointerException();
    }
    addAll(Arrays.asList(elements));
  }

  public Node<E> getRoot() {
    return root;
  }

  public boolean hasExist(E elem) {
    Optional<Node> node = Optional.of(root);
    while (node.isPresent()) {
      if (node.get().getElement().equals(elem)) {
        return true;
      } else {
        int distance = distanceAlgo.getDistance(node.get().getElement(), elem);
        node = node.get().getChildNode(distance);
      }
    }

    return false;
  }

  public List<E> getPossibleNodesForDistance(E elem, int expectDistance) {
    List<E> res = new ArrayList<E>();
    Deque<Node> stack = new ArrayDeque<Node>();

    if (hasExist(elem)) {
      res.add(elem);
      return res;
    }

    int count = 0;

    if (root == null) {
      return res;
    }

    stack.addLast(root);

    while (!stack.isEmpty()) {
      count++;
      Node<String> node = stack.pollFirst();
      int currDistance = distanceAlgo.getDistance(elem, node.getElement());

      // Find a match
      if (currDistance <= expectDistance) {
        res.add((E) node.getElement());
      }

      int minDistance = currDistance - toleranceDistance;
      int maxDistance = currDistance + toleranceDistance;

      for (int distance = minDistance; distance <= maxDistance; distance++) {
        if (node.containsChild(distance)) {
          stack.addLast((Node) node.getChildNode(distance).get());
        }
      }
    }

//    System.out.println("Searched " + count + " elements...\n");

    if (res.size() == 0) {
      // Can't found a single one.... return the element itself...
      res.add(elem);
    }

    return res;
  }

  public List<E> getPossibleNodesForDistanceWithOrder(E elem, int expectDistance) {
    List<E> possibleNodesForDistance = getPossibleNodesForDistance(elem, expectDistance);
    List<E> res = new ArrayList<E>();

    Queue<ScoredResult> resQueue = new PriorityQueue<ScoredResult>();
    for (E _elem : possibleNodesForDistance) {
      double score = elemFrequency.count(_elem);
      if (score == 0) {
        // smoothing...
        score = 1.0;
      }
      ScoredResult scoredResult = new ScoredResult(_elem, score);
      resQueue.add(scoredResult);
    }

    while (!resQueue.isEmpty()) {
      res.add((E) resQueue.poll().elem);
    }
    return res;
  }

  public List<E> getPossibleNodesForDistanceWithOrder(E elem, int expectDistance, int size) {
    List<E> possibleNodesForDistance = getPossibleNodesForDistanceWithOrder(elem, expectDistance);
    List<E> res = new ArrayList<E>();
    int num = Math.min(size, possibleNodesForDistance.size());

    for (int i = 0; i < num; i++) {
      res.add(possibleNodesForDistance.get(i));
    }

    return res;
  }

  public int getElementCount() {
    return elementCount;
  }

  public boolean isEnglishWord(String term) {
    return term.matches("^[a-zA-Z]+$");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BKTree that = (BKTree) o;

    if (this.root == null ^ that.root == null) return false;
    if (this.root == null && that.root == null) return true;
    if (!this.root.equals(that.root)) return false;
    if (!this.distanceAlgo.equals(that.distanceAlgo)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int res = 17;
    res = 31 * res + (root == null ? 0 : root.hashCode());
    res = 31 * res + distanceAlgo.hashCode();
    return res;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("BKTree: { ");
    sb.append(" distance algorithm: ").append(distanceAlgo.toString());
    sb.append(", root:").append(root.toString());
    sb.append(" }");
    return sb.toString();
  }
}
