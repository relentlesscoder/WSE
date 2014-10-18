package edu.nyu.cs.cs2580;

import java.io.StringReader;
import java.util.Vector;

/**
 * Representation of a user query.
 * 
 * In HW1: instructors provide this simple implementation.
 * 
 * In HW2: students must implement {@link QueryPhrase} to handle phrases.
 * 
 * @author congyu
 * @auhtor fdiaz
 */
public class Query {
  public String _query = null;
  public Vector<String> _tokens = new Vector<String>();

  public Query(String query) {
    _query = query;
  }

  public void processQuery() {
    if (_query == null) {
      return;
    }
    Tokenizer tokenizer = new Tokenizer(new StringReader(_query));

    while (tokenizer.hasNext()) {
      String term = Tokenizer.porterStemmerFilter(tokenizer.getText(), "english").toLowerCase();
      _tokens.add(term);
    }
  }
}
