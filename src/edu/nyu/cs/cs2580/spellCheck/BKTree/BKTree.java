package edu.nyu.cs.cs2580.spellCheck.BKTree;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.io.Files;
import edu.nyu.cs.cs2580.spellCheck.MisspellDataSet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Following pages are my reference...
 * http://norvig.com/spell-correct.html
 * http://nullwords.wordpress.com/2013/03/13/the-bk-tree-a-data-structure-for-spell-checking/
 * http://blog.notdot.net/2007/4/Damn-Cool-Algorithms-Part-1-BK-Trees
 * <p>
 * *Misspell data set
 * http://www.dcs.bbk.ac.uk/~ROGER/corpora.html
 */
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
  // is computed, all the target node's children with distance between +-TOLERANCE_DISTANCE is
  // pushed into the stack and wait for further assessment.
  private static final int TOLERANCE_DISTANCE = 2;

  // Default edit distance, this should cover most of the misspelled words
  private static final int DISTANCE_ONE = 1;
  private static final int DISTANCE_TWO = 2;

  private Node<E> root;
  private int elementCount;
  private DistanceAlgo distanceAlgo;
  // The element frequency multiset
  private Multiset<E> elemFrequency;
  // This is the misspell data set, or in another word, the error model...
  private MisspellDataSet misspellDataSet;

  /**
   * Constructor...
   */
  public BKTree(DistanceAlgo distanceAlgo) {
    this.distanceAlgo = distanceAlgo;
    this.elementCount = 0;
    this.elemFrequency = HashMultiset.create();
    this.misspellDataSet = new MisspellDataSet();
  }

  /**
   * Constructor, with the element frequency map
   */
  public BKTree(DistanceAlgo distanceAlgo, Map<E, Integer> elemFrequency) {
    this(distanceAlgo);
    for (E elem : elemFrequency.keySet()) {
      this.elemFrequency.setCount(elem, elemFrequency.get(elem));
    }
  }

  /**
   * Constructor, with the element frequency multiset
   */
  public BKTree(DistanceAlgo distanceAlgo, Multiset<E> elemFrequency) {
    this(distanceAlgo);
    this.elemFrequency = elemFrequency;
  }

  /**
   * Add an element to the tree
   */
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

  /**
   * Add a external dictionary, the format should be one word per line...
   * TODO:
   * By using this method, we assume E is String.... Deal with it later....
   */
  public void addDictionary(File file) throws IOException {
    // The dependency is guava lib...
    // This is for convenience... It does use more memory
    String content = Files.toString(file, StandardCharsets.UTF_8);

    String[] words = content.split("\n");
    for (String word : words) {
      add((E) word);
    }
  }

  /**
   * Add elements to the tree
   */
  public void addAll(Iterable<? extends E> elements) {
    if (elements == null) {
      throw new NullPointerException();
    }

    for (E element : elements) {
      add(element);
    }
  }

  /**
   * Add elements to the tree
   */
  public void addAll(E... elements) {
    if (elements == null) {
      throw new NullPointerException();
    }
    addAll(Arrays.asList(elements));
  }

  /**
   * Add the misspell data set to improve the accuracy
   */
  public void addMisspellDataSet(MisspellDataSet misspellDataSet) {
    this.misspellDataSet = misspellDataSet;
  }

  /**
   * Get the root element
   */
  public Node<E> getRoot() {
    return root;
  }

  /**
   * Check if the element exist in the tree, a more efficient way is to check the
   * dictionary though...
   */
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

  /**
   * Get all possible elements for a specific distance
   *
   * @return a list of possible elements, if not elements were found, return an
   * empty list.
   */
  public List<E> getPossibleElementsForDistance(E elem, int expectDistance) {
    List<E> res = new ArrayList<E>();
    Deque<Node> stack = new ArrayDeque<Node>();

    if (root == null) {
      return res;
    }

    stack.addLast(root);

    while (!stack.isEmpty()) {
      Node<String> node = stack.pollFirst();
      int currDistance = distanceAlgo.getDistance(elem, node.getElement());

      if (currDistance == 0) {
        // Return the element itself, since it does exist in the tree, it may
        // not be misspell word...
        res.clear();
        res.add(elem);
        return res;
      }

      // Find a match
      if (currDistance <= expectDistance) {
        res.add((E) node.getElement());
      }

      int minDistance = currDistance - TOLERANCE_DISTANCE;
      int maxDistance = currDistance + TOLERANCE_DISTANCE;

      for (int distance = minDistance; distance <= maxDistance; distance++) {
        if (node.containsChild(distance)) {
          stack.addLast((Node) node.getChildNode(distance).get());
        }
      }
    }

//    System.out.println("Searched " + count + " elements...\n");

    return res;
  }

  /**
   * Get all possible elements.
   * It will first use distance 1, if not results were found, it will try
   * distance 2. If still none found, an empty list will be returned.
   *
   * @return a list of possible elements, if not elements were found, return an
   * empty list.
   */
  public List<E> getPossibleElements(E elem) {
    List<E> res = getPossibleElementsForDistance(elem, DISTANCE_ONE);
    if (res.size() == 0) {
      // Not found for distance 1, try distance 2...
      return getPossibleElementsForDistance(elem, DISTANCE_TWO);
    } else {
      return res;
    }
  }

  /**
   * Get all possible elements for a specific distance with order.
   *
   * @return a list of possible elements, if not elements were found, return an
   * empty list.
   */
  public List<E> getPossibleElementsForDistanceWithOrder(E elem, int expectDistance) {
    List<E> possibleElements = getPossibleElementsForDistance(elem, expectDistance);

    return sortPossibleElements(possibleElements, elem);
  }

  /**
   * Get all possible elements for a specific distance with order.
   * It will first use distance 1, if not results were found, it will try
   * distance 2. If still none found, an empty list will be returned.
   *
   * @return a list of possible elements, if not elements were found, return an
   * empty list.
   */
  public List<E> getPossibleElementsWithOrder(E elem) {
    List<E> possibleElements = getPossibleElements(elem);

    return sortPossibleElements(possibleElements, elem);
  }

  /**
   * Get a specific number of possible elements for a specific distance with order.
   *
   * @return a list of possible elements, if not elements were found, return an
   * empty list. If the number of possible elements is less then the specific
   * size, it will return as many as it can found.
   */
  public List<E> getPossibleElementsForDistanceWithOrder(E elem, int expectDistance, int size) {
    List<E> possibleElements = getPossibleElementsForDistanceWithOrder(elem, expectDistance);
    List<E> res = new ArrayList<E>();
    int num = Math.min(size, possibleElements.size());

    for (int i = 0; i < num; i++) {
      res.add(possibleElements.get(i));
    }

    return res;
  }

  /**
   * Get a specific number of elements for a specific distance with order.
   * It will first use distance 1, if not results were found, it will try
   * distance 2. If still none found, an empty list will be returned.
   *
   * @return a list of possible elements, if not elements were found, return an
   * empty list. If the number of possible elements is less then the specific
   * size, it will return as many as it can found.
   */
  public List<E> getPossibleElementsWithOrder(E elem, int size) {
    List<E> possibleNodesForDistance = getPossibleElementsWithOrder(elem);
    List<E> res = new ArrayList<E>();
    int num = Math.min(size, possibleNodesForDistance.size());

    for (int i = 0; i < num; i++) {
      res.add(possibleNodesForDistance.get(i));
    }

    return res;
  }

  /**
   * Get the most possible element for a specific distance.
   *
   * @return the most possible element, if it is not present... just check it with Optional.isPresent()...
   */
  public Optional<E> getMostPossibleElementsForDistance(E elem, int expectDistance) {
    List<E> possibleElements = getPossibleElementsForDistanceWithOrder(elem, expectDistance);

    if (possibleElements.size() > 0) {
      return Optional.fromNullable(possibleElements.get(0));
    } else {
      return Optional.fromNullable(null);
    }
  }

  /**
   * Get a specific number of elements for a specific distance with order.
   * It will first use distance 1, if not results were found, it will try
   * distance 2. If still none found, an empty list will be returned.
   *
   * @return a list of possible elements, if not elements were found, return an
   * empty list. If the number of possible elements is less then the specific
   * size, it will return as many as it can found.
   */
  public Optional<E> getMostPossibleElement(E elem) {
    List<E> possibleElements = getPossibleElementsWithOrder(elem);

    if (possibleElements.size() > 0) {
      return Optional.fromNullable(possibleElements.get(0));
    } else {
      return Optional.fromNullable(null);
    }
  }

  /**
   * Sort the possible elements..
   * Normalization reference: http://stn.spotfire.com/spotfire_client_help/norm/norm_scale_between_0_and_1.htm
   */
  private List<E> sortPossibleElements(List<E> possibleElementsForDistance, E elem) {
    List<String> correctWordsData = misspellDataSet.getCorrectWords(elem.toString());
    List<E> res = new ArrayList<E>();

    Queue<ScoredResult> resQueue = new PriorityQueue<ScoredResult>();
    double minFrequency = 1;
    double maxFrequency = Integer.MIN_VALUE;

    for (E _elem : possibleElementsForDistance) {
      int frequency= elemFrequency.count(_elem);
      if (frequency > 0) {
        maxFrequency = Math.max(maxFrequency, frequency);
      }
    }

    for (E _elem : possibleElementsForDistance) {
      // Set the basic score as the element frequency...
      double frequency = elemFrequency.count(_elem);
      frequency = frequency == 0 ? 1 : frequency;

      double score = (frequency - minFrequency) / (maxFrequency - minFrequency);

      if (correctWordsData.contains(_elem.toString())) {
        score += 1;
      }

      ScoredResult scoredResult = new ScoredResult(_elem, score);
      resQueue.add(scoredResult);
    }

    while (!resQueue.isEmpty()) {
      res.add((E) resQueue.poll().elem);
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
