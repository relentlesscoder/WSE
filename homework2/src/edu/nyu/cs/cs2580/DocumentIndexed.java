package edu.nyu.cs.cs2580;

import java.util.Vector;

/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 * information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document {
  private static final long serialVersionUID = 9184892508124423115L;

  private Indexer indexer = null;

  private Vector<String> _links = new Vector<String>();

  public DocumentIndexed(int docid, Indexer indexer) {
    super(docid);
    this.indexer = indexer;
  }

  public Vector<String> get_links() {
    return _links;
  }

  public void set_links(Vector<String> _links) {
    this._links = _links;
  }
}
