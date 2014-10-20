Group ID G06: yl1949, ws951, sy1288

#Compile and Run

## Shell Script:

### In our script, we already set the JVM max runtime memory as 512M, if you want to change the limit, feel free to edit the file.

### Construct index:
  <pre><code>
    $ chmod u+x SearchEngine.sh
    $ ./SearchEngine.sh ./SearchEngine.sh \ -—mode=index --options=conf/engine.conf 
  </code></pre>
    
### Serving index:
  <pre><code>
    $ ./SearchEngine.sh \--mode=serve --port=[port] --options=conf/engine.conf
  </code></pre>    

## Manual:

### We used several external library, So, whenever compile the files,  constructing index or serving, the path to the library’s files need to be include in classpath.  
  
### example:

  <pre><code>
    homework2$ mkdir ./bin
  </code></pre>
  <pre><code>
    $ cp -r ./lib ./bin
  </code></pre>
  <pre><code>
    $ export CLASSPATH=./bin:./bin/lib/*
  </code></pre>
  <pre><code>
    $  javac -d ./bin  ./src/edu/nyu/cs/cs2580/*/*.java \ ./src/edu/nyu/cs/cs2580/*.java -nowarn
  </code></pre>
  <pre><code>
    $ java -Xmx512m edu.nyu.cs.cs2580.SearchEngine \ --mode=index --options=conf/engine.conf
  </code></pre>
  <pre><code>
    $ java -Xmx512m edu.nyu.cs.cs2580.SearchEngine \ --mode=serve --port=25806 --options=conf/engine.conf
  </code></pre>
    
    
#Implementation Detail

## Text Processing
- Using Snowball Porter Stemmer Java library for aggressive text stemming, all library files are packaged into the "edu.nyu.cs.cs2580.Snowball".
  
- Using Apache Lucene KStmmer Java Library for text stemming with validation, all library files are packaged into the "edu.nyu.cs.cs2580.KStemmer". 
  
- Using JSoup for HTML extracting.
  
- Using JFlex for text analysis. All regular expressions are based on Word Boundaries defined on http://www.unicode.org/reports/tr29/.

## Constructing
- We set a memory threshold for inverted index and will write it into disk once the threshold is met.
  
- After processing the corpus, we will merge all temporary written inverted index files according to alphabetical order.
  
- The merge process will produce many small files each host certain number of posting list by order.
  
- The rest will be serialized to a single file.
  
## Serving
- We load the index object first.
  
- The inverted index is not loaded at first.
  
- Every time a query comes in, we will check whether if it exists in the inverted index, if not, we will load it dynamically.
  
- We will load a single posting list every time. (Though we have to read the whole file first, there's the reason they are so small..)
  
- If the inverted index hits a threshold, then it will be cleared for now... (Due to our implementation, it rarely happen...)