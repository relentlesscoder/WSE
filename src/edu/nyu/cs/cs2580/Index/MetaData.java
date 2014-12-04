package edu.nyu.cs.cs2580.Index;

import java.io.Serializable;

public class MetaData implements Serializable {
  private static final long serialVersionUID = 1L;

  private long corpusTermFrequency;
  private int corpusDocFrequencyByTerm;
  private Offsets postingListMetaData;

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

  public Offsets getPostingListMetaData() {
    return postingListMetaData;
  }

  public void setPostingListMetaData(Offsets postingListMetaData) {
    this.postingListMetaData = postingListMetaData;
  }
}
