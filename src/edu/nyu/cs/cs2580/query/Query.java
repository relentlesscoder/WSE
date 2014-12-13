package edu.nyu.cs.cs2580.query;

import edu.nyu.cs.cs2580.tokenizer.Tokenizer;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Vector;

/**
 * Representation of a user query.
 * <p>
 * In HW1: instructors provide this simple implementation.
 * <p>
 * In HW2: students must implement {@link QueryPhrase} to handle phrases.
 *
 * @author congyu
 * @auhtor fdiaz
 */
public class Query {
  public String query = null;
  public Vector<String> _tokens = new Vector<String>();

  public Query(String query) {
    this.query = query;
  }

  public void processQuery() {
    if (query == null) {
      return;
    }
    Tokenizer tokenizer = new Tokenizer(new StringReader(query));
    HashSet<String> tokenSet = new HashSet<String>();
    while (tokenizer.hasNext()) {
      String term = Tokenizer.lowercaseFilter(tokenizer.getText());
      term = Tokenizer.stopwordFilter(term);
      if (term != null) {
        term = Tokenizer.krovetzStemmerFilter(term);
        tokenSet.add(term);
      }
    }
    for (String term : tokenSet) {
      _tokens.add(term);
    }
  }
}
