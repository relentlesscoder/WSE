package edu.nyu.cs.cs2580;

import com.sun.net.httpserver.HttpServer;
import edu.nyu.cs.cs2580.index.Indexer;
import edu.nyu.cs.cs2580.handler.HtmlHandler;
import edu.nyu.cs.cs2580.handler.PrfHandler;
import edu.nyu.cs.cs2580.handler.QueryHandler;
import edu.nyu.cs.cs2580.minning.CorpusAnalyzer;
import edu.nyu.cs.cs2580.minning.LogMiner;
import edu.nyu.cs.cs2580.utils.Util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * This is the main entry class for the Search Engine.
 * <p>
 * Usage (must be running from the parent directory of src): 0) Compiling javac
 * src/edu/nyu/cs/cs2580/*.java 1) Mining java -cp src
 * edu.nyu.cs.cs2580.SearchEngine \ --mode=mining --options=conf/engine.conf 2)
 * Indexing java -cp src edu.nyu.cs.cs2580.SearchEngine \ --mode=index
 * --options=conf/engine.conf 3) Serving java -cp src -Xmx256m
 * edu.nyu.cs.cs2580.SearchEngine \ --mode=serve --port=[port]
 * --options=conf/engine.conf 4) Searching
 * http://localhost:[port]/search?query=web&ranker=fullscan
 *
 * @author congyu
 * @author fdiaz
 * @CS2580: You must ensure your program runs with maximum heap memory size
 * -Xmx512m. You must use a port number 258XX, where XX is your group
 * number.
 * <p>
 * Students do not need to change this class except to add server
 * options.
 */
public class SearchEngine {

  public enum CORPUS_TYPE {
    WEB_PAGE_CORPUS,
    NEWS_FEED_CORPUS
  }

  /**
   * Stores all the options and configurations used in our search engine. For
   * simplicity, all options are publicly accessible.
   */
  public static class Options {
    // The parent path where the web page corpus resides.
    public String _corpusPrefix = null;

    // The parent path where the news feed corpus resides.
    public String _newsPrefix = null;

    // The parent path where the log date reside.
    public String _logPrefix = null;

    // The parent path where the page rank data resides
    public String _pagerankPrefix = null;

    // The parent path where the number of views data resides
    public String _numviewPrefix = null;

    // The parent path where the constructed index resides.
    public String _indexPrefix = null;

    // The specific Indexer to be used.
    public String _indexerType = null;

    // The specific CorpusAnalyzer to be used.
    public String _corpusAnalyzerType = null;

    // The specific LogMiner to be used.
    public String _logMinerType = null;

    // The specific damping factor for page ranking
    public double _dampingFactor = 0;

    // The specific iteration times for page ranking
    public int _iteration_times = 0;

    // The parent path where the search template resides
    public String _searchTemplate = null;

    // The parent path where the result template resides
    public String _resultTemplate = null;

    // The parent path where the spell check index resides
    public String _indexSpell = null;

    /**
     * Constructor for options.
     *
     * @param optionsFile where all the options must reside
     * @throws IOException
     */
    public Options(String optionsFile) throws IOException {
      // Read options from the file.
      BufferedReader reader = new BufferedReader(new FileReader(optionsFile));
      Map<String, String> options = new HashMap<String, String>();
      String line = null;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        String[] vals = line.split(":", 2);
        if (vals.length < 2) {
          reader.close();
          Util.Check(false, "Wrong option: " + line);
        }
        options.put(vals[0].trim(), vals[1].trim());
      }
      reader.close();

      // Populate global options.
      _corpusPrefix = options.get("corpus_prefix");
      Util.Check(_corpusPrefix != null, "Missing option: corpus_prefix!");

      _newsPrefix = options.get("news_prefix");
      Util.Check(_newsPrefix != null, "Missing option: news_prefix!");

      _logPrefix = options.get("log_prefix");
      Util.Check(_logPrefix != null, "Missing option: log_prefix!");

      _pagerankPrefix = options.get("pagerank_prefix");
      Util.Check(_pagerankPrefix != null, "Missing option: pagerank_prefix!");

      _numviewPrefix = options.get("numview_prefix");
      Util.Check(_numviewPrefix != null, "Missing option: numview_prefix!");

      _indexPrefix = options.get("index_prefix");
      Util.Check(_indexPrefix != null, "Missing option: index_prefix!");

      // Populate specific options.
      _indexerType = options.get("indexer_type");
      Util.Check(_indexerType != null, "Missing option: indexer_type!");

      _corpusAnalyzerType = options.get("corpus_analyzer_type");
      Util.Check(_corpusAnalyzerType != null,
          "Missing option: corpus_analyzer_type!");

      _logMinerType = options.get("log_miner_type");
      Util.Check(_logMinerType != null, "Missing option: log_miner_type!");

      String dampingValue = options.get("damping_factor");
      Util.Check(dampingValue != null, "Missing option: damping_factor!");
      _dampingFactor = Double.parseDouble(dampingValue);

      String iterationTimes = options.get("iteration_times");
      Util.Check(iterationTimes != null, "Missing option: iteration_times!");
      _iteration_times = Integer.parseInt(iterationTimes);

      _searchTemplate = options.get("search_template");
      Util.Check(_searchTemplate != null, "Missing option: search_template!");

      _resultTemplate = options.get("result_template");
      Util.Check(_resultTemplate != null, "Missing option: result_template!");

      _indexSpell = options.get("index_spell");
      Util.Check(_indexSpell != null, "Missing option: index_spell!");
    }
  }

  public static Options OPTIONS = null;

  /**
   * Running mode of the search engine.
   */
  public static enum Mode {
    NONE, MINING, WEB_PAGE_INDEX, NEWS_FEED_INDEX, SERVE,
  }

  ;

  public static Mode MODE = Mode.NONE;

  public static int PORT = -1;

  private static void parseCommandLine(String[] args) throws IOException,
      NumberFormatException {
    for (String arg : args) {
      String[] vals = arg.split("=", 2);
      String key = vals[0].trim();
      String value = vals[1].trim();
      if (key.equals("--mode") || key.equals("-mode")) {
        try {
          MODE = Mode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
          // Ignored, error msg will be printed below.
        }
      } else if (key.equals("--port") || key.equals("-port")) {
        PORT = Integer.parseInt(value);
      } else if (key.equals("--options") || key.equals("-options")) {
        OPTIONS = new Options(value);
      }
    }
    Util.Check(MODE == Mode.SERVE || MODE == Mode.WEB_PAGE_INDEX || MODE == Mode.NEWS_FEED_INDEX || MODE == Mode.MINING,
        "Must provide a valid mode: serve or index or mining!");
    Util.Check(MODE != Mode.SERVE || PORT != -1,
        "Must provide a valid port number (258XX) in serve mode!");
    Util.Check(OPTIONS != null, "Must provide options!");
  }

  // Main functionalities start

  private static void startMining() throws IOException,
      NoSuchAlgorithmException {
    CorpusAnalyzer analyzer = CorpusAnalyzer.Factory
        .getCorpusAnalyzerByOption(SearchEngine.OPTIONS);
    Util.Check(analyzer != null, "Analyzer "
        + SearchEngine.OPTIONS._corpusAnalyzerType + " not found!");

    long totalStartTimeStamp, startTimeStamp, duration;
    totalStartTimeStamp = System.currentTimeMillis();

    /**************************************************************************
     * Start preparing....
     *************************************************************************/
    System.out.println("Start preparing...");
    startTimeStamp = System.currentTimeMillis();

    analyzer.prepare();

    duration = System.currentTimeMillis() - startTimeStamp;
    System.out.println("Preparing takes time: " + Util.convertMillis(duration));

    /**************************************************************************
     * Start computing....
     *************************************************************************/
    System.out.println("Start computing...");
    startTimeStamp = System.currentTimeMillis();

    analyzer.compute();

    duration = System.currentTimeMillis() - startTimeStamp;
    System.out.println("Computing takes time: " + Util.convertMillis(duration));

    /**************************************************************************
     * Start computing logs....
     *************************************************************************/
    LogMiner miner = LogMiner.Factory.getLogMinerByOption(SearchEngine.OPTIONS);
    Util.Check(miner != null, "Miner " + SearchEngine.OPTIONS._logMinerType
        + " not found!");

    System.out.println("Start computing logs...");
    startTimeStamp = System.currentTimeMillis();

    miner.compute();

    duration = System.currentTimeMillis() - startTimeStamp;
    System.out.println("Computing logs takes time: " + Util.convertMillis(duration));


    duration = System.currentTimeMillis() - totalStartTimeStamp;
    System.out.println("Total time takes: " + Util.convertMillis(duration));
  }

  private static void startIndexing(CORPUS_TYPE corpusType) throws IOException {
    Indexer indexer = Indexer.Factory.getIndexerByOption(SearchEngine.OPTIONS, corpusType);
    Util.Check(indexer != null, "Indexer " + SearchEngine.OPTIONS._indexerType
        + " not found!");
    indexer.constructIndex();
  }

  private static void startServing() throws IOException, ClassNotFoundException {
    // Create the handler and its associated indexer.
    // TODO: This only create the indexer for normal web page corpus, news are not included.
    CORPUS_TYPE corpusType = CORPUS_TYPE.WEB_PAGE_CORPUS;
    Indexer indexer = Indexer.Factory.getIndexerByOption(SearchEngine.OPTIONS, corpusType);
    Util.Check(indexer != null, "Indexer " + SearchEngine.OPTIONS._indexerType
        + " not found!");
    indexer.loadIndex();
    QueryHandler queryHandler = new QueryHandler(SearchEngine.OPTIONS, indexer);
    PrfHandler prfHandler = new PrfHandler(SearchEngine.OPTIONS, indexer);
    HtmlHandler htmlHandler = new HtmlHandler(SearchEngine.OPTIONS);

    // Establish the serving environment
    InetSocketAddress addr = new InetSocketAddress(SearchEngine.PORT);
    HttpServer server = HttpServer.create(addr, -1);
    server.createContext("/search", queryHandler);
    server.createContext("/prf", prfHandler);
    server.createContext("/", htmlHandler);
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
    System.out.println("Listening on port: "
        + Integer.toString(SearchEngine.PORT));
  }

  public static void main(String[] args) {
    try {
      SearchEngine.parseCommandLine(args);
      CORPUS_TYPE corpusType;
      switch (SearchEngine.MODE) {
        case MINING:
          startMining();
          break;
        case WEB_PAGE_INDEX:
          corpusType = CORPUS_TYPE.WEB_PAGE_CORPUS;
          startIndexing(corpusType);
          break;
        case NEWS_FEED_INDEX:
          corpusType = CORPUS_TYPE.NEWS_FEED_CORPUS;
          startIndexing(corpusType);
          break;
        case SERVE:
          startServing();
          break;
        default:
          Util.Check(false, "Wrong mode for SearchEngine!");
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
