package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Scanner;

class Evaluator {

  public static void main(String[] args) throws IOException {
    HashMap<String, HashMap<Integer, Double>> relevance_judgments =
        new HashMap<String, HashMap<Integer, Double>>();
    if (args.length < 1) {
      System.out.println("Need to provide relevance_judgments...");
      return;
    }
    String judgePath = args[0];
    // first read the relevance judgments into the HashMap
    readRelevanceJudgments(judgePath, relevance_judgments);
    // now evaluate the results from stdin
    evaluateStdin(relevance_judgments);
  }

  public static void readRelevanceJudgments(
      String judgePath,
      // <String: query, <Integer: documentID, Double: binary relevance>>
      HashMap<String, HashMap<Integer, Double>> relevanceJudgments) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(judgePath));
      try {
        String line = null;
        while ((line = reader.readLine()) != null) {
          // parse the query,docId,relevance line
          Scanner s = new Scanner(line).useDelimiter("\t");
          String query = s.next();
          int docId = Integer.parseInt(s.next());
          String grade = s.next();
          double rel = 0.0;

          // convert to binary relevance
          if ((grade.equals("Perfect")) ||
              (grade.equals("Excellent")) ||
              (grade.equals("Good"))) {
            rel = 1.0;
          }

          if (!relevanceJudgments.containsKey(query)) {
            HashMap<Integer, Double> tmpMap = new HashMap<Integer, Double>();
            tmpMap.put(docId, rel);
            relevanceJudgments.put(query, tmpMap);
          }
        }
      } finally {
        reader.close();
      }
    } catch (IOException ioe) {
      System.err.println("Oops " + ioe.getMessage());
    }
  }

  public static void evaluateStdin(
      // <String: query, <Integer: documentID, Double: binary relevance>>
      HashMap<String, HashMap<Integer, Double>> relevanceJudgments) {
    // only consider one query per call    
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

      String line = null;
      double RR = 0.0;
      double N = 0.0;
      while ((line = reader.readLine()) != null) {
        Scanner s = new Scanner(line).useDelimiter("\t");
        String query = s.next();
        int docId = Integer.parseInt(s.next());
        String title = s.next();
        double rel = Double.parseDouble(s.next());
        if (relevanceJudgments.containsKey(query) == false) {
          throw new IOException("query not found");
        }
        HashMap<Integer, Double> qr = relevanceJudgments.get(query);
        if (qr.containsKey(docId) != false) {
          RR += qr.get(docId);
        }
        ++N;
      }
      System.out.println(Double.toString(RR / N));
    } catch (Exception e) {
      System.err.println("Error:" + e.getMessage());
    }
  }
}
