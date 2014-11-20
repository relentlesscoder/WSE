package edu.nyu.cs.cs2580;

import java.io.Serializable;
import java.util.HashMap;

public class MetaData implements Serializable {
  private static final long serialVersionUID = 1L;

  private long corpusTermFrequency;
  private int corpusDocFrequencyByTerm;
  private MetaPair postingListMetaData;

  public MetaData() {
    corpusTermFrequency = 0;
    corpusDocFrequencyByTerm = 0;
  }

  public long getCorpusTermFrequency() {
    return corpusTermFrequency;
  }

  public void setCorpusTermFrequency(long corpusTermFrequency) {
    this.corpusTermFrequency = corpusTermFrequency;
  }

  public int getCorpusDocFrequencyByTerm() {
    return corpusDocFrequencyByTerm;
  }

  public void setCorpusDocFrequencyByTerm(int corpusDocFrequencyByTerm) {
    this.corpusDocFrequencyByTerm = corpusDocFrequencyByTerm;
  }

  public MetaPair getPostingListMetaData() {
    return postingListMetaData;
  }

  public void setPostingListMetaData(MetaPair postingListMetaData) {
    this.postingListMetaData = postingListMetaData;
  }
}
