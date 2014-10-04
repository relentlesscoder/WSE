package edu.nyu.cs.cs2580;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

class Document {
  private static final Logger logger = LogManager.getLogger();
  // <Term, Unique ID>
  private static Map<String, Integer> _dictionary = new HashMap<String, Integer>();
  // All terms
  private static List<String> _rdictionary = new ArrayList<String>();
  // <ID, Number of documents the term occurs in>
  private static Map<Integer, Integer> _df = new HashMap<Integer, Integer>();
  // <ID, Number of occurrences of the term in the entire collection>
  private static Map<Integer, Integer> _tf = new HashMap<Integer, Integer>();
  // Number of occurrences of all terms in the entire collection
  private static int _total_tf = 0;
  public int docId;
  // List of terms of the title
  private List<Integer> titleTermsList = new ArrayList<Integer>();
  // List of terms of the body
  private List<Integer> bodyTermsList = new ArrayList<Integer>();
  // The title string
  private String titleStr;
  private int numviews;

  public Document(int docId, String content) {
    logger.info("Initiating document...");
    Scanner scanner = null;

    try {
      scanner = new Scanner(content);
      scanner.useDelimiter("\t");

      titleStr = scanner.next();

      // Read the title
      readTermList(titleStr, titleTermsList);
      // Read the body
      readTermList(scanner.next(), bodyTermsList);

      Set<Integer> uniqueTerms = new HashSet<Integer>();

      for (int i = 0; i < titleTermsList.size(); ++i) {
        int idx = titleTermsList.get(i);
        uniqueTerms.add(idx);
        int old_tf = _tf.get(idx);
        _tf.put(idx, old_tf + 1);
        _total_tf++;
      }

      for (int i = 0; i < bodyTermsList.size(); ++i) {
        int idx = bodyTermsList.get(i);
        uniqueTerms.add(idx);
        int old_tf = _tf.get(idx);
        _tf.put(idx, old_tf + 1);
        _total_tf++;
      }

      for (Integer idx : uniqueTerms) {
        if (_df.containsKey(idx)) {
          int old_df = _df.get(idx);
          _df.put(idx, old_df + 1);
        }
      }

      numviews = Integer.parseInt(scanner.next());
      this.docId = docId;

    } catch (Exception e) {
      logger.error("Create document error, due to: " + e);
    } finally {
      if (scanner != null) {
        scanner.close();
      }
    }

  }

  /**
   * Return the number of documents a specific term occurs in.
   */
  public static int documentFrequency(String term) {
    return _dictionary.containsKey(term) ? _df.get(_dictionary.get(term)) : 0;
  }

  /**
   * Return the number of occurrences of a specific term in the entire
   * collection.
   */
  public static int termFrequency(String term) {
    return _dictionary.containsKey(term) ? _tf.get(_dictionary.get(term)) : 0;
  }

  /**
   * Returns the total number of words occurrences in the collection
   * (i.e. the sum of termFrequency(s) over all words in the vocabulary).
   */
  public static int termFrequency() {
    return _total_tf;
  }

  /**
   * Get a term from the dictionary by a its index
   */
  public static String getTerm(int index) {
    return _rdictionary.get(index);
  }

  /**
   * Return the number of different terms in the collection.
   */
  public static int numTerms() {
    return _rdictionary.size();
  }

  public String getTitleStr() {
    return titleStr;
  }

  public int getNumviews() {
    return numviews;
  }

  public List<String> getTitleList() {
    return getTermList(titleTermsList);
  }

  public List<String> getBodyList() {
    return getTermList(bodyTermsList);
  }

  private List<String> getTermList(List<Integer> termList) {
    List<String> retval = new ArrayList<String>();
    for (int idx : termList) {
      retval.add(_rdictionary.get(idx));
    }
    return retval;
  }

  private void readTermList(String raw, List<Integer> termList) {
    Scanner scanner = null;

    try {
      scanner = new Scanner(raw);
      while (scanner.hasNext()) {
        String term = scanner.next();
        int idx = -1;

        if (_dictionary.containsKey(term)) {
          idx = _dictionary.get(term);
        } else {
          idx = _rdictionary.size();
          _rdictionary.add(term);
          _dictionary.put(term, idx);
          _tf.put(idx, 0);
          _df.put(idx, 0);
        }
        termList.add(idx);
      }
    } catch (Exception e) {
      logger.error("error, due to: " + e);
    } finally {
      if (scanner != null) {
        scanner.close();
      }
    }
  }
}
