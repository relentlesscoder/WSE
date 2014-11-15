package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

/**
 * This is the main entry class for the Search Engine.
 * <p/>
 * Usage (must be running from the parent directory of src): 0) Compiling javac
 * src/edu/nyu/cs/cs2580/*.java 1) Indexing java -cp src
 * edu.nyu.cs.cs2580.SearchEngine \ --mode=index --options=conf/engine.conf 2)
 * Serving java -cp src -Xmx256m edu.nyu.cs.cs2580.SearchEngine \ --mode=serve
 * --port=[port] --options=conf/engine.conf 3) Searching
 * http://localhost:[port]/search?query=web&ranker=fullscan
 * 
 * @author congyu
 * @author fdiaz
 * @CS2580 You must ensure your program runs with maximum heap memory size
 *         -Xmx512m. You must use a port number 258XX, where XX is your group
 *         number.
 *         <p/>
 *         Students do not need to change this class except to add server
 *         options.
 */
public class SearchEngine {

	public static Options OPTIONS = null;
	public static Mode MODE = Mode.NONE;
	public static int PORT = -1;

	/**
	 * Prints {@code msg} and exits the program if {@code condition} is false.
	 */
	public static void Check(boolean condition, String msg) {
		if (!condition) {
			System.err.println("Fatal error: " + msg);
			System.exit(-1);
		}
	}

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
		Check(MODE == Mode.SERVE || MODE == Mode.INDEX,
		    "Must provide a valid mode: serve or index!");
		Check(MODE != Mode.SERVE || PORT != -1,
		    "Must provide a valid port number (258XX) in serve mode!");
		Check(OPTIONS != null, "Must provide options!");
	}

	private static void startIndexing() throws IOException {
		Indexer indexer = Indexer.Factory.getIndexerByOption(SearchEngine.OPTIONS);
		Check(indexer != null, "Indexer " + SearchEngine.OPTIONS._indexerType
		    + " not found!");
		indexer.constructIndex();
	}

	private static void startServing() throws IOException, ClassNotFoundException {
		// Create the handler and its associated indexer.
		Indexer indexer = Indexer.Factory.getIndexerByOption(SearchEngine.OPTIONS);
		Check(indexer != null, "Indexer " + SearchEngine.OPTIONS._indexerType
		    + " not found!");
		indexer.loadIndex();
		QueryHandler queryHandler = new QueryHandler(SearchEngine.OPTIONS, indexer);
		HtmlHandler htmlHandler = new HtmlHandler(SearchEngine.OPTIONS);

		// Establish the serving environment
		InetSocketAddress addr = new InetSocketAddress(SearchEngine.PORT);
		HttpServer server = HttpServer.create(addr, -1);
		server.createContext("/search", queryHandler);
		server.createContext("/", htmlHandler);
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
		System.out.println("Listening on port: "
		    + Integer.toString(SearchEngine.PORT));
	}

	// /// Main functionalities start

	public static void main(String[] args) {
		try {
			SearchEngine.parseCommandLine(args);
			switch (SearchEngine.MODE) {
			case INDEX:
				startIndexing();
				break;
			case SERVE:
				startServing();
				break;
			default:
				Check(false, "Wrong mode for SearchEngine!");
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Running mode of the search engine.
	 */
	public static enum Mode {
		NONE, INDEX, SERVE,
	}

	/**
	 * Stores all the options and configurations used in our search engine. For
	 * simplicity, all options are publicly accessible.
	 */
	public static class Options {
		// The parent path where the corpus resides.
		// HW1: We have only one file, corpus.csv.
		// HW2: We have a partial Wikipedia dump.
		public String _corpusPrefix = null;

		// The parent path where the constructed index resides.
		// HW1: n/a
		// HW2: This is where the index is built into and loaded from.
		public String _indexPrefix = null;

		// The specific Indexer to be used.
		public String _indexerType = null;

		// Additional group specific configuration can be added below.

		public String _searchTemplate = null;

		public String _resultTemplate = null;

		/**
		 * Constructor for options.
		 * 
		 * @param optionFile
		 *          where all the options must reside
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
					Check(false, "Wrong option: " + line);
				}
				options.put(vals[0].trim(), vals[1].trim());
			}
			reader.close();

			// Populate global options.
			_corpusPrefix = options.get("corpus_prefix");
			Check(_corpusPrefix != null, "Missing option: corpus_prefix!");
			_indexPrefix = options.get("index_prefix");
			Check(_indexPrefix != null, "Missing option: index_prefix!");

			// Populate specific options.
			_indexerType = options.get("indexer_type");
			Check(_indexerType != null, "Missing option: indexer_type!");

			_searchTemplate = options.get("search_template");
			Check(_searchTemplate != null, "Missing option: search_template!");

			_resultTemplate = options.get("result_template");
			Check(_resultTemplate != null, "Missing option: result_template!");
		}
	}
}
