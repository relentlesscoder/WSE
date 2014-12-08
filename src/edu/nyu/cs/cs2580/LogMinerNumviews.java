package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.nyu.cs.cs2580.SearchEngine.Options;
import edu.nyu.cs.cs2580.Utils.Util;

/**
 * @CS2580: Implement this class for HW3.
 */
public class LogMinerNumviews extends LogMiner {
  // Key: Document ID
  // Value: File name
  BiMap<Integer, String> docidMap;

  // Key: Document ID
  // Value: Number of views
  Map<Integer, Integer> docNumView;

  public LogMinerNumviews(Options options) {
    super(options);
    docidMap = HashBiMap.create();
    docNumView = new HashMap<Integer, Integer>();
  }

  /**
   * This function processes the logs within the log directory as specified by
   * the {@link _options}. The logs are obtained from Wikipedia dumps and have
   * the following format per line: [language]<space>[article]<space>[#views].
   * Those view information are to be extracted for documents in our corpus and
   * stored somewhere to be used during indexing.
   *
   * Note that the log contains view information for all articles in Wikipedia
   * and it is necessary to locate the information about articles within our
   * corpus.
   *
   * @throws IOException
   */
  @Override
  public void compute() throws IOException {
    // Get the corpus file
    File corpusFolder = new File(_options._corpusPrefix);
    //add filter to exclude hidden files
    FilenameFilter filenameFilter = new FilenameFilter() {
      @Override
      public boolean accept(File file, String name) {
        return !name.startsWith(".");
      }
    };
    File[] corpusFiles = corpusFolder.listFiles(filenameFilter);

    for (int docid = 0; docid < corpusFiles.length; docid++) {
      docidMap.put(docid, corpusFiles[docid].getName());
    }

    // Get the log file
    File logFolder = new File(_options._logPrefix);
    File[] logFiles = logFolder.listFiles();
    
    // Mining the logs
    for (File logFile : logFiles) {
      FileInputStream fis = new FileInputStream(logFile);
      BufferedReader br = new BufferedReader(new InputStreamReader(fis));

      String line = null;
      while ((line = br.readLine()) != null) {
        String[] tokens = line.split(" ");

        if (!isValidLogEntry(tokens)) {
          continue;
        }

        if(docidMap.inverse().containsKey(tokens[1])){
          docNumView.put(docidMap.inverse().get(tokens[1]), Integer.parseInt(tokens[2]));
        }
      }
    }

    for (int docid = 0; docid < corpusFiles.length; docid++){
      if(!docNumView.containsKey(docid)){
        docNumView.put(docid, 0);
      }
    }

    /**************************************************************************
     * First check if the folder exists... If not, create one.
     *************************************************************************/
    File outputFolder = new File(_options._numviewPrefix);
    if (!(outputFolder.exists() && outputFolder.isDirectory())) {
      outputFolder.mkdir();
    }

    /**************************************************************************
     * First delete the numViews file if there exist an old one....
     *************************************************************************/
    for (File file : outputFolder.listFiles()) {
      file.delete();
    }

    /**************************************************************************
     * Writing to file....
     *************************************************************************/
    String filePath = _options._numviewPrefix + "/numViews.g6";

    Kryo kryo = new Kryo();
    Output output = new Output(new FileOutputStream(filePath));

    kryo.writeObject(output, docNumView);
    output.close();
  }

  /**
   * During indexing mode, this function loads the NumViews values computed
   * during mining mode to be used by the indexer.
   * 
   * @throws IOException
   */
  @Override
  public Object load() throws IOException {
    System.out.println("Loading using " + this.getClass().getName());

    String filePath = _options._numviewPrefix + "/numViews.g6";
    Kryo kryo = new Kryo();
    Input input = new Input(new FileInputStream(filePath));

    docNumView.clear();
    docNumView = kryo.readObject(input, HashMap.class);

    input.close();

    return docNumView;
  }

  private boolean isValidLogEntry(String[] tokens) {
    if (tokens.length != 3 || !Util.isNumber(tokens[2])) {
      return false;
    }

    return true;
  }
}
