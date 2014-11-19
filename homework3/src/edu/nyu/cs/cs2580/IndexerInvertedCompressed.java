package edu.nyu.cs.cs2580;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.common.primitives.Bytes;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * This is the compressed inverted indexer...
 */
public class IndexerInvertedCompressed extends Indexer implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final int K = 100;
	/**
	 * ***********************************************************************
	 * {@code lastDocid}, {@code lastPostingListSize} and
	 * {@code lastSkipPointerOffset} is temporary and will be cleared once the
	 * index isconstructed...
	 * ***********************************************************************
	 */

	// The last/previous docid of the posting list.
	// Key is the term and value is the last/previous docid of the posting list.
	// This is used to construct the index and will be cleared after the process.
	Map<String, Integer> lastDocid = new HashMap<String, Integer>();
	Map<String, Integer> lastPostingListSize = new HashMap<String, Integer>();
	Map<String, Integer> lastSkipPointerOffset = new HashMap<String, Integer>();
	// Compressed inverted index.
	// Key is the term and value is the compressed posting list.
	private ListMultimap<String, Byte> invertedIndex = ArrayListMultimap.create();
	// The offset of each docid of the posting list for each term.
	// Key is the term and value is the offsets for each of docid in the posting
	// list.
	private ListMultimap<String, Byte> skipPointers = ArrayListMultimap.create();
	// Term frequency across whole corpus.
	// key is the term and value is the frequency of the term across the whole
	// corpus.
	private Multiset<String> _termCorpusFrequency = HashMultiset.create();
	private List<DocumentIndexed> documents = new ArrayList<DocumentIndexed>();
	private Map<String, Integer> docUrlMap = new HashMap<String, Integer>();
	private Map<String, MetaPair> metaData = new HashMap<String, MetaPair>();

	// Provided for serialization
	public IndexerInvertedCompressed() {
	}

	public IndexerInvertedCompressed(Options options) {
		super(options);
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}

	/**
	 * Extract i + 1 first 7 bits from {@code val} (From right to left) e.g. If i
	 * = 0 and val = 101 0101 1010 (In binary form) The return byte will be 0101
	 * 1010...
	 * 
	 * @param i
	 *          The i first 7 bits (From right to left)
	 * @param val
	 *          the number...
	 * @return the extracted 7 bits
	 */
	private static byte extract7Bits(int i, long val) {
		return (byte) ((val >> (7 * i)) & ((1 << 7) - 1));
	}

	/**
	 * If {@code val} has n bits, return the last n % 7 bit. (From right to left)
	 * e.g. If i = 0 and val = 101 0101 1010 (In binary form) The return byte will
	 * be 0000 1010...
	 * 
	 * @param i
	 *          The last n % 7 (n - 7 * i) bits (From right to left)
	 * @param val
	 *          the number...
	 * @return the extracted 7 bits
	 */
	private static byte extractLastBits(int i, long val) {
		return (byte) ((val >> (7 * i)));
	}

	@Override
	public void constructIndex() throws IOException {
		long totalStartTimeStamp = System.currentTimeMillis();
		ProgressBar progressBar = new ProgressBar();
		File folder = new File(_options._corpusPrefix);
		File[] files = folder.listFiles();
		int fileCount = 0;
		long startTimeStamp, duration;

		checkNotNull(files, "No files found in: %s", folder.getPath());

		/**************************************************************************
		 * First clean the folder....
		 *************************************************************************/
		File outputFolder = new File(_options._indexPrefix);
		for (File file : outputFolder.listFiles()) {
			file.delete();
		}

		/**************************************************************************
		 * Indexing....
		 *************************************************************************/
		startTimeStamp = System.currentTimeMillis();
		System.out.println("Indexing...");

		// Process file/document one by one and assign each of them a unique docid
		for (int docid = 0; docid < files.length; docid++) {
			checkNotNull(files[docid], "File can not be null!");
			// Update the progress bar first :)
			progressBar.update(docid, files.length);
			processDocument(files[docid], docid);

			if (Util.hasReachThresholdCompress(invertedIndex)) {
				// Memory not enough, write to file first...
				Util.writePartialInvertedIndexCompress(invertedIndex, _options,
				    ++fileCount);
				invertedIndex.clear();
			}
		}

		// Write the rest partial inverted index...
		Util.writePartialInvertedIndexCompress(invertedIndex, _options, ++fileCount);
		invertedIndex.clear();

		// Get the number of documents
		_numDocs = documents.size();

		// Clear the {@code lastDocid} and {@code lastSkipPointerOffset} since it's
		// no longer needed...
		lastDocid.clear();
		lastSkipPointerOffset.clear();
		lastPostingListSize.clear();

		duration = System.currentTimeMillis() - startTimeStamp;

		System.out.println("Complete indexing");
		System.out.println("Indexing time: " + Util.convertMillis(duration));
		System.out.println("Indexed " + Integer.toString(_numDocs) + " docs with "
		    + Long.toString(_totalTermFrequency) + " terms.");

		/**************************************************************************
		 * Merging....
		 *************************************************************************/
		startTimeStamp = System.currentTimeMillis();
		System.out.println("Merging...");

		try {
			merge();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		duration = System.currentTimeMillis() - startTimeStamp;
		System.out.println("Complete merging");
		System.out.println("Merging time: " + Util.convertMillis(duration));

		/**************************************************************************
		 * Serializing the rest...
		 *************************************************************************/
		startTimeStamp = System.currentTimeMillis();
		System.out.println("Serializing...");

		// Serialize the whole object :)
		String indexFile = _options._indexPrefix + "/corpus.idx";
		System.out.println("Storing inverted index to: " + _options._indexPrefix);
		ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(
		    indexFile));
		writer.writeObject(this);
		writer.close();

		duration = System.currentTimeMillis() - startTimeStamp;
		System.out.println("Mission completed :)");
		System.out.println("Serialization time: " + Util.convertMillis(duration));

		duration = System.currentTimeMillis() - totalStartTimeStamp;
		System.out.println("Mission complete :) 唉呀妈呀, 跑死我了... OTL");
		System.out.println("Total time: " + Util.convertMillis(duration));
	}

	/**
	 * Process the document. First store the document, then populate the inverted
	 * index.
	 * 
	 * @param file
	 *          The file waiting to be indexed.
	 * @param docid
	 *          The file's document ID.
	 * @throws IOException
	 */
	private void processDocument(File file, int docid) throws IOException {
		org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(file, "UTF-8");

		String bodyText = jsoupDoc.body().text();
		String title = jsoupDoc.title();

		// Create the document and store it.
		DocumentIndexed doc = new DocumentIndexed(docid, this);
		doc.setTitle(title);

		doc.setUrl(file.getAbsolutePath());
		documents.add(doc);
		docUrlMap.put(doc.getUrl(), docid);

		// Populate the inverted index.
		populateInvertedIndex(title + " " + bodyText, docid);

		// TODO: Deal with all the links later...
		// Elements links = jsoupDoc.select("a[href]");
	}

	/**
	 * Populate the inverted index.
	 * 
	 * @param content
	 *          The text content of the html document.
	 * @param docid
	 *          The document ID.
	 */
	private void populateInvertedIndex(String content, int docid) {
		// Uncompressed temporary inverted index.
		// Key is the term and value is the uncompressed posting list.
		ListMultimap<String, Integer> tmpInvertedIndex = ArrayListMultimap.create();
		Tokenizer tokenizer = new Tokenizer(new StringReader(content));
		int position = 0;

		/**************************************************************************
		 * Start to process the document one term at a time.
		 *************************************************************************/
		while (tokenizer.hasNext()) {
			// Tokenizer... Stemmer... Filter...

			String term = Tokenizer.lowercaseFilter(tokenizer.getText());
			term = Tokenizer.krovetzStemmerFilter(term);

			if (term == null) {
				continue;
			}

			// Populate the temporary inverted index.
			if (tmpInvertedIndex.containsKey(term)) {
				// The term has already been seen at least once in the document.
				int occurs = tmpInvertedIndex.get(term).get(1);
				// Update the occurrence
				tmpInvertedIndex.get(term).set(1, occurs + 1);
				// Add the position of this term
				tmpInvertedIndex.get(term).add(position);
			} else {
				// This is the first time the term has been seen in the document
				if (lastDocid.containsKey(term)) {
					// The inverted index has already seen the term in previous
					// documents

					// Get the last/previous docid of the term's posting list
					int prevDocid = lastDocid.get(term);
					int deltaDocid = docid - prevDocid;
					tmpInvertedIndex.get(term).add(deltaDocid);

					if ((lastPostingListSize.get(term) - lastSkipPointerOffset.get(term))
					    / K > 0) {
						skipPointers.get(term).addAll(vByteEncoding(prevDocid));
						skipPointers.get(term).addAll(
						    vByteEncoding(lastPostingListSize.get(term)));

						lastSkipPointerOffset.put(term, lastPostingListSize.get(term));
					}
				} else {
					// The inverted index hasn't seen the term in previous documents

					// No need to calculate the delta since it's the first docid of
					// the
					// posting list.
					// the delta.
					tmpInvertedIndex.get(term).add(docid);
					lastDocid.put(term, 0);
					lastPostingListSize.put(term, 0);
					lastSkipPointerOffset.put(term, 0);
				}
				tmpInvertedIndex.get(term).add(1);
				tmpInvertedIndex.get(term).add(position);
			}

			// Update the termCorpusFrequency
			_termCorpusFrequency.add(term);

			// Update the totalTermFrequency
			_totalTermFrequency++;
			// Move to the next position
			position++;
		}

		/**************************************************************************
		 * Finish the process of all terms.
		 *************************************************************************/

		documents.get(docid).setTotalDocTerms(++position);

		/**************************************************************************
		 * Start to compress...
		 *************************************************************************/

		// 1. Convert all positions of the posting list to deltas
		convertPositionToDelta(tmpInvertedIndex);

		// 2. Compress the temporary inverted index and populate the inverted index.
		for (String term : tmpInvertedIndex.keySet()) {
			// Update lastDocid
			lastDocid.put(term, docid);

			// Encode the posting list
			List<Byte> partialPostingList = vByteEncodingList(tmpInvertedIndex
			    .get(term));

			// Append the posting list if one exists, otherwise create one first :)
			invertedIndex.get(term).addAll(partialPostingList);
			lastSkipPointerOffset.put(term, lastPostingListSize.get(term)
			    + invertedIndex.get(term).size());
		}
	}

	/**
	 * Convert the offsets of each posting list to deltas. e.g. From: List: 0
	 * (docid), 4 (occurrence), 5, 12, 18, 29 To: List: 0 (docid), 4 (occurrence),
	 * 5, 7, 6, 11
	 * 
	 * @param partialInvertedIndex
	 *          The partial/temporary inverted index
	 */
	private void convertPositionToDelta(
	    ListMultimap<String, Integer> partialInvertedIndex) {
		for (String term : partialInvertedIndex.keySet()) {
			List<Integer> list = partialInvertedIndex.get(term);
			if (list.get(1) > 1) {
				for (int i = list.size() - 1; i > 2; i--) {
					int offset = list.get(i);
					int prevOffset = list.get(i - 1);
					list.set(i, offset - prevOffset);
				}
			}
		}
	}

	/**
	 * Check if the byte is the end of the number.
	 * 
	 * @param b
	 *          A v-byte encoded byte.
	 * @return true if the byte is the last byte of the number (the highest bit is
	 *         1), otherwise false.
	 */
	private boolean isEndOfNum(byte b) {
		return b < 0;
	}

	/**
	 * Encode a integer number to a list of bytes
	 * 
	 * @param num
	 *          The number needed to be encoded
	 * @return a list of v-byte encoded byte represents the input number
	 */
	private List<Byte> vByteEncoding(int num) {
		List<Byte> bytes = new ArrayList<Byte>();

		if (num < (1 << 7)) {
			bytes.add((byte) (num | (1 << 7)));
		} else if (num < (1 << 14)) {
			bytes.add((byte) extract7Bits(0, num));
			bytes.add((byte) (extractLastBits(1, (num)) | (1 << 7)));
		} else if (num < (1 << 21)) {
			bytes.add((byte) extract7Bits(0, num));
			bytes.add((byte) extract7Bits(1, num));
			bytes.add((byte) (extractLastBits(2, (num)) | (1 << 7)));
		} else if (num < (1 << 28)) {
			bytes.add((byte) extract7Bits(0, num));
			bytes.add((byte) extract7Bits(1, num));
			bytes.add((byte) extract7Bits(2, num));
			bytes.add((byte) (extractLastBits(3, (num)) | (1 << 7)));
		} else {
			bytes.add((byte) extract7Bits(0, num));
			bytes.add((byte) extract7Bits(1, num));
			bytes.add((byte) extract7Bits(2, num));
			bytes.add((byte) extract7Bits(3, num));
			bytes.add((byte) (extractLastBits(4, (num)) | (1 << 7)));
		}

		return bytes;
	}

	/**
	 * Decode a list of byteList to one integer number
	 * 
	 * @param byteList
	 *          A list of v-byte encoded bytes needed to be decoded
	 * @return The decoded number
	 */
	private int vByteDecoding(List<Byte> byteList) {
		int num = 0;

		for (int i = 0; i < byteList.size(); i++) {
			num = ((byteList.get(i) & 0x7F) << 7 * i) | num;
		}

		return num;
	}

	/**
	 * Encode a list of Integers to a list of Bytes by using v-byte
	 * 
	 * @param list
	 *          A list of integer numbers needed to be encoded
	 * @return a list of v-byte encoded bytes
	 */
	private List<Byte> vByteEncodingList(List<Integer> list) {
		List<Byte> res = new ArrayList<Byte>();

		for (int i : list) {
			res.addAll(vByteEncoding(i));
		}

		return res;
	}

	/**
	 * Decode a list of Bytes to a list of Integers by using v-byte
	 * 
	 * @param byteList
	 *          A list of v-byte encoded bytes which represents a list of number
	 * @return a list of decoded integer numbers
	 */
	private List<Integer> vByteDecodingList(List<Byte> byteList) {
		List<Integer> res = new ArrayList<Integer>();
		int i = 0;

		while (i < byteList.size()) {
			List<Byte> tmpByteList = new ArrayList<Byte>();
			int num = 0;

			while (!isEndOfNum(byteList.get(i))) {
				tmpByteList.add(byteList.get(i++));
			}

			tmpByteList.add(byteList.get(i++));

			res.add(vByteDecoding(tmpByteList));
		}

		return res;
	}

	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
		IndexerInvertedCompressed loaded;
		File folder = new File(_options._indexPrefix);
		File[] files = folder.listFiles();

		// Load the class file first
		for (File file : files) {
			if (file.getName().equals("corpus.idx")) {
				String indexFile = _options._indexPrefix + "/corpus.idx";
				System.out.println("Load index from: " + indexFile);

				ObjectInputStream reader = new ObjectInputStream(new FileInputStream(
				    indexFile));
				loaded = (IndexerInvertedCompressed) reader.readObject();

				this.documents = loaded.documents;

				// Compute numDocs and totalTermFrequency b/c Indexer is not
				// serializable.
				this._numDocs = documents.size();
				this._totalTermFrequency = loaded._termCorpusFrequency.size();

				this.invertedIndex = loaded.invertedIndex;
				this.skipPointers = loaded.skipPointers;
				this._termCorpusFrequency = loaded._termCorpusFrequency;
				this.docUrlMap = loaded.docUrlMap;
				this.metaData = loaded.metaData;
				reader.close();

				break;
			}
		}

		System.out.println(Integer.toString(_numDocs) + " documents loaded "
		    + "with " + Long.toString(_totalTermFrequency) + " terms!");
	}

	@Override
	public DocumentIndexed getDoc(int docid) {
		return documents.get(docid);
	}

	@Override
	public Document nextDoc(Query query, int docid) {
		checkNotNull(docid, "docid can not be null!");

		List<String> queryTerms = query.terms;

		// Dynamic loading :)
		try {
			dynamicLoading(queryTerms);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		// Get the next docid which satisfies the query terms
		int nextDocid = nextCandidateDocid(queryTerms, docid);

		return nextDocid == -1 ? null : documents.get(nextDocid);
	}

	/**
	 * Return the next document which satisfy at least one of the term of the
	 * query...
	 * 
	 * @param query
	 * @param docid
	 * @return
	 */
	public Document nextDocLoose(Query query, int docid) {
		checkNotNull(docid, "docid can not be null!");
		List<String> queryTerms = query.terms;

		try {
			dynamicLoading(queryTerms);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		int minDocid = Integer.MAX_VALUE;
		for (String term : queryTerms) {
			int nextDocid = nextDocid(term, docid);
			if (nextDocid != -1) {
				minDocid = Math.min(minDocid, nextDocid);
			}
		}

		if (minDocid == Integer.MAX_VALUE) {
			return null;
		} else {
			return documents.get(minDocid);
		}
	}

	/**
	 * Return the next docid which satisfies the query terms, if none of such
	 * docid can be found, return -1. This function uses document at a time
	 * retrieval method.
	 * 
	 * @param queryTerms
	 *          A list of query terms
	 * @param docid
	 *          The document ID
	 * @return the next docid right after {@code docid} satisfying
	 *         {@code queryTerms} or -1 if no such document exists.
	 */
	private int nextCandidateDocid(List<String> queryTerms, int docid) {
		int largestDocid = -1;

		// For each query term's document ID list, find the largest docId because it
		// is a reasonable candidate.
		for (String term : queryTerms) {
			// Get the next document ID next to the current {@code docid} in the list
			int nextDocid = nextDocid(term, docid);
			if (nextDocid == -1) {
				// The next document ID does not exist...
				return -1;
			}
			largestDocid = Math.max(largestDocid, nextDocid);
		}

		// Check if the largest document ID satisfy all query terms.
		for (String term : queryTerms) {
			if (!hasDocid(term, largestDocid)) {
				// The largest docid does not satisfy this term, hence check the next
				return nextCandidateDocid(queryTerms, largestDocid);
			}
		}

		// If the satisfied docid has been found, return it.
		return largestDocid;
	}

	/**
	 * Return the next docid after the current one of the posting list, or -1 if
	 * no such docid exists.
	 * 
	 * @param term
	 *          The term...
	 * @param docid
	 *          The document ID
	 * @return the next document ID after the current document or -1 if no such
	 *         document ID exists.
	 */
	private int nextDocid(String term, int docid) {
		List<Integer> partialSkipPointers = vByteDecodingList(skipPointers
		    .get(term));
		int startOffsetOfPostingList = 0;
		int nextDocid = 0;
		int prevDocid = 0;

		if (docid >= 0) {
			// Get the start offset of the skip pointers...
			int startOffsetOfSkipPointers = getDocidPossibleSkipPointerStartOffset(
			    partialSkipPointers, term, docid + 1);
			if (startOffsetOfSkipPointers >= 0) {
				// Skip...
				prevDocid = partialSkipPointers.get(startOffsetOfSkipPointers);
				startOffsetOfPostingList = partialSkipPointers
				    .get(startOffsetOfSkipPointers + 1);
			}
		}

		nextDocid = scanPostingListForNextDocid(term, docid, prevDocid,
		    startOffsetOfPostingList);

		return nextDocid;
	}

	/**
	 * Check if the docid exists in the term's posting list.
	 * 
	 * @param term
	 * @param docid
	 * @return
	 */
	private boolean hasDocid(String term, int docid) {
		List<Integer> partialSkipPointers = vByteDecodingList(skipPointers
		    .get(term));
		int startOffsetOfSkipPointers = getDocidPossibleSkipPointerStartOffset(
		    partialSkipPointers, term, docid);
		int prevDocid = 0;
		int startOffsetOfPostingList = 0;

		if (startOffsetOfSkipPointers >= 0) {
			prevDocid = partialSkipPointers.get(startOffsetOfSkipPointers);
			startOffsetOfPostingList = partialSkipPointers
			    .get(startOffsetOfSkipPointers + 1);
		}
		return scanPostingListForDocid(term, docid, prevDocid,
		    startOffsetOfPostingList);
	}

	/**
	 * Search the skip pointers for the possible start offset for docid of the
	 * posting list.
	 * 
	 * @param term
	 * @param docid
	 * @return
	 */
	private int getDocidPossibleSkipPointerStartOffset(
	    List<Integer> partialSkipPointers, String term, int docid) {
		int size = partialSkipPointers.size();

		if (size == 0 || docid <= partialSkipPointers.get(0)) {
			return -1;
		}

		if (docid > partialSkipPointers.get(size - 2)) {
			return size - 2;
		}

		// Use binary search to find if the {@code docid} exists in the list
		int low = 0;
		int high = size / 2 - 1;

		while (low < high - 1) {
			int mid = low + (high - low) / 2;
			int midDocid = partialSkipPointers.get(mid * 2);
			if (midDocid == docid) {
				return (mid - 1) * 2;
			} else if (midDocid < docid) {
				low = mid;
			} else {
				high = mid;
			}
		}

		return low * 2;
	}

	/**
	 * Scan the posting list for the next docid right after {@code targetDocid}.
	 * It starts from the {@code startOffsetOfPostingList} with previous docid as
	 * {@code prevDocid}
	 * 
	 * @param term
	 *          the term.
	 * @param targetDocid
	 *          the target docid
	 * @param prevDocid
	 *          docid before the start {@code startOffsetOfPostingList}
	 * @param startOffsetOfPostingList
	 *          the start offset of the posting list
	 * @return the next docid or -1 if none exists
	 */
	private int scanPostingListForNextDocid(String term, int targetDocid,
	    int prevDocid, int startOffsetOfPostingList) {
		List<Byte> postingList = invertedIndex.get(term);
		List<Byte> byteList = new ArrayList<Byte>();
		int nextDocid = prevDocid;
		int i = startOffsetOfPostingList;

		// Get the first docid from the posting list
		if (targetDocid == -1) {
			while (!isEndOfNum(postingList.get(i))) {
				byteList.add(postingList.get(i++));
			}
			byteList.add(postingList.get(i));
			return vByteDecoding(byteList);
		}

		while (true) {
			// No more docid in the posting list
			if (i >= postingList.size()) {
				return -1;
			}

			// Get the docid
			while (!isEndOfNum(postingList.get(i))) {
				byteList.add(postingList.get(i++));
			}
			byteList.add(postingList.get(i++));
			nextDocid += vByteDecoding(byteList);
			byteList.clear();

			// If the docid is larger than the targetDocid, then the next docid is
			// found
			if (nextDocid > targetDocid) {
				return nextDocid;
			}

			// Get the occurs
			while (!isEndOfNum(postingList.get(i))) {
				byteList.add(postingList.get(i++));
			}
			byteList.add(postingList.get(i++));
			int occur = vByteDecoding(byteList);
			byteList.clear();

			// Skip all position data...
			for (int j = 0; j < occur; j++) {
				// Get the occurs
				while (!isEndOfNum(postingList.get(i))) {
					i++;
				}
				i++;
			}

			// Move on to the next docid...
		}
	}

	/**
	 * Scan the posting list for the {@code targetDocid}. It starts from the
	 * {@code startOffsetOfPostingList} with previous docid as {@code prevDocid}
	 * 
	 * @param term
	 *          the term.
	 * @param targetDocid
	 *          the target docid
	 * @param prevDocid
	 *          docid before the start {@code startOffsetOfPostingList}
	 * @param startOffsetOfPostingList
	 *          the start offset of the posting list
	 * @return true if the docid exist, otherwise false
	 */
	private boolean scanPostingListForDocid(String term, int targetDocid,
	    int prevDocid, int startOffsetOfPostingList) {
		List<Byte> postingList = invertedIndex.get(term);
		List<Byte> byteList = new ArrayList<Byte>();
		int nextDocid = prevDocid;
		int i = startOffsetOfPostingList;

		while (true) {
			// No more docid in the posting list
			if (i >= postingList.size()) {
				return false;
			}

			// Get the docid
			while (!isEndOfNum(postingList.get(i))) {
				byteList.add(postingList.get(i++));
			}
			byteList.add(postingList.get(i++));
			nextDocid += vByteDecoding(byteList);
			byteList.clear();
			// If the docid is larger than the targetDocid, then the next docid is
			// found
			if (nextDocid == targetDocid) {
				return true;
			} else if (nextDocid > targetDocid) {
				return false;
			}

			// Get the occurs
			while (!isEndOfNum(postingList.get(i))) {
				byteList.add(postingList.get(i++));
			}
			byteList.add(postingList.get(i++));
			int occur = vByteDecoding(byteList);
			byteList.clear();

			// Skip all position data...
			for (int j = 0; j < occur; j++) {
				// Get the occurs
				while (!isEndOfNum(postingList.get(i))) {
					i++;
				}
				i++;
			}

			// Move on to the next docid...
		}
	}

	/**
	 * Scan the posting list for the {@code targetDocid} offset of the posting
	 * list. It starts from the {@code startOffsetOfPostingList} with previous
	 * docid as {@code prevDocid}
	 * 
	 * @param term
	 *          the term.
	 * @param targetDocid
	 *          the target docid
	 * @param prevDocid
	 *          docid before the start {@code startOffsetOfPostingList}
	 * @param startOffsetOfPostingList
	 *          the start offset of the posting list
	 * @return the docid offset of the posting list
	 */
	private int scanPostingListForDocidOffset(String term, int targetDocid,
	    int prevDocid, int startOffsetOfPostingList) {
		List<Byte> postingList = invertedIndex.get(term);
		List<Byte> byteList = new ArrayList<Byte>();
		int offset = 0;
		int nextDocid = prevDocid;
		int i = startOffsetOfPostingList;

		while (true) {
			// No more docid in the posting list
			if (i >= postingList.size()) {
				return -1;
			}

			offset = i;

			// Get the docid
			while (!isEndOfNum(postingList.get(i))) {
				byteList.add(postingList.get(i++));
			}
			byteList.add(postingList.get(i++));
			nextDocid += vByteDecoding(byteList);
			byteList.clear();

			// If the docid is larger than the targetDocid, then the next docid is
			// found
			if (nextDocid == targetDocid) {
				return offset;
			} else if (nextDocid > targetDocid) {
				return -1;
			}

			// Get the occurs
			while (!isEndOfNum(postingList.get(i))) {
				byteList.add(postingList.get(i++));
			}
			byteList.add(postingList.get(i++));
			int occur = vByteDecoding(byteList);
			byteList.clear();

			// Skip all position data...
			for (int j = 0; j < occur; j++) {
				// Get the occurs
				while (!isEndOfNum(postingList.get(i))) {
					i++;
				}
				i++;
			}

			// Move on to the next docid...
		}
	}

	/**
	 * Get the docid offset of the posting list
	 * 
	 * @param term
	 *          the term
	 * @param docid
	 *          the docid
	 * @return the docid offset of the posting list
	 */
	private int getDocidOffset(String term, int docid) {
		List<Integer> partialSkipPointers = vByteDecodingList(skipPointers
		    .get(term));
		int startOffsetOfPostingList = 0;
		int nextDocid = 0;
		int prevDocid = 0;

		// Get the start offset of the skip pointers...
		int startOffsetOfSkipPointers = getDocidPossibleSkipPointerStartOffset(
		    partialSkipPointers, term, docid);
		if (startOffsetOfSkipPointers >= 0) {
			// Skip...
			prevDocid = partialSkipPointers.get(startOffsetOfSkipPointers);
			startOffsetOfPostingList = partialSkipPointers
			    .get(startOffsetOfSkipPointers + 1);
		}

		return scanPostingListForDocidOffset(term, docid, prevDocid,
		    startOffsetOfPostingList);
	}

	/**
	 * Return the next position for a term after {@code pos} in a document.
	 * 
	 * @param term
	 *          The term...
	 * @param docid
	 *          The document ID
	 * @param pos
	 *          The position of the term in the document
	 * @return the next position for the term in the document. If no more term in
	 *         the next, return -1.
	 */
	public int nextPos(String term, int docid, int pos) {
		checkNotNull(term, "term can not be null!");
		checkNotNull(docid, "docid can not be null!");
		checkNotNull(pos, "pos can not be null!");

		if (!invertedIndex.containsKey(term)) {
			return -1;
		}

		// Get the decompressed docidOffsetList
		List<Integer> docidOffsetList = vByteDecodingList(skipPointers.get(term));
		List<Byte> postingList = invertedIndex.get(term);
		List<Byte> tmpList = new ArrayList<Byte>();
		int offset = getDocidOffset(term, docid);

		int occur = -1;
		int currPos = 0;

		// Skip the doc id first
		while (!isEndOfNum(postingList.get(offset++))) {
		}

		// Get the occurs
		while (!isEndOfNum(postingList.get(offset))) {
			tmpList.add(postingList.get(offset++));
		}
		tmpList.add(postingList.get(offset++));
		occur = vByteDecoding(tmpList);
		tmpList.clear();

		for (int i = 0; i < occur; i++) {
			// Get the occurs
			while (!isEndOfNum(postingList.get(offset))) {
				tmpList.add(postingList.get(offset++));
			}
			tmpList.add(postingList.get(offset++));
			currPos += vByteDecoding(tmpList);
			tmpList.clear();

			if (currPos > pos) {
				return currPos;
			}
		}

		// No more term...
		return -1;
	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
		if (invertedIndex.containsKey(term)) {
			return invertedIndex.get(term).size();
		} else {
			return 0;
		}
	}

	@Override
	public int corpusTermFrequency(String term) {
		return _termCorpusFrequency.count(term);
	}

	@Override
	public int documentTermFrequency(String term, int docid) {

		int docTermFrequency = 0;
		int offset = getDocidOffset(term, docid);

		if (offset == -1) {
			return 0;
		}

		List<Byte> postingList = invertedIndex.get(term);
		List<Byte> tmpList = new ArrayList<Byte>();

		// Skip the doc id first
		while (!isEndOfNum(postingList.get(offset++))) {
		}

		// Get the occurs
		while (!isEndOfNum(postingList.get(offset))) {
			tmpList.add(postingList.get(offset++));
		}
		tmpList.add(postingList.get(offset++));
		docTermFrequency = vByteDecoding(tmpList);

		return docTermFrequency;
	}

	/**
	 * Merge all partial index files into a single file.
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void merge() throws IOException, ClassNotFoundException {
		String invertedIndexFileName = _options._indexPrefix + "/main.idx";
		RandomAccessFile raf = new RandomAccessFile(invertedIndexFileName, "rw");
		long currentPos = 0;
		int length = 0;

		/**************************************************************************
		 * Prepare merging...
		 *************************************************************************/
		File folder = new File(_options._indexPrefix);
		int numOfPartialIndex = 0;

		// Get the number of partial index file
		for (File f : folder.listFiles()) {
			if (f.getName().matches("^corpus[0-9]+\\.idx")) {
				numOfPartialIndex++;
			}
		}

		Kryo kryo = new Kryo();
		File[] files = new File[numOfPartialIndex];
		Input[] inputs = new Input[numOfPartialIndex];
		String[] terms = new String[numOfPartialIndex];
		int[] numOfPostingList = new int[numOfPartialIndex];

		// Initialize the files, inputs and
		// Then get the quantity of the posting list for each partial file
		for (int i = 0; i < numOfPartialIndex; i++) {
			for (File file : folder.listFiles()) {
				if (file.getName().matches(
				    "^corpus" + String.format("%03d", i + 1) + "\\.idx")) {
					terms[i] = "";
					files[i] = file;
					inputs[i] = new Input(new FileInputStream(file.getAbsolutePath()));
					numOfPostingList[i] = kryo.readObject(inputs[i], Integer.class);
					break;
				}
			}
		}

		/**************************************************************************
		 * Start merging...
		 *************************************************************************/
		while (hasMorePostingList(numOfPostingList)) {
			// Start to process the next posting list
			String outputTerm = "";
			List<Byte> outputPostingList = new ArrayList<Byte>();
			SortedSetMultimap<String, Integer> sortedTermAndFileIndex = TreeMultimap
			    .create(Ordering.natural(), Ordering.natural());

			// First read the next term from each of the partial index file if it has
			// more.
			for (int i = 0; i < numOfPartialIndex; i++) {
				if (terms[i].equals("") && numOfPostingList[i] > 0) {
					numOfPostingList[i]--;
					terms[i] = kryo.readObject(inputs[i], String.class);
				}
			}

			// Sort all next terms by alphabetical order
			// For two same terms, sort their file number to
			for (int i = 0; i < numOfPartialIndex; i++) {
				if (!terms[i].equals("")) {
					sortedTermAndFileIndex.put(terms[i], i);
				}
			}

			// Retrieve the first term and posting list according to the sorted result
			for (Map.Entry entry : sortedTermAndFileIndex.entries()) {
				outputTerm = (String) entry.getKey();
				for (int i : sortedTermAndFileIndex.asMap().get(outputTerm)) {
					outputPostingList.addAll(kryo.readObject(inputs[i], ArrayList.class));
					terms[i] = "";
				}
				break;
			}

			currentPos = raf.length();
			raf.seek(currentPos);
			raf.write(Bytes.toArray(outputPostingList));

			// Assume the posting list will not be too big...
			length = (int) (raf.length() - currentPos);
			metaData.put(outputTerm, new MetaPair(currentPos, length));
		}

		raf.close();

		/**************************************************************************
		 * Wrapping up...
		 *************************************************************************/
		for (File f : folder.listFiles()) {
			if (f.getName().matches("^corpus[0-9]+\\.idx")) {
				// Delete all partial index file
				f.delete();
			}
		}
	}

	/**
	 * Check if there's still at least one posting list needed to be merged.
	 * 
	 * @param numOfPostingList
	 *          the number of posting list of each partial index file.
	 * @return true if there's at least one posting list needed to be merged,
	 *         otherwise false.
	 */
	private boolean hasMorePostingList(int[] numOfPostingList) {
		for (int i : numOfPostingList) {
			if (i > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Dynamically load posting list at run time
	 * 
	 * @param query
	 *          the query terms
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void dynamicLoading(List<String> query) throws IOException,
	    ClassNotFoundException {
		String invertedIndexFileName = _options._indexPrefix + "/main.idx";
		RandomAccessFile raf = new RandomAccessFile(invertedIndexFileName, "r");
		boolean hasAlready = true;
		MetaPair metaPair;
		int count = 0;

		// Clean if not enough memory...
		if (invertedIndex.keys().size() > Util.MAX_INVERTED_INDEX_SIZE) {
			invertedIndex.clear();
		}

		for (String term : query) {
			if (!invertedIndex.containsKey(term)) {
				hasAlready = false;
			}
		}

		// All query terms are already loaded...
		if (hasAlready) {
			return;
		}

		System.out.println("Start dynamic loading...");
		long startTimeStamp = System.currentTimeMillis();

		for (String term : query) {
			// Load the posting list for a term if it's not already loaded.
			// Also check if it exists in the metaData, load it only if it exists.
			if (!invertedIndex.containsKey(term) && metaData.containsKey(term)) {
				metaPair = metaData.get(term);
				raf.seek(metaPair.getStartPos());
				byte[] postingListBytes = new byte[metaPair.getLength()];
				raf.readFully(postingListBytes);
				List<Byte> postingList = Bytes.asList(postingListBytes);
				invertedIndex.get(term).addAll(postingList);
				count++;
			}
		}

		raf.close();
		long duration = System.currentTimeMillis() - startTimeStamp;
		System.out.println("Compete dynamic loading. Loads " + count
		    + " posting lists and takes time " + Util.convertMillis(duration));
	}
}