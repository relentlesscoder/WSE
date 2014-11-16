package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2 based on a refactoring of your favorite
 *          Ranker (except RankerPhrase) from HW1. The new Ranker should no
 *          longer rely on the instructors' {@link IndexerFullScan}, instead it
 *          should use one of your more efficient implementations.
 */
public class RankerFavorite extends Ranker {
	private final static double LAMDA = 0.50;
	IndexerInvertedCompressed indexerInvertedCompressed;
	DocumentIndexed document;

	public RankerFavorite(Options options, CgiArguments arguments, Indexer indexer) {
		super(options, arguments, indexer);
		this.indexerInvertedCompressed = (IndexerInvertedCompressed) this._indexer;
		System.out.println("Using Ranker: " + this.getClass().getSimpleName());
	}

	@Override
	public Vector<ScoredDocument> runQuery(Query query, int numResults) {
		System.out.println("runing query...");
		Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
		int nextDocid = -1;
		int count = 0;

		while (true) {
			Document document = indexerInvertedCompressed.nextDocLoose(query,
			    nextDocid);
			if (document == null) {
				break;
			}

			rankQueue.add(scoreDocument(query, document._docid));
			nextDocid = document._docid;
			if (rankQueue.size() > numResults) {
				rankQueue.poll();
			}
		}

		Vector<ScoredDocument> results = new Vector<ScoredDocument>();
		ScoredDocument scoredDoc = null;
		while ((scoredDoc = rankQueue.poll()) != null) {
			results.add(scoredDoc);
		}
		Collections.sort(results, Collections.reverseOrder());
		return results;
	}

	public ScoredDocument scoreDocument(Query query, int docId) {
		ScoredDocument scoredDocument = null;
		// C is the total number of word occurrences in the collection.
		long C = indexerInvertedCompressed.totalTermFrequency();

		// Query vector
		List<String> queryList = new ArrayList<String>();
		for (String term : query.terms) {
			queryList.add(term);
		}

		DocumentIndexed document = indexerInvertedCompressed.getDoc(docId);

		// Score the document. Here we have provided a very simple ranking model,
		// where a document is scored 1.0 if it gets hit by at least one query
		// term.
		double score = 0.0;

		for (int i = 0; i < queryList.size(); ++i) {
			String qi = queryList.get(i);

			// fqi_D is the number of times word qi occurs in document D.
			int fqi_D = indexerInvertedCompressed.documentTermFrequency(qi,
			    document._docid);
			// cqi is the number of times a query word occurs in the collection of
			// documents
			int cqi = indexerInvertedCompressed.corpusDocFrequencyByTerm(qi);
			// D is the number of words in D.
			double D = document.getTotalDocTerms();

			score += Math.log((1 - LAMDA) * (fqi_D / D) + LAMDA * (cqi / C));
		}

		// TODO: Not sure...
		score = Math.exp(score);

		scoredDocument = new ScoredDocument(document, score);

		return scoredDocument;
	}
}
