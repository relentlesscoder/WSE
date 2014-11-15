package edu.nyu.cs.cs2580;

import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 * ["new york city"], the presence of the phrase "new york city" must be
 * recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

  //  public ListMultimap<String, String> _phrases = ArrayListMultimap.create();
  public List<List<String>> phrases = new ArrayList<List<String>>();
//  public List<String> soloTerms = new ArrayList<String>();
  public boolean containsPhrase;

  public QueryPhrase(String query, boolean containsPhrase) {
    super(query);
    this.containsPhrase = containsPhrase;
  }

  @Override
  public void processQuery() {
    Set<String> uniqueTokens = new HashSet<String>();
    Tokenizer tokenizer = new Tokenizer(new StringReader(query));

    if (query == null) {
      return;
    }

    // Process all phrases first
    if (containsPhrase) {
      Pattern p = Pattern.compile("(\".+?\")");
      Matcher m = p.matcher(query);

      // Store all phrases and terms (including the stop words)
      while (m.find()) {
        List<String> phraseTerms = new ArrayList<String>();
        String phrase = m.group(1).replaceAll("\"", "").trim();
        tokenizer = new Tokenizer(new StringReader(phrase));

        while (tokenizer.hasNext()) {
          String term = Tokenizer.lowercaseFilter(tokenizer.getText());
          term = Tokenizer.krovetzStemmerFilter(term);
          phraseTerms.add(term);
          uniqueTokens.add(term);
        }
        // Add the phrase :)
        phrases.add(phraseTerms);
      }
    }

    while (tokenizer.hasNext()) {
      String term = Tokenizer.lowercaseFilter(tokenizer.getText());
      // Delete the stop words for normal query terms
      term = Tokenizer.stopwordFilter(term);
      term = Tokenizer.krovetzStemmerFilter(term);
      checkNotNull(term, "Term can not be null... or what did you do to the term, tokenizer!?)");
      uniqueTokens.add(term);
    }

    for (String term : uniqueTokens) {
      terms.add(term);
    }
  }
}
