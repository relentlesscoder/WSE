package edu.nyu.cs.cs2580.spellCheck.BKTree;

import com.google.common.base.Optional;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the implementation of node in BK-Tree.
 *
 * @param <E> type of elements in the tree
 */
public class Node<E> {
  private final E element;
  private final Map<Integer, Node<E>> children;

  Node(E element) {
    this.element = element;
    children = new HashMap<Integer, Node<E>>();
  }

  /**
   * Get the element within the node.
   * @return the element within the node.
   */
  public E getElement() {
    return element;
  }

  /**
   * Add an element as a child of that node
   * @param element the element needed to be added into the node's children
   * @param distance the distance between the element within the node and the element pass as one of the parameters
   */
  public void addChild(E element, int distance) {
    children.put(distance, new Node<E>(element));
  }

  /**
   * Get the child node of a specific distance.
   * If there's no such child node with the specific distance, the returned element will be absence.
   *
   * @param distance the distance between the source element and the element within the node.
   * @return the node of that specific distance.
   */
  public Optional<Node<E>> getChildNode(int distance) {
    if (children.containsKey(distance)) {
      return Optional.fromNullable(children.get(distance));
    } else {
      return Optional.fromNullable(null);
    }
  }

  /**
   * Check if the node contains a child with a specific distance
   * @param distance distance from that element within the node
   * @return true if the child exist, otherwise false
   */
  public boolean containsChild(int distance) {
    return children.containsKey(distance);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Node that = (Node) o;

    if (!this.children.equals(that.children)) {
      return false;
    }

    if (!this.element.equals(that.element)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int res = 17;
    res = 31 * res + element.hashCode();
    res = 31 * res + children.hashCode();
    return res;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Node: {");
    sb.append(" element: ").append(element.toString());
    sb.append(", children: ").append(children.toString());
    sb.append(" }");
    return sb.toString();
  }
}
