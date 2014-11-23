package edu.nyu.cs.cs2580;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 *          ["new york city"], the presence of the phrase "new york city" must
 *          be recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

  public List<List<String>> phrases = new ArrayList<List<String>>();
  public List<String> soloTerms = new ArrayList<String>();
  public boolean containsPhrase;

  public QueryPhrase(String query, boolean containsPhrase) {
    super(query);
    this.containsPhrase = containsPhrase;
  }

  @Override
  public void processQuery() {
    if (query == null) {
      return;
    }

    Set<String> uniqueTokens = new LinkedHashSet<String>();
    Tokenizer tokenizer = new Tokenizer(new StringReader(query));

    // Add all terms appearing in query
    while (tokenizer.hasNext()) {
      String term = Tokenizer.lowercaseFilter(tokenizer.getText());
      // Delete the stop words for normal query terms
      term = Tokenizer.stopwordFilter(term);
      term = Tokenizer.krovetzStemmerFilter(term);
      checkNotNull(term,
              "Term can not be null... or what did you do to the term, tokenizer!?)");
      uniqueTokens.add(term);
    }

    if (containsPhrase){
      Pattern p = Pattern.compile("(\".+?\")");
      Matcher m = p.matcher(query);

      while (m.find()) {
        String phrase = m.group(1).replaceAll("\"", "").trim();
        if (phrase.length() == 0) {
          continue;
        }
        List<String> phraseTerms = new ArrayList<String>();
        tokenizer = new Tokenizer(new StringReader(phrase));

        while (tokenizer.hasNext()) {
          String term = Tokenizer.lowercaseFilter(tokenizer.getText());
          term = Tokenizer.krovetzStemmerFilter(term);
          phraseTerms.add(term);
        }
        // Add the phrase :)
        phrases.add(phraseTerms);
      }
      Set<String> phraseTermSet = new LinkedHashSet<String>();
      for (List<String> phrase : phrases){
        phraseTermSet.addAll(phrase);
      }
      for(String term : uniqueTokens){
        if (!phraseTermSet.contains(term)){
            soloTerms.add(term);
          }
      }
      uniqueTokens.addAll(phraseTermSet);
      terms.addAll(uniqueTokens);
    }else{
      for (String term : uniqueTokens) {
        terms.add(term);
      }
    }
  }
}