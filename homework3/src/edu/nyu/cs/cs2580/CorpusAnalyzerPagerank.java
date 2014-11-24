package edu.nyu.cs.cs2580;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import edu.nyu.cs.cs2580.SearchEngine.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.io.*;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @CS2580: Implement this class for HW3.
 */
public class CorpusAnalyzerPagerank extends CorpusAnalyzer {

  // Page ranks.
  // Key: Document ID
  // Value: Page rank score
  Map<Integer, Double> _pageRanks;

  // Directed graph.
  // Vertex: Page/Document
  // Edge: Out coming or In coming links.
  private DirectedGraph<Integer, PageGraphEdge> _pageGraph;

  public CorpusAnalyzerPagerank(Options options) {
    super(options);
    _pageGraph = new DefaultDirectedGraph<Integer, PageGraphEdge>(PageGraphEdge.class);
    _pageRanks = new HashMap<Integer, Double>();
  }

  /**
   * This function processes the corpus as specified inside {@link _options}
   * and extracts the "internal" graph structure from the pages inside the
   * corpus. Internal means we only store links between two pages that are both
   * inside the corpus.
   * 
   * Note that you will not be implementing a real crawler. Instead, the corpus
   * you are processing can be simply read from the disk. All you need to do is
   * reading the files one by one, parsing them, extracting the links for them,
   * and computing the graph composed of all and only links that connect two
   * pages that are both in the corpus.
   * 
   * Note that you will need to design the data structure for storing the
   * resulting graph, which will be used by the {@link compute} function. Since
   * the graph may be large, it may be necessary to store partial graphs to
   * disk before producing the final graph.
   *
   * @throws IOException
   */
  @Override
  public void prepare() throws IOException {
    System.out.println("Preparing " + this.getClass().getName());
    File folder  = new File(_options._corpusPrefix);
    //add filter to exclude hidden files
    FilenameFilter filenameFilter = new FilenameFilter() {
      @Override
      public boolean accept(File file, String name) {
        return !name.startsWith(".");
      }
    };
    File[] files = folder.listFiles(filenameFilter);
    ProgressBar progressBar = new ProgressBar();

    checkNotNull(files, "No files found in: %s", folder.getPath());

    Map<String, FileMetaData> nameDocIdMap = new HashMap<String, FileMetaData>();
    ArrayList redirectList = new ArrayList<Integer>();

    for (int docid = 0; docid < files.length; docid++) {
      checkNotNull(files[docid], "File can not be null!");
      FileMetaData fileMetaData = new FileMetaData();
      fileMetaData.setDocId(docid);
      String redirectUrl = tryGetRedirectUrl(files[docid]);
      if(redirectUrl != null && !redirectUrl.isEmpty()){
        fileMetaData.setIsRedirectPage(true);
        fileMetaData.setRedirectUrl(redirectUrl);
        redirectList.add(docid);
      }
      nameDocIdMap.put(files[docid].getName(), fileMetaData);
      progressBar.update(docid, files.length * 2);
    }

    for (int docid = 0; docid < files.length; docid++) {
      checkNotNull(files[docid], "File can not be null!");
      if(!_pageGraph.containsVertex(docid) && !redirectList.contains(docid)){
        _pageGraph.addVertex(docid);
      }
      HeuristicLinkExtractor linkExtractor = new HeuristicLinkExtractor(files[docid]);
      String url = linkExtractor.getNextInCorpusLinkTarget();
      while(url != null){
        if(isValidInternalUrl(url) && nameDocIdMap.containsKey(url)){
          FileMetaData linkToDoc = nameDocIdMap.get(url);

          int linkToDocId = 0;
          if(!linkToDoc.getIsRedirectPage()){
            linkToDocId = linkToDoc.getDocId();
          }
          else{
            do {
              linkToDoc = nameDocIdMap.get(linkToDoc.getRedirectUrl());
            }
            while (linkToDoc.getIsRedirectPage());
            if(linkToDoc != null){
              linkToDocId = linkToDoc.getDocId();
            }
          }

          if(linkToDocId > 0){
            // add target vertex if it is not in the graph
            if(!_pageGraph.containsVertex(linkToDocId)){
              _pageGraph.addVertex(linkToDocId);
            }
            // only add edge once
            if(!_pageGraph.containsEdge(docid, linkToDocId)){
              _pageGraph.addEdge(docid, linkToDocId);
            }
          }
        }
        url = linkExtractor.getNextInCorpusLinkTarget();
      }

      // Update the progress bar after processing a document :)
      progressBar.update(files.length + docid, files.length * 2);
    }
  }

  /**
   * This function computes the PageRank based on the internal graph generated
   * by the {@link prepare} function, and stores the PageRank to be used for
   * ranking.
   * 
   * Note that you will have to store the computed PageRank with each document
   * the same way you do the indexing for HW2. I.e., the PageRank information
   * becomes part of the index and can be used for ranking in serve mode. Thus,
   * you should store the whatever is needed inside the same directory as
   * specified by _indexPrefix inside {@link _options}.
   *
   * @throws IOException
   */
  @Override
  public void compute() throws IOException {
    System.out.println("Computing using " + this.getClass().getName());
    System.out.println("Iterative time: " + _options._iteration_times);
    System.out.println("Damping factor: " + _options._dampingFactor);
    File folder  = new File(_options._corpusPrefix);
    //add filter to exclude hidden files
    FilenameFilter filenameFilter = new FilenameFilter() {
      @Override
      public boolean accept(File file, String name) {
        return !name.startsWith(".");
      }
    };
    File[] files = folder.listFiles(filenameFilter);

    //graph is not initialized
    if(_pageGraph == null){
      return;
    }

    ProgressBar progressBar = new ProgressBar();

    // set initial value
    for(int docid = 0; docid < files.length; docid++){
      // since pagerank value is lower bounded by (1 - d), so we use 0.0
      // to represent N/A
      _pageRanks.put(docid, _pageGraph.containsVertex(docid) ? 1.0 : 0.0);
    }

    int counter = 0;
    while(counter < _options._iteration_times){
  for(int docid = 0; docid < files.length; docid++){
        if(_pageGraph.containsVertex(docid)){
          double pageRank = (1.0 - _options._dampingFactor) / files.length;
          Set<PageGraphEdge> incomingEdges = _pageGraph.incomingEdgesOf(docid);
          double otherRanks = 0.0;
          if(incomingEdges != null && incomingEdges.size() > 0){
            for(PageGraphEdge edge : incomingEdges){
              int sourceId = (Integer)edge.getEdgeSource();
              Set<PageGraphEdge> sourceOutgoingEdges = _pageGraph.outgoingEdgesOf(sourceId);
              if(sourceOutgoingEdges != null && sourceOutgoingEdges.size() > 0){
                otherRanks += _pageRanks.get(sourceId) / sourceOutgoingEdges.size();
              }
              else{
                otherRanks += _pageRanks.get(sourceId);
              }
            }
          }
          pageRank += _options._dampingFactor * otherRanks;
          _pageRanks.put(docid, pageRank);
        }
        else{
          _pageRanks.put(docid, 0.0);
        }
        // Update the progress bar after processing a document :)
        progressBar.update(docid, files.length);
      }
      counter++;
    }

    /**************************************************************************
     * First check if the folder exists... If not, create one.
     *************************************************************************/
    File outputFolder = new File(_options._pagerankPrefix);
    if (!(outputFolder.exists() && outputFolder.isDirectory())) {
      outputFolder.mkdir();
    }

    /**************************************************************************
     * First clean the folder before writing to file....
     *************************************************************************/
    for (File file : outputFolder.listFiles()) {
        file.delete();
    }

    /**************************************************************************
     * Writing to file....
     *************************************************************************/
    String filePath = _options._pagerankPrefix + "/pageRanks.g6";

    Kryo kryo = new Kryo();
    Output output = new Output(new FileOutputStream(filePath));

    kryo.writeObject(output, _pageRanks);
    output.close();

  }

  /**
   * During indexing mode, this function loads the PageRank values computed
   * during mining mode to be used by the indexer.
   *
   * @throws IOException
   */
  @Override
  public Object load() throws IOException {
    System.out.println("Loading using " + this.getClass().getName());

    File file = new File(_options._pagerankPrefix + "/_pageRanks.g6");
    Kryo kryo = new Kryo();
    Input input = new Input(new FileInputStream(file));

    _pageRanks.clear();
    _pageRanks = kryo.readObject(input, HashMap.class);

    input.close();

    return _pageRanks;
  }

/*  private String extractLinkUrl(String link){
    String url = null;
    int index = link.indexOf("href=\"");
    if(index != -1){
      index = index + "href=\"".length();
      int endIndex = link.indexOf("\"", index);
      if(endIndex != -1){
        url = link.substring(index, endIndex);
      }
    }

    return url;
  }*/

  private boolean isValidInternalUrl(String url){
    boolean isValid = true;

    if(url != null && url.startsWith("..") || url.startsWith("http:") || url.startsWith("https:")){
      isValid = false;
    }

    return isValid;
  }
}
