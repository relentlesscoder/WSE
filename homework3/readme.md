Group ID G06: yl1949, ws951, sy1288 :)

#Compile and Run

## Compile:

### We used Ant for our project environment setting. Thus, you could easily to compile the project using command:
  <pre><code>
    homework3$ ant clean compile
  </code></pre>

## Run:

### We used several external library, So, whenever compile the files, mining, constructing index or serving, the path to the library’s files need to be include in classpath.

### We recommended to include the path manual when running the program, because we implemented a progress bar which Ant can not print it in real time.

### example:
  <pre><code>
    homework3$ java -cp ./bin/classes:./bin/lib/* edu.nyu.cs.cs2580.SearchEngine --mode=mining --options=conf/engine.conf
	</code></pre>
	<pre><code>
	  homework3$ java -cp ./bin/classes:./bin/lib/* edu.nyu.cs.cs2580.SearchEngine --mode=index --options=conf/engine.conf
	</code></pre>
	<pre><code>
	  homework3$ java -cp ./bin/classes:./bin/lib/* edu.nyu.cs.cs2580.SearchEngine --mode=serve --port=25806 --options=conf/engine.conf
	</code></pre>
	<pre><code>
	  homework3$ java -cp ./bin/classes:./bin/lib/* Bhattacharyya prf.tsv qsim.tsv
	</code></pre>

#Implementation Detail

## Text Processing
- Using Snowball Porter Stemmer Java library for aggressive text stemming, all library files are packaged into the "edu.nyu.cs.cs2580.Snowball".

- Using Apache Lucene KStmmer Java Library for text stemming with validation, all library files are packaged into the "edu.nyu.cs.cs2580.KStemmer".

- Using JSoup for HTML extracting.

- Using JFlex for text analysis. All regular expressions are based on Word Boundaries defined on http://www.unicode.org/reports/tr29/.

## Constructing
- Apart from the inverted index, this time we also keep a data structure for all document's term frequency.

- We set a memory threshold for inverted index and document term frequency map and will write it into disk once the threshold is met.

- After processing the corpus, we will merge all temporary written files.

- The merge process will produce only one file for inverted index called corpus.idx and another file for document term frequency map called documents.idx.

- The rest will be serialized to a single file called main.idx.

## Serving
- We load the index object first, then load the pageRank.g6 and numView.g6 file.

- The inverted index and document term frequency are not loaded at first.

- Every time a query comes in, we will check whether if it exists in the inverted index, if not, we will load it dynamically.

- Every time a operation request all term frequency of a specific document, it will be loaded first from disk.

- If the size of inverted index or document term frequency hits a threshold, then it will be cleared first. (Priority clean is not implemented yet... Which will first pop the least popular one)

## Note
