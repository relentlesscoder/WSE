package edu.nyu.cs.cs2580;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;


import java.io.StringReader;
import java.util.*;
import java.util.regex.*;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 * ["new york city"], the presence of the phrase "new york city" must be
 * recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

  public ListMultimap<String, String> _phrases = ArrayListMultimap.create();
  public Vector<String> soloTokens = new Vector<String>();

  public QueryPhrase(String query) {
      super(query);
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
        String term = Tokenizer.lowercaseFilter(tokenizer.getText());
        term = Tokenizer.krovetzStemmerFilter(term);
        _phrases.get(phrase).add(term);
        //System.out.println(term);
      }
    }

    tokenizer = new Tokenizer(new StringReader(_query));
    HashSet<String> tokenSet = new HashSet<String>();
    while (tokenizer.hasNext()) {
      String term = Tokenizer.lowercaseFilter(tokenizer.getText());
      if (term != null) {
        term = Tokenizer.krovetzStemmerFilter(term);
        tokenSet.add(term);
      }
    }
    Set<String> phraseValueSet = new HashSet<String>();
    phraseValueSet.addAll(_phrases.values());
    for(String term : tokenSet){

      if (phraseValueSet.contains(term)){
        _tokens.add(term);
      }else {
        term = Tokenizer.stopwordFilter(term);
        if (term != null){
          _tokens.add(term);
          soloTokens.add(term);
        }
      }


    }
  }


}
