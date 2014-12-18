### Group ID G06: yl1949, ws951, sy1288 :)

#Prerequisite

* Some of the codes may require JAVA 8.

* For using the ngram spell checker, 1GB memory may be required. (Default will be BKTree spell checker which requires less memory)

* Since serve mode require both index of web pages and news feed, we decide to put the index file of news feed along with the codes. (The corpora of news feed is too large to submit.)

* Ngram spell checker index mode may not be required since the default spell checker will be BKTree due the less memory consuming.

#Compile and Run

## Compile:

We used Ant for our project environment setting. Thus, you could easily to compile the project using command.
  <pre><code>
    homework3$ ant clean compile
  </code></pre>

## Run:

* We used several external library, So, whenever compile the files, mining, constructing index or serving, the path to the library’s files need to be include in classpath.

* We recommended to include the path manual when running the program, because we implemented a progress bar which Ant can not print it in real time.

### Examples:

  <pre><code>
    Minining:
    homework3$ java -cp ./bin/classes:./bin/lib/* edu.nyu.cs.cs2580.SearchEngine --mode=mining --options=conf/engine.conf
  </code></pre>

  <pre><code>
    Wiki Web Page Indexing:
    homework3$ java -cp ./bin/classes:./bin/lib/* edu.nyu.cs.cs2580.SearchEngine --mode=web_page_index --options=conf/engine.conf
  </code></pre>

  <pre><code>
    News Feed Indexing:
    homework3$ java -cp ./bin/classes:./bin/lib/* edu.nyu.cs.cs2580.SearchEngine --mode=news_feed_index --options=conf/engine.conf
  </code></pre>

  <pre><code>
    Ngram spell checker Indexing (Require both meta.idx from web pages corpora and news feed corpora):
    homework3$ java -cp ./bin/classes:./bin/lib/* edu.nyu.cs.cs2580.SearchEngine --mode=ngram-spell-check-index --options=conf/engine.conf
  </code></pre>

  <pre><code>
    Serving:
    homework3$ java -cp ./bin/classes:./bin/lib/* edu.nyu.cs.cs2580.SearchEngine --mode=serve --port=25806 --options=conf/engine.conf
  </code></pre>

  <pre><code>
    Bhattacharyya:
    homework3$ java -cp ./bin/classes:./bin/lib/* edu.nyu.cs.cs2580.Bhattacharyya prf.tsv qsim.tsv
  </code></pre>

  <pre><code>
    Spearman:
    homework3$ java -cp ./bin/classes:./bin/lib/* edu.nyu.cs.cs2580.Spearman data/pageRank/pageRanks.g6 data/numView/numViews.g6
  </code></pre>

  <pre><code>
    Spell checker evaluator:
    homework3$ java -cp ./bin/classes:./bin/lib/* edu.nyu.cs.cs2580.evaluator.SpellCorrectionEvaluator
  </code></pre>
  
### News vertical search:
  <pre><code>
    Crawler will keep running and crawls every 2 hours:
    ant crawl
    
    To generate the news input for MALLET (with window size of 2 weeks ):
    ant interdata
    
    Then run Shell Script to call MALLET to generate topics:
    ./topic_model.sh
    
    To get the metrics of topics, run:
    ant analyze
  </code></pre>

#Implementation Detail

## Text Processing
- Using Snowball Porter Stemmer Java library for aggressive text stemming, all library files are packaged into the "edu.nyu.cs.cs2580.Snowball".

- Using Apache Lucene KStmmer Java Library for text stemming with validation, all library files are packaged into the "edu.nyu.cs.cs2580.KStemmer".

- Using JSoup for HTML extracting.

- Using JFlex for text analysis. All regular expressions are based on Word Boundaries defined on http://www.unicode.org/reports/tr29/.

## Index Constructing
- We mainly use compressed inverted index to index the web pages and news feed.

- We use boilerpipe to extract main content from the html pages.

- We implemented extent list, which help us to know whether a term exists in the title or not. (Current only two field: main content and title.)

- We set a memory threshold for inverted index and document term frequency map and will split it into disk once the threshold is met. Later we will merge them together...

- The merge process will produce only one file for inverted index called corpus.idx and another file for document term frequency map called documents.idx.

- The rest will be serialized to a single file called meta.idx.

- According to different mode, we are able to process different kinds of documents by using the same index class with different document processor class and document class.

## Ngram spell checker index constructing

- Ngram spell checker need to construct its own index based on the corpus index meta data which including its dictionary, term frequency etc...

- It's a separate mode because it takes some time for constructing and can be optional during the serving mode. (Currently are disabled, and use another spell checker to speed the load time at the beginning.)

## Serving
- We load the index object first.

- For ngram spell checker to work, we also need to load its index unless it's disabled in the query handlers.

- BKTree spell checker will start to construct the tree... (Which loads faster than ngram, so currently is the default spell checker)

- For ngram spell checker to work, we also need to load its index

- The inverted index (posting list) and document term frequency are not loaded at beginning, instead we load them at run time if we need them.

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
