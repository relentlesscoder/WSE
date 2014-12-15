package edu.nyu.cs.cs2580.ngram;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import edu.nyu.cs.cs2580.query.Query;
import edu.nyu.cs.cs2580.tokenizer.Tokenizer;
import edu.nyu.cs.cs2580.utils.ProgressBar;
import edu.nyu.cs.cs2580.utils.StringUtil;
import edu.nyu.cs.cs2580.utils.Util;

import java.io.File;
import java.io.Serializable;
import java.util.*;

public class NGramSpellChecker implements Serializable {
  private static final long serialVersionUID = 1L;

  private File _index;

  // Compressed inverted index, dynamically loaded per term at run time
  // Key: Term ID
  // Value: Compressed posting list.
  private Map<Integer, ArrayList<Integer>> _invertedIndex;

  private BiMap<String, Integer> _ngramDictionary;

  private BiMap<String, Integer> _dictionary;

  private static final int SUGGEST_COUNT = 10;

  public NGramSpellChecker(File index, ImmutableBiMap<String, Integer> dictionary){
    _index = index;
    _invertedIndex = new HashMap<>();
    _ngramDictionary = HashBiMap.create();
    _dictionary = dictionary;
  }

  public void buildIndex(){
    // Progress bar
    ProgressBar _progressBar = new ProgressBar();
    if(_dictionary != null && _dictionary.size() > 0){
      //index terms for spell check
      int count = 0;
      for(Map.Entry dictionaryEntry: _dictionary.entrySet()){
        count++;
        _progressBar.update(count, _dictionary.size());
        int termId = (Integer)dictionaryEntry.getValue();
        String term = (String)dictionaryEntry.getKey();
        ArrayList<String> ngrams = Tokenizer.nGramFilters(term, -1, -1);
        if(ngrams != null && ngrams.size() > 0){
          for(String ngram : ngrams){
            addToInvertedIndex(ngram, termId);
          }
        }
      }
    }
  }

  private void addToInvertedIndex(String ngram, int termId){
    int gramId;
    ArrayList<Integer> postingList;
    if(_ngramDictionary.containsKey(ngram)){
      gramId = _ngramDictionary.get(ngram);
      postingList = _invertedIndex.get(gramId);
      if(!postingList.contains(termId)){
        postingList.add(termId);
      }
    }
    else{
      gramId = _ngramDictionary.size();
      _ngramDictionary.put(ngram, gramId);
      ArrayList<Integer> termIds = new ArrayList<>();
      termIds.add(termId);
      _invertedIndex.put(gramId, termIds);
    }
  }

  public SpellCheckResult getSpellCheckResults(Query query){
    HashMap<String, ArrayList<String>> results = new HashMap<>();

    for(String term : query._tokens){
      results.put(term, getSuggestion(term));
    }

    return new SpellCheckResult(results);
  }

  private ArrayList<String> getSuggestion(String term){
    ArrayList<String> _suggestions = new ArrayList<>();

    if(_invertedIndex == null || _invertedIndex.size() <= 0){
      return null;
    }
    else{
      int length = term.length();
      ArrayList<String> termGrams = Tokenizer.nGramFilters(term, getMin(length), getMax(length));
      if(termGrams != null && termGrams.size() > 0){
        HashMap<Integer, Integer> candidateTerms = new HashMap<>();
        ArrayList<Integer> postingList;
        for(String gram : termGrams){
          if(_ngramDictionary.containsKey(gram)){
            int gramId = _ngramDictionary.get(gram);
            postingList = _invertedIndex.get(gramId);
            if(postingList != null && postingList.size() > 0){
              for(Integer termId : postingList){
                if(candidateTerms.containsKey(termId)){
                  candidateTerms.put(termId, candidateTerms.get(termId) + 1);
                }
                else{
                  candidateTerms.put(termId, 1);
                }
              }
            }
          }
        }

        LinkedHashMap<Integer, Integer> sortedMap = Util.sortHashMapByValues(candidateTerms, true);
        Map<String, Float> candidateDistances = new HashMap<>();

        Set entrySet = sortedMap.entrySet();
        Iterator it = entrySet.iterator();

        String candidateTerm;
        int loopCount = 0;
        while (it.hasNext()){
          Map.Entry pairs = (Map.Entry)it.next();
          int termId = (Integer)pairs.getKey();
          candidateTerm = _dictionary.inverse().get(termId);
          float stringDistance = StringUtil.getLevensteinDistance(term, candidateTerm);
          stringDistance = stringDistance *
          candidateDistances.put(candidateTerm, stringDistance);
          loopCount++;
        }

        LinkedHashMap<String, Float> sortedCandidate = Util.sortHashMapByValues(candidateDistances, true);
        entrySet = sortedCandidate.entrySet();
        it = entrySet.iterator();
        loopCount =0;
        String suggestTerm;
        while (loopCount < SUGGEST_COUNT && it.hasNext()){
          Map.Entry pairs = (Map.Entry)it.next();
          suggestTerm = (String)pairs.getKey();
          _suggestions.add(suggestTerm);
          loopCount++;
        }
      }
    }

    return _suggestions;
  }

  private static int getMin(int length){
    if(length > 5){
      return 3;
    }
    else if(length == 5){
      return 2;
    }
    return 1;
  }

  private static int getMax(int length){
    if(length > 5){
      return 4;
    }
    else if(length == 5){
      return 3;
    }
    return 2;
  }
}
