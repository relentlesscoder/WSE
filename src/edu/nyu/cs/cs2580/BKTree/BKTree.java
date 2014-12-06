package edu.nyu.cs.cs2580.BKTree;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public class BKTree<E> {
  private static final int THRESHOLD = 1;
  private Node<E> root;
  private DistanceAlgo distanceAlgo;

  public BKTree(DistanceAlgo distanceAlgo) {
    this.distanceAlgo = distanceAlgo;
  }

  public void add(E element) {
    if (element == null) {
      throw new NullPointerException();
    }

    if (root == null) {
      root = new Node<E>(element);
    } else {
      Node<E> node = root;
      while (!node.getElement().equals(element)) {
        int distance = distanceAlgo.getDistance(node.getElement(), element);
        if (!node.getChildNode(distance).isPresent()) {
          node.addChild(distance, element);
        }
        node = node.getChildNode(distance).get();
      }
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
    res = 31 * res + (root == null ? 0 :root.hashCode());
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
