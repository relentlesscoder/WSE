package edu.nyu.cs.cs2580.spellCheck;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.nyu.cs.cs2580.SearchEngine;
import edu.nyu.cs.cs2580.index.IndexerInvertedCompressed;
import edu.nyu.cs.cs2580.query.Query;
import edu.nyu.cs.cs2580.rankers.IndexerConstant;
import edu.nyu.cs.cs2580.spellCheck.BKTree.DamerauLevenshteinAlgorithm;
import edu.nyu.cs.cs2580.spellCheck.BKTree.DistanceAlgo;
import edu.nyu.cs.cs2580.tokenizer.Tokenizer;
import edu.nyu.cs.cs2580.utils.ProgressBar;
import edu.nyu.cs.cs2580.utils.StringUtil;
import edu.nyu.cs.cs2580.utils.Util;

import java.io.*;
import java.util.*;

public class NGramSpellChecker implements SpellChecker, Serializable {
  private static final long serialVersionUID = 1L;

  private static final int SUGGEST_COUNT = 10;

  // Compressed inverted index, dynamically loaded per term at run time
  // Key: Term ID
  // Value: Compressed posting list.
  private Map<Integer, ArrayList<Integer>> _invertedIndex;

  private BiMap<String, Integer> _ngramDictionary;

  private BiMap<String, Integer> _dictionary;

  private Map<String, Integer> _termFrequency;

  private DistanceAlgo _distanceAlgo;

  private MisspellDataSet _misspellDataSet;

  private long _totalTermFrequency;

  public NGramSpellChecker() {
    _invertedIndex = new HashMap<>();
    _ngramDictionary = HashBiMap.create();
    _misspellDataSet = new MisspellDataSet();
    _dictionary = HashBiMap.create();
    _termFrequency = new HashMap<String, Integer>();
  }

  public NGramSpellChecker(BiMap<String, Integer> dictionary, Map<String, Integer> termFrequency) {
    _invertedIndex = new HashMap<>();
    _ngramDictionary = HashBiMap.create();
    _misspellDataSet = new MisspellDataSet();
    _dictionary = dictionary;
    _termFrequency = termFrequency;
  }

  public NGramSpellChecker(BiMap<String, Integer> dictionary, Map<String, Integer> termFrequency,
                           DistanceAlgo distanceAlgo, MisspellDataSet misspellDataSet) {
    this(dictionary, termFrequency);
    _distanceAlgo = distanceAlgo;
    _misspellDataSet = misspellDataSet;
  }

  public NGramSpellChecker(BiMap<String, Integer> dictionary, Map<String, Integer> termFrequency,
                           long totalTermFrequency) {
    this(dictionary, termFrequency);
    _totalTermFrequency = totalTermFrequency;
  }

  public void construct(SearchEngine.Options options, String fileName) throws IOException, ClassNotFoundException {
    IndexerInvertedCompressed loaded;
    File folder = new File(options._indexPrefix);
    File[] files = folder.listFiles();

    /**************************************************************************
     * Load the class file first
     *************************************************************************/
    for (File file : files) {
      if (file.getName().equals(fileName + IndexerConstant.EXTENSION_IDX)) {
        String indexFile = options._indexPrefix + "/" + fileName + IndexerConstant.EXTENSION_IDX;

        ObjectInputStream reader = new ObjectInputStream(new BufferedInputStream(new FileInputStream(indexFile)));
        loaded = (IndexerInvertedCompressed) reader.readObject();

        this._dictionary = loaded.getDictionary();
        this._termFrequency = loaded.getTermFrequency();
        _totalTermFrequency = loaded.getTotalTermFrequency();

        reader.close();
      }
    }

    buildIndex();
    String spellIndexFile = "";
    if (fileName.equals(IndexerConstant.HTML_META)) {
      spellIndexFile = options._spellPrefix + "/" + IndexerConstant.HTML_SPELL_INDEX + IndexerConstant.EXTENSION_IDX;
    } else {
      spellIndexFile = options._spellPrefix + "/" + IndexerConstant.NEWS_FEED_SPELL_INDEX + IndexerConstant.EXTENSION_IDX;
    }

    System.out.println("Storing spell inverted index to: " + options._spellPrefix);
    ObjectOutputStream spellWriter = new ObjectOutputStream(new BufferedOutputStream(new
        FileOutputStream(spellIndexFile)));

    spellWriter.writeObject(this);
    spellWriter.close();
  }

  public void buildIndex() {
    // Progress bar
    ProgressBar _progressBar = new ProgressBar();
    if (_dictionary != null && _dictionary.size() > 0) {
      //index terms for spell check
      int count = 0;
      for (Map.Entry dictionaryEntry : _dictionary.entrySet()) {
        count++;
        _progressBar.update(count, _dictionary.size());
        int termId = (Integer) dictionaryEntry.getValue();
        String term = (String) dictionaryEntry.getKey();
        ArrayList<String> ngrams = Tokenizer.nGramFilters(term, -1, -1);
        if (ngrams != null && ngrams.size() > 0) {
          for (String ngram : ngrams) {
            addToInvertedIndex(ngram, termId);
          }
        }
      }
    }
  }

  public void loadIndex(String indexFile) {
    System.out.println("Load index from: " + indexFile);
    NGramSpellChecker load = null;
    ObjectInputStream reader = null;

    try {
      reader = new ObjectInputStream(new BufferedInputStream(new FileInputStream(indexFile)));
      load = (NGramSpellChecker) reader.readObject();
      this._dictionary = load._dictionary;
      this._termFrequency = load._termFrequency;
      this._totalTermFrequency = load._totalTermFrequency;
      this._ngramDictionary = load._ngramDictionary;
      this._invertedIndex = load._invertedIndex;
      this._distanceAlgo = new DamerauLevenshteinAlgorithm<String>();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void addToInvertedIndex(String ngram, int termId) {
    int gramId;
    ArrayList<Integer> postingList;
    if (_ngramDictionary.containsKey(ngram)) {
      gramId = _ngramDictionary.get(ngram);
      postingList = _invertedIndex.get(gramId);
      if (!postingList.contains(termId)) {
        postingList.add(termId);
      }
    } else {
      gramId = _ngramDictionary.size();
      _ngramDictionary.put(ngram, gramId);
      ArrayList<Integer> termIds = new ArrayList<>();
      termIds.add(termId);
      _invertedIndex.put(gramId, termIds);
    }
  }

  public String getSuggestion(String term, int distance) {
    if (_dictionary.containsKey(term)) {
      return term;
    }

    ArrayList<String> _suggestions = new ArrayList<>();

    if (_invertedIndex == null || _invertedIndex.size() <= 0) {
      return null;
    } else {
      int threhold = getSimilarityThreshold(term.length(), distance);

      int length = term.length();
      ArrayList<String> termGrams = Tokenizer.nGramFilters(term, getMin(length), getMax(length));
      if (termGrams != null && termGrams.size() > 0) {
        HashMap<Integer, Integer> candidateTerms = new HashMap<>();
        ArrayList<Integer> postingList;
        for (String gram : termGrams) {
          if (_ngramDictionary.containsKey(gram)) {
            int gramId = _ngramDictionary.get(gram);
            postingList = _invertedIndex.get(gramId);
            if (postingList != null && postingList.size() > 0) {
              for (Integer termId : postingList) {
                String termToAdd = _dictionary.inverse().get(termId);
                int lengthDiff = termToAdd.length() < term.length() ? term.length() - termToAdd.length() : termToAdd.length() - term.length();
                if (lengthDiff <= threhold) {
                  if (candidateTerms.containsKey(termId)) {
                    candidateTerms.put(termId, candidateTerms.get(termId) + 1);
                  } else {
                    candidateTerms.put(termId, 1);
                  }
                }
              }
            }
          }
        }

        LinkedHashMap<Integer, Integer> sortedMap = Util.sortHashMapByValues(candidateTerms, true);
        Map<String, Double> candidateDistances = new HashMap<>();

        Set entrySet = sortedMap.entrySet();
        Iterator it = entrySet.iterator();

        int suggestCount = SUGGEST_COUNT;

        String candidateTerm;
        int loopCount = 0;
        while (it.hasNext()) {
          Map.Entry pairs = (Map.Entry) it.next();
          int termId = (Integer) pairs.getKey();
          candidateTerm = _dictionary.inverse().get(termId);
          int stringDistance = StringUtil.getLevenshteinDistance(term, candidateTerm);
          if (stringDistance <= threhold) {
            if (!candidateTerm.equals(term)) {
              candidateDistances.put(candidateTerm, (double) stringDistance);
            } else {
              _suggestions.add(candidateTerm);
              suggestCount--;
            }
          }
          loopCount++;
        }

        //TODO: use term frequency to boost results, need to be optimized
        LinkedHashMap<String, Double> sortedCandidate = Util.sortHashMapByValues(candidateDistances, true);
        String suggestTerm;
        loopCount = 0;

        boolean useTermFrequency = (_misspellDataSet.isEmpty() && _totalTermFrequency != 0);

        if (useTermFrequency) {
          candidateDistances.clear();
          entrySet = sortedCandidate.entrySet();
          it = entrySet.iterator();
          Double editDistance;
          double score;
          while (loopCount < suggestCount && it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            suggestTerm = (String) pairs.getKey();
            editDistance = (Double) pairs.getValue();
            int termFrequency = 1;
            if (_termFrequency.containsKey(suggestTerm)) {
              termFrequency = _termFrequency.get(suggestTerm);
            }
            score = editDistance * ((double) termFrequency / (double) _totalTermFrequency);
            candidateDistances.put(suggestTerm, score);
            loopCount++;
          }
        }

        loopCount = 0;
        sortedCandidate.clear();
        sortedCandidate = Util.sortHashMapByValues(candidateDistances, true);
        entrySet = sortedCandidate.entrySet();
        it = entrySet.iterator();
        while (loopCount < suggestCount && it.hasNext()) {
          Map.Entry pairs = (Map.Entry) it.next();
          suggestTerm = (String) pairs.getKey();
          _suggestions.add(suggestTerm);
          loopCount++;
        }

        if (!useTermFrequency) {
          _suggestions = sortPossibleElementsByMissSpellSet(_suggestions, term);
        }

        if (_suggestions.size() > 0) {
          return _suggestions.get(0);
        }
      }
    }

    return "";
  }

  /**
   * Sort the possible elements..
   * Normalization reference: http://stn.spotfire.com/spotfire_client_help/norm/norm_scale_between_0_and_1.htm
   */
  private ArrayList<String> sortPossibleElementsByMissSpellSet(List<String> possibleElementsForDistance, String elem) {
    List<String> correctWordsData = _misspellDataSet.getCorrectWords(elem.toString());
    ArrayList<String> res = new ArrayList<String>();

    Map<String, Double> resQueue = new HashMap<>();
    double minFrequency = 1;
    double maxFrequency = Integer.MIN_VALUE;

    for (String _elem : possibleElementsForDistance) {
      int frequency = _termFrequency.get(_elem);
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
    while (it.hasNext()) {
      Map.Entry pairs = (Map.Entry) it.next();
      suggestTerm = (String) pairs.getKey();
      res.add(suggestTerm);
    }

    return res;
  }

  private static int getSimilarityThreshold(int length, int distance) {
    int allowDistance;
    if (distance == -1) {
      if (length > 10) {
        allowDistance = 2;
      } else {
        allowDistance = 1;
      }
    } else {
      allowDistance = distance;
    }

    return allowDistance;
  }

  private static int getMin(int length) {
    if (length > 5) {
      return 3;
    } else if (length == 5) {
      return 2;
    }
    return 1;
  }

  private static int getMax(int length) {
    if (length > 5) {
      return 4;
    } else if (length == 5) {
      return 3;
    }
    return 2;
  }

  @Override
  public CorrectedQuery getCorrectedQuery(Query query) {
    StringBuilder sb = new StringBuilder();
    boolean isQueryCorrect = true;
    for (String word : query._tokens) {
      String correctedWord = getSuggestion(word, -1);
      boolean isWordCorrect = (correctedWord.equals(word));
      if (isWordCorrect) {
        sb.append(word).append(" ");
      } else {
        isQueryCorrect = false;
        sb.append(correctedWord).append(" ");
      }
    }

    // Delete the last space
    sb.setLength(sb.length() - 1);
    return new CorrectedQuery(isQueryCorrect, sb.toString());
  }

  @Override
  public String getMostPossibleCorrectWord(String term) {
    return getSuggestion(term, -1);
  }

  @Override
  public List<String> getMostPossibleCorrectWord(String term, int size) {
    return new ArrayList<String>();
  }

  //  @Override
//  public SpellCheckResult getSpellCheckResults(Query query) {
//
//    ArrayList<SpellCheckCorrection> results = new ArrayList<>();
//
//    for (String term : query._tokens) {
//      String spellCorrection = getSuggestion(term, -1);
//      boolean isCorrect = (spellCorrection.equals(term));
//      results.add(new SpellCheckCorrection(isCorrect, spellCorrection));
//    }
//
//    return new SpellCheckResult(results);
//  }
}
