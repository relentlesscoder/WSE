package edu.nyu.cs.cs2580;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Vector;

// @CS2580: This is a simple implementation that you will be changing
// in homework 2.  For this homework, don't worry about how this is done.
class Document {
  public int _docid;

  private static HashMap<String, Integer> _dictionary = new HashMap<String, Integer>();
  private static Vector<String> _rdictionary = new Vector<String>();
  private static HashMap<Integer, Integer> _df = new HashMap<Integer, Integer>();
  private static HashMap<Integer, Integer> _tf = new HashMap<Integer, Integer>();
  private static int _total_tf = 0;

  private Vector<Integer> _body;
  private Vector<Integer> _title;
  private String _titleString;
  private int _numviews;

  public static int documentFrequency(String term) {
    return _dictionary.containsKey(term) ? _df.get(_dictionary.get(term)) : 0;
  }

  public static int termFrequency(String term) {
    return _dictionary.containsKey(term) ? _tf.get(_dictionary.get(term)) : 0;
  }

  public static int termFrequency() {
    return _total_tf;
  }

  public static String getTerm(int index) {
    return _rdictionary.get(index);
  }

  public static int numTerms() {
    return _rdictionary.size();
  }

  public Document(int did, String content) {
    Scanner scanner = null;

    try {
      scanner = new Scanner(content);
      scanner.useDelimiter("\t");

      _titleString = scanner.next();
      _title = new Vector<Integer>();
      _body = new Vector<Integer>();

      readTermVector(_titleString, _title);
      readTermVector(scanner.next(), _body);

      HashSet<Integer> unique_terms = new HashSet<Integer>();
      for (int i = 0; i < _title.size(); ++i) {
        int idx = _title.get(i);
        unique_terms.add(idx);
        int old_tf = _tf.get(idx);
        _tf.put(idx, old_tf + 1);
        _total_tf++;
      }
      for (int i = 0; i < _body.size(); ++i) {
        int idx = _body.get(i);
        unique_terms.add(idx);
        int old_tf = _tf.get(idx);
        _tf.put(idx, old_tf + 1);
        _total_tf++;
      }
      for (Integer idx : unique_terms) {
        if (_df.containsKey(idx)) {
          int old_df = _df.get(idx);
          _df.put(idx, old_df + 1);
        }
      }
      _numviews = Integer.parseInt(scanner.next());
      _docid = did;

    } catch (Exception e) {
      // TODO: handle exception
    } finally {
      if (scanner != null) {
        scanner.close();
      }
    }

  }

  public String get_title_string() {
    return _titleString;
  }

  public int get_numviews() {
    return _numviews;
  }

  public Vector<String> get_title_vector() {
    return getTermVector(_title);
  }

  public Vector<String> get_body_vector() {
    return getTermVector(_body);
  }

  private Vector<String> getTermVector(Vector<Integer> tv) {
    Vector<String> retval = new Vector<String>();
    for (int idx : tv) {
      retval.add(_rdictionary.get(idx));
    }
    return retval;
  }

  private void readTermVector(String raw, Vector<Integer> tv) {
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
        tv.add(idx);
      }
    } catch (Exception e) {
      // TODO: handle exception
    } finally {
      if (scanner != null) {
        scanner.close();
      }
    }
  }
}
