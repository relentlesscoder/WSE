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
  	  homework3$ java -cp ./bin/classes:./bin/lib/* edu.nyu.cs.cs2580.Bhattacharyya prf.tsv qsim.tsv
  	</code></pre>
  	<pre><code>
        homework3$ java -cp ./bin/classes:./bin/lib/* edu.nyu.cs.cs2580.Spearman data/pageRank/pageRanks.g6 data/numView/numViews.g6
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
Spearman coefficient: 0.4468566840544932
Damping factor: 0.9
Iteration: 2

Explanation:
According to Larry Page and Sergey Brin's original white paper from Stanford, The Anatomy of a Large-Scale Hypertextual Web Search Engine, which became the blueprints for Google. PageRank is based
on the random surfer model. Essentially, the damping factor is a decay factor. What it represents is the chance that a user will stopping clicking links and get bored with the current page and then request another
random page (as with directly typing in a new URL rather than following a link on the current page). This was originally set to about 85% or 0.85. If the damping factor is 85% then there is assumed to be about a
15% chance that a typical users won't follow any links on the page and instead navigate to a new random URL. And the computation of PageRank is a iterative process,  According to Lawrence Page and Sergey Brin,
about 100 iterations are necessary to get a good approximation of the PageRank values of the whole web. So in our project, we selected the combination of 0.9 as the damping factor and 2 iterations and we
obtained the above Spearman coefficient value.

The full results are below:
0.9  2   0.4468566840544932
0.9  1   0.4265427961791642
0.1  2   0.36090445016905776
0.1  1   0.365476077049136