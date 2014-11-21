package edu.nyu.cs.cs2580;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.nyu.cs.cs2580.SearchEngine.Options;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;

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

    Map<String, FileMetaData> nameDocIdMap = new HashMap<String, FileMetaData>();
    ArrayList redirectList = new ArrayList<Integer>();

    for (int docid = 0; docid < files.length; docid++) {
      checkNotNull(files[docid], "File can not be null!");
      if(isValidDocument(files[docid])){
        FileMetaData fileMetaData = new FileMetaData();
        _docCount++;
        fileMetaData.setDocId(docid);
        String redirectUrl = tryGetRedirectUrl(files[docid]);
        if(redirectUrl != null && !redirectUrl.isEmpty()){
          fileMetaData.setIsRedirectPage(true);
          fileMetaData.setRedirectUrl(redirectUrl);
          redirectList.add(docid);
        }
        nameDocIdMap.put(files[docid].getName(), fileMetaData);
      }
    }

    for (int docid = 0; docid < files.length; docid++) {
      checkNotNull(files[docid], "File can not be null!");
      if(isValidDocument(files[docid])){
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
    int redirectCount = 0;
    for(int docid = 0; docid < _docCount; docid++){
      // since pagerank value is lower bounded by (1 - d), so we use 0.0
      // to represent N/A
      if(!_pageGraph.containsVertex(docid)){
        redirectCount++;
      }
      pageRanks.put(docid, _pageGraph.containsVertex(docid) ? 1.0 : 0.0);
    }

    int counter = 0;
    while(counter < _options._iteration_times){
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

      counter++;
    }

    //Write to file
    FileOutputStream fos = null;
    try{
      File file = new File(_options._pagerankPrefix + "/pageranks.pks");
      if(!file.exists()){
        file.createNewFile();
      }
      fos = new FileOutputStream(file);
      fos.write(Util.serialize(pageRanks));
    }
    catch (Exception e){
      e.printStackTrace();
    }
    finally {
      try{
        if(fos != null){
          fos.close();
        }
      }
      catch(Exception e){
        e.printStackTrace();
      }
    }

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
    Object pageRanks = null;

    File file = new File(_options._pagerankPrefix + "/pageranks");
    FileInputStream fis = null;

    try{
      fis = new FileInputStream(file);
      byte[] fileCotent = new byte[(int)file.length()];
      fis.read(fileCotent);
      pageRanks = Util.deserialize(fileCotent);
    }
    catch (Exception e){
      e.printStackTrace();
    }
    finally {
      try{
        if(fis != null){
          fis.close();
        }
      }
      catch (Exception e){
        e.printStackTrace();
      }
    }

    return pageRanks;
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
