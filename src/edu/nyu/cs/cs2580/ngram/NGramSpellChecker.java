package edu.nyu.cs.cs2580.ngram;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.nyu.cs.cs2580.query.Query;
import edu.nyu.cs.cs2580.spellCheck.BKTree.DistanceAlgo;
import edu.nyu.cs.cs2580.spellCheck.MisspellDataSet;
import edu.nyu.cs.cs2580.tokenizer.Tokenizer;
import edu.nyu.cs.cs2580.utils.ProgressBar;
import edu.nyu.cs.cs2580.utils.Util;

import java.io.*;
import java.util.*;

public class NGramSpellChecker implements Serializable {
  private static final long serialVersionUID = 1L;

  // Compressed inverted index, dynamically loaded per term at run time
  // Key: Term ID
  // Value: Compressed posting list.
  private Map<Integer, ArrayList<Integer>> _invertedIndex;

  private BiMap<String, Integer> _ngramDictionary;

  private BiMap<String, Integer> _dictionary;

  private static final int SUGGEST_COUNT = 10;

  private Map<String, Integer> _termFrequency;

  private DistanceAlgo _distanceAlgo;

  private MisspellDataSet _misspellDataSet;

  public NGramSpellChecker(BiMap<String, Integer> dictionary, Map<String, Integer> termFrequency,
                           DistanceAlgo distanceAlgo, MisspellDataSet misspellDataSet){
    _invertedIndex = new HashMap<>();
    _ngramDictionary = HashBiMap.create();
    _dictionary = dictionary;
    _termFrequency = termFrequency;
    _distanceAlgo = distanceAlgo;
    _misspellDataSet = misspellDataSet;
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

  public static NGramSpellChecker loadIndex(String indexFile) throws IOException, ClassNotFoundException{
    System.out.println("Load index from: " + indexFile);

    ObjectInputStream reader = new ObjectInputStream(new BufferedInputStream(new FileInputStream(indexFile)));
    NGramSpellChecker spellChecker = (NGramSpellChecker) reader.readObject();

    return spellChecker;
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
      results.put(term, getSuggestion(term, -1));
    }

    return new SpellCheckResult(results);
  }

  public ArrayList<String> getSuggestion(String term, int distance){
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
        Map<String, Integer> candidateDistances = new HashMap<>();

        Set entrySet = sortedMap.entrySet();
        Iterator it = entrySet.iterator();

        int suggestCount = SUGGEST_COUNT;

        int threhold = getSimilarityThrehold(term.length(), distance);
        String candidateTerm;
        int loopCount = 0;
        while (it.hasNext()){
          Map.Entry pairs = (Map.Entry)it.next();
          int termId = (Integer)pairs.getKey();
          candidateTerm = _dictionary.inverse().get(termId);
          int stringDistance = _distanceAlgo.getDistance(term, candidateTerm);
          if(stringDistance <= threhold){
            if(!candidateTerm.equals(term)){
              candidateDistances.put(candidateTerm, stringDistance);
            }
            else{
              _suggestions.add(candidateTerm);
              suggestCount--;
            }
          }
          loopCount++;
        }

        //TODO: use term frequency to boost results, need to be optimized
        LinkedHashMap<String, Integer> sortedCandidate = Util.sortHashMapByValues(candidateDistances, true);
        String suggestTerm;
        loopCount =0;
/*        candidateDistances.clear();
        entrySet = sortedCandidate.entrySet();
        it = entrySet.iterator();
        int editDistance;
        while (loopCount < suggestCount && it.hasNext()){
          Map.Entry pairs = (Map.Entry)it.next();
          suggestTerm = (String)pairs.getKey();
          editDistance = (Integer)pairs.getValue();
          int termFrequency = 1;
          if(_termFrequency.containsKey(suggestTerm)){
            termFrequency = _termFrequency.get(suggestTerm);
          }
          editDistance = editDistance * termFrequency;
          candidateDistances.put(suggestTerm, editDistance);
          loopCount++;
        }*/

        sortedCandidate.clear();
        sortedCandidate = Util.sortHashMapByValues(candidateDistances, true);
        entrySet = sortedCandidate.entrySet();
        it = entrySet.iterator();
        while (loopCount < suggestCount && it.hasNext()){
          Map.Entry pairs = (Map.Entry)it.next();
          suggestTerm = (String)pairs.getKey();
          _suggestions.add(suggestTerm);
          loopCount++;
        }

        _suggestions = sortPossibleElements(_suggestions, term);
      }
    }

    return _suggestions;
  }

  /**
   * Sort the possible elements..
   * Normalization reference: http://stn.spotfire.com/spotfire_client_help/norm/norm_scale_between_0_and_1.htm
   */
  private ArrayList<String> sortPossibleElements(List<String> possibleElementsForDistance, String elem) {
    List<String> correctWordsData = _misspellDataSet.getCorrectWords(elem.toString());
    ArrayList<String> res = new ArrayList<String>();

    Map<String, Double> resQueue = new HashMap<>();
    double minFrequency = 1;
    double maxFrequency = Integer.MIN_VALUE;

    for (String _elem : possibleElementsForDistance) {
      int frequency= _termFrequency.get(_elem);
      if (frequency > 0) {
        maxFrequency = Math.max(maxFrequency, frequency);
      }
    }

    for (String _elem : possibleElementsForDistance) {
      // Set the basic score as the element frequency...
      double frequency = _termFrequency.get(_elem);
      frequency = frequency == 0 ? 1 : frequency;

      double score = (frequency - minFrequency) / (maxFrequency - minFrequency);

      if (correctWordsData.contains(_elem.toString())) {
        score += 1;
      }
      resQueue.put(_elem, score);
    }

    LinkedHashMap<String, Double> sortedMap = Util.sortHashMapByValues(resQueue, true);
    Set entrySet = sortedMap.entrySet();
    Iterator it = entrySet.iterator();
    String suggestTerm;
    while (it.hasNext()){
      Map.Entry pairs = (Map.Entry)it.next();
      suggestTerm = (String)pairs.getKey();
      res.add(suggestTerm);
    }

    return res;
  }

  private static int getSimilarityThrehold(int length, int distance){
    int allowDistance;
    if(distance == -1){
      if(length > 10){
        allowDistance = 2;
      }
      else{
        allowDistance = 1;
      }
    }
    else {
      allowDistance = distance;
    }

    return allowDistance;
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
