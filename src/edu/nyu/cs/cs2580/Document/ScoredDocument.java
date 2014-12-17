package edu.nyu.cs.cs2580.document;

/**
 * document with score.
 * 
 * @author fdiaz
 * @author congyu
 */
public class ScoredDocument implements Comparable<ScoredDocument> {
	private Document _doc;
	private double _score;
	private double _pageRank;
	private int _numView;

	public ScoredDocument(Document doc, double score, double pageRank, int numView) {
		_doc = doc;
		_score = score;
		_pageRank = pageRank;
		_numView = numView;
	}

	public String asTextResult() {
		StringBuffer buf = new StringBuffer();
		buf.append(_doc._docid).append("\t");
		buf.append(_doc.getTitle()).append("\t");
		buf.append(_score).append("\t");
		buf.append(_pageRank).append("\t");
		buf.append(_numView);
		return buf.toString();
	}

	/**
	 * @CS2580: Student should implement {@code asHtmlResult} for final project.
	 */
	public String asHtmlResult() {
		return "";
	}

	@Override
	public int compareTo(ScoredDocument o) {
		if (this._score == o._score) {
			return 0;
		}
		return (this._score > o._score) ? 1 : -1;
	}

	public double getScore() {
		return _score;
	}

	public void setScore(double score) {
		this._score = score;
	}

	public double getPageRank() {
		return _pageRank;
	}

	public void setPageRank(double _pageRank) {
		this._pageRank = _pageRank;
	}

	public int getNumView() {
		return _numView;
	}

	public void setNumView(int _numView) {
		this._numView = _numView;
	}

	public String getTitle() {
		return this._doc.getTitle();
	}

	public String getUrl() {
		return this._doc.getUrl();
	}

  public Document getDocument() {
    return this._doc;
  }

  public String getServerUrl() {
		String url = this._doc.getUrl();
		if (url.matches("^http.*")) {
			return url;
		} else {
			return "data/wiki/" + url;
		}
	}

  public int getDocID() { return this._doc._docid; }
}
