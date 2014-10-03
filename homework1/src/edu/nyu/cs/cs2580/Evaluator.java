package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class Evaluator {

  private static final Logger logger = LogManager.getLogger(Evaluator.class);

  public static void main(String[] args) throws IOException {
    HashMap<String, HashMap<Integer, Double>> relevance_judgments = new HashMap<String, HashMap<Integer, Double>>();
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

  public static void readRelevanceJudgments(String judgePath,
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
          if ((grade.equals("Perfect")) || (grade.equals("Excellent"))
              || (grade.equals("Good"))) {
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

  public static String evaluateStdin(
  // <String: query, <Integer: documentID, Double: binary relevance>>
      HashMap<String, HashMap<Integer, Double>> relevanceJudgments) {
    StringBuilder output = new StringBuilder();
    ArrayList<Double> recallList = new ArrayList<Double>();
    ArrayList<Double> precisionList = new ArrayList<Double>();

    // only consider one query per call
    try {

      BufferedReader reader = new BufferedReader(new InputStreamReader(
          System.in));
      String query = "";
      String line = reader.readLine();
      int firstDocId = -1;
      if (line != null) {
        Scanner scanner = new Scanner(line).useDelimiter("\t");
        query = scanner.next();
        firstDocId = Integer.parseInt(scanner.next());
        if (!relevanceJudgments.containsKey(query)) {
          throw new IOException("query not found");
        }
        scanner.close();
      }

      output.append(query);
      double relevants = 0.0;
      HashMap<Integer, Double> queryJudgements = relevanceJudgments.get(query);
      Iterator<Entry<Integer, Double>> it = queryJudgements.entrySet()
          .iterator();
      while (it.hasNext()) {
        Entry<Integer, Double> pairs = (Entry<Integer, Double>) it.next();
        if (pairs.getValue() == 1.0) {
          relevants += 1.0;
        }
        it.remove();
      }

      double retrievedRelevants = 0.0;
      if (queryJudgements.containsKey(firstDocId)
          && queryJudgements.get(firstDocId) == 1.0) {
        recallList.add(1.0 / relevants);
        precisionList.add(1.0);
        retrievedRelevants += 1.0;
      }

      int count = 0;
      double recallN = 0.0;
      double precisionN = 0.0;
      while (count < 10 && (line = reader.readLine()) != null) {
        count++;
        Scanner scanner = new Scanner(line).useDelimiter("\t");
        scanner.next();
        int docId = Integer.parseInt(scanner.next());
        if (queryJudgements.containsKey(firstDocId)
            && queryJudgements.get(docId) == 1.0) {
          retrievedRelevants += 1.0;
        }
        recallN = retrievedRelevants / relevants;
        precisionN = retrievedRelevants / (count + 1);
        recallList.add(recallN);
        precisionList.add(precisionN);
        scanner.close();
      }
    } catch (Exception e) {
      System.err.println("Error:" + e.getMessage());
      logger.error("Processing document error, due to: " + e);
    }

    output.append("\t" + precisionList.get(0));
    output.append("\t" + precisionList.get(4));
    output.append("\t" + precisionList.get(9));
    output.append("\t" + recallList.get(0));
    output.append("\t" + recallList.get(4));
    output.append("\t" + recallList.get(9));
    output.append("\t" + getFMeasure(precisionList.get(0), recallList.get(0)));
    output.append("\t" + getFMeasure(precisionList.get(4), recallList.get(4)));
    output.append("\t" + getFMeasure(precisionList.get(9), recallList.get(9)));
    // TODO: precision at recall level
    output.append("\t" + getAveragePrecision(precisionList));
    return output.toString();
  }

  private static double getFMeasure(double precision, double recall) {
    return 2 * (precision * recall) / (precision + recall);
  }

  private static double getAveragePrecision(ArrayList<Double> precisionList) {
    double sum = 0.0;
    int size = precisionList.size();
    for (int i = 0; i < size; i++) {
      sum += precisionList.get(i);
    }
    return sum / size;
  }
}
