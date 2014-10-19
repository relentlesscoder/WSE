package edu.nyu.cs.cs2580;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;


import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.*;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 * ["new york city"], the presence of the phrase "new york city" must be
 * recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

  private ListMultimap<String, String> _phrases = ArrayListMultimap.create();
  private Vector<String> soloTokens = new Vector<String>();

  public QueryPhrase(String query) {
      super(query);
      System.out.println("Using Query: " + this.getClass().getSimpleName());
  }

  @Override
  public void processQuery() {

    if (_query == null) {
      return;
    }
    Tokenizer tokenizer;

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

    tokenizer = new Tokenizer(new StringReader(_query));
    while (tokenizer.hasNext()) {
      String term = Tokenizer.porterStemmerFilter(tokenizer.getText(), "english").toLowerCase();
      _tokens.add(term);
      //filter SoloTokens
      if (!isInPhrase(term, _phrases)) soloTokens.add(term);
    }
  }

  private boolean isInPhrase (String term, ListMultimap<String, String> phrases){
    ArrayList<String> strings = (ArrayList<String>) phrases.values();
    for (String str : strings){
      if (str.equals(term)) return true;
    }
    return false;
  }
}
