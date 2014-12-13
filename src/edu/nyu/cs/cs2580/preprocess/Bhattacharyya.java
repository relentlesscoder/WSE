package edu.nyu.cs.cs2580.preprocess;

import edu.nyu.cs.cs2580.handler.TermPrf;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tanis on 11/18/14.
 */
public class Bhattacharyya {
  public static void main (String[] args){
    try {
      String inputPaths = args[0];
      String outputFilename = args[1];
      List<String> queries = new ArrayList<String>();
      List<String> paths = new ArrayList<String>();
//      Map<String,String> paths4Queries = new HashMap<String, String>();
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(inputPaths))));
      String s;
      while ((s = in.readLine()) != null) {
        String[] str = s.split(":");
        queries.add(str[0]);
        paths.add(str[1]);
      }
      in.close();

      List<List<TermPrf>> extendedQueries = new ArrayList<List<TermPrf>>();
      for (String path : paths){
        in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))));
        List<TermPrf> termPrfs = new ArrayList<TermPrf>();
        while ((s = in.readLine()) != null) {
          String[] str = s.split("\t");
          termPrfs.add(new TermPrf(str[0],Double.parseDouble(str[1])));
        }
        extendedQueries.add(termPrfs);
        in.close();
      }

      StringBuilder sb = new StringBuilder();
      for (int i=0; i<queries.size(); i++){
        String q1 = queries.get(i);
        for (int j=0; j<queries.size(); j++){
          if (j==i) {
            continue;
          }else{
            String q2 = queries.get(j);
            double similarity = computeSimilarity(extendedQueries.get(i),extendedQueries.get(j));
            sb.append(q1).append('\t').append(q2).append('\t').append(similarity).append('\n');
          }
        }
      }
      System.out.println(sb.toString());

      File outputFile = new File(outputFilename);
      FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
      fileOutputStream.write(sb.substring(0,sb.length()-1).getBytes());
      System.out.println("Written");
      fileOutputStream.flush();
      fileOutputStream.close();

    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

  public static double computeSimilarity (List<TermPrf> tps1, List<TermPrf> tps2) {
    double similarity = 0;
    for (TermPrf tp1 : tps1){
      for (TermPrf tp2 : tps2){
        if (tp1.term.equals(tp2.term)){
          similarity += Math.sqrt(tp1.prf*tp2.prf);
        }
      }
    }
    return similarity;
  }
}
