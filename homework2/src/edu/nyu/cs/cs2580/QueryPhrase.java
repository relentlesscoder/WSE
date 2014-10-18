package edu.nyu.cs.cs2580;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.io.StringReader;
import java.util.regex.*;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 * ["new york city"], the presence of the phrase "new york city" must be
 * recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

  private ListMultimap<String, String> _phrases = ArrayListMultimap.create();

  public QueryPhrase(String query) {
      super(query);
      System.out.println("Using Query: " + this.getClass().getSimpleName());
  }

  @Override
  public void processQuery() {

    if (_query == null) {
      return;
    }
    Tokenizer tokenizer = new Tokenizer(new StringReader(_query));

    while (tokenizer.hasNext()) {
      String term = Tokenizer.porterStemmerFilter(tokenizer.getText(), "english").toLowerCase();
      _tokens.add(term);
    }

    Pattern p = Pattern.compile("(\".+?\")");
    Matcher m = p.matcher(_query);

    while(m.find()) {
      String phrase = m.group(1).replaceAll("\"","").trim();
      tokenizer = new Tokenizer(new StringReader(phrase));

      while (tokenizer.hasNext()){
        String term = Tokenizer.porterStemmerFilter(tokenizer.getText(), "english").toLowerCase();
        _phrases.get(phrase).add(term);
        //System.out.println(term);
      }
    }
  }
}
