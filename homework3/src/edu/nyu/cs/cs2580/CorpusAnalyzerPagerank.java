package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.nyu.cs.cs2580.SearchEngine.Options;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @CS2580: Implement this class for HW3.
 */
public class CorpusAnalyzerPagerank extends CorpusAnalyzer {
  private DirectedGraph<Integer, PageGraphEdge> _pageGraph = null;
  private int _docCount = 0;

  public CorpusAnalyzerPagerank(Options options) {
    super(options);
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
    File[] files = folder.listFiles();

    checkNotNull(files, "No files found in: %s", folder.getPath());

    _pageGraph = new DefaultDirectedGraph<Integer, PageGraphEdge>(PageGraphEdge.class);

    Map<String, Integer> nameDocIdMap = new HashMap<String, Integer>();
    for (int docid = 0; docid < files.length; docid++) {
      checkNotNull(files[docid], "File can not be null!");
      if(isValidDocument(files[docid])){
        _docCount++;
        nameDocIdMap.put(files[docid].getName(), docid);
      }
    }

    for (int docid = 0; docid < files.length; docid++) {
      checkNotNull(files[docid], "File can not be null!");
      _pageGraph.addVertex(docid);
      if(isValidDocument(files[docid])){
        HeuristicLinkExtractor linkExtractor = new HeuristicLinkExtractor(files[docid]);
        String link = linkExtractor.getNextInCorpusLinkTarget();
        while(link != null){
          String url = extractLinkUrl(link);
          if(isValidInternalUrl(url) && nameDocIdMap.containsKey(url)){
            int linkToDocId = nameDocIdMap.get(url);
            //TODO: need to confirm only adding same edge once
            if(!_pageGraph.containsEdge(docid, linkToDocId)){
              _pageGraph.addEdge(docid, linkToDocId);
            }
          }
          link = linkExtractor.getNextInCorpusLinkTarget();
        }
      }
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

    //graph is not initialized
    if(_pageGraph == null){
      return;
    }

    Map pageRanks = new HashMap<Integer, Integer>();

    // set initial value
    for(int docid = 0; docid < _docCount; docid++){
      pageRanks.put(docid, 1.0);
    }

    for(int docid = 0; docid < _docCount; docid++){
      if(_pageGraph.containsVertex(docid)){
        double pageRank = 1.0 - _options._dampingFactor;
        Set<PageGraphEdge> incomingEdges = _pageGraph.incomingEdgesOf(docid);
        if(incomingEdges.size() > 0){
          for(PageGraphEdge edge : incomingEdges){
            int sourceId = (Integer)edge.getEdgeSource();
            Set<PageGraphEdge> sourceOutgoingEdges = _pageGraph.outgoingEdgesOf(sourceId);
            if(sourceOutgoingEdges.size() > 0){
              pageRank += _options._dampingFactor * ((Double)pageRanks.get(sourceId) / sourceOutgoingEdges.size());
            }
            else{
              pageRank += _options._dampingFactor * ((Double)pageRanks.get(sourceId));
            }
          }
        }

        pageRanks.put(docid, pageRank);
      }
    }

    //TODO: Write to file
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
    return null;
  }

  private String extractLinkUrl(String link){
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
  }

  private boolean isValidInternalUrl(String url){
    boolean isValid = true;

    if(url.startsWith("..") || url.startsWith("http:") || url.startsWith("https:")){
      isValid = false;
    }

    return isValid;
  }
}
