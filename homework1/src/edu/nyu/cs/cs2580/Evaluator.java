package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class Evaluator {

  private static final Logger logger = LogManager.getLogger(Evaluator.class);
  private static final double[] STANDARD_RECALL_LEVELS = new double[] { 0.0,
      0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 };

  public static void main(String[] args) throws IOException {
    HashMap<String, HashMap<Integer, RelevancePair>> relevance_judgments = new HashMap<String, HashMap<Integer, RelevancePair>>();
    if (args.length < 1) {
      System.out.println("Need to provide relevance_judgments...");
      return;
    }
    String judgePath = args[0];
    // first read the relevance judgments into the HashMap
    readRelevanceJudgments(judgePath, relevance_judgments);
    // now evaluate the results from stdin
    String output = evaluateStdin(relevance_judgments);
    // TODO call write to file utility method
  }

  public static void readRelevanceJudgments(String judgePath,
  // <String: query, <Integer: documentID, RelevancePair: binary relevance and
  // categorical relevance pair>>
      HashMap<String, HashMap<Integer, RelevancePair>> relevanceJudgments) {
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
          double crel = 0.0;

          // convert to binary relevance
          if ((grade.equals("Perfect")) || (grade.equals("Excellent"))
              || (grade.equals("Good"))) {
            rel = 1.0;
            if (grade.equals("Perfect")) {
              crel = 10.0;
            } else if (grade.equals("Excellent")) {
              crel = 7.0;
            } else {
              crel = 5.0;
            }
          } else if (grade.equals("Fair")) {
            crel = 1.0;
          } else {
            crel = 0.0;
          }

          if (!relevanceJudgments.containsKey(query)) {
            HashMap<Integer, RelevancePair> tmpMap = new HashMap<Integer, RelevancePair>();
            RelevancePair relevancePair = new RelevancePair(rel, crel);
            tmpMap.put(docId, relevancePair);
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
  // <String: query, <Integer: documentID, RelevancePair: binary relevance and
  // categorical relevance pair>>
      HashMap<String, HashMap<Integer, RelevancePair>> relevanceJudgments) {
    StringBuilder output = new StringBuilder();
    ArrayList<Double> recallList = new ArrayList<Double>();
    ArrayList<Double> precisionList = new ArrayList<Double>();
    ArrayList<Double> ideaDCGList = new ArrayList<Double>();
    ArrayList<Double> queryDCGList = new ArrayList<Double>();
    double reciprocalRank = 0.0;

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
      HashMap<Integer, RelevancePair> queryJudgements = relevanceJudgments
          .get(query);
      Iterator<Entry<Integer, RelevancePair>> it = queryJudgements.entrySet()
          .iterator();
      while (it.hasNext()) {
        Entry<Integer, RelevancePair> pairs = (Entry<Integer, RelevancePair>) it
            .next();
        Object value = pairs.getValue();
        if (value != null) {
          RelevancePair relevancePair = (RelevancePair) value;
          if (relevancePair.getBinaryRelevance() == 1.0) {
            relevants += 1.0;
          }
          ideaDCGList.add(relevancePair.getCategoricalRelevance());
        }
        it.remove();
      }
      // sort decreasingly to get idea DCG list
      Collections.sort(ideaDCGList, new Comparator<Double>() {
        @Override
        public int compare(Double o1, Double o2) {
          return (o2 > o1) ? 1 : (o2 < o1) ? -1 : 0;
        }
      });

      boolean foundRelevant = false;

      double retrievedRelevants = 0.0;
      precisionList.set(0, 0.0);
      recallList.set(0, 0.0);
      queryDCGList.set(0, 0.0);
      Object value;
      if (queryJudgements.containsKey(firstDocId)
          && ((value = queryJudgements.get(firstDocId)) != null)) {
        RelevancePair relevancePair = (RelevancePair) value;
        if (relevancePair.getBinaryRelevance() == 1.0) {
          recallList.set(0, 1.0 / relevants);
          precisionList.set(0, 1.0);
          reciprocalRank = 1.0;
          foundRelevant = true;
          retrievedRelevants += 1.0;
          queryDCGList.set(0, relevancePair.getCategoricalRelevance());
        }
      }

      int count = 0;
      double recallN = 0.0;
      double precisionN = 0.0;
      double categoricalRelevance = 0.0;
      while (count < 10 && (line = reader.readLine()) != null) {
        count++;
        Scanner scanner = new Scanner(line).useDelimiter("\t");
        scanner.next();
        int docId = Integer.parseInt(scanner.next());
        if (queryJudgements.containsKey(docId)
            && ((value = queryJudgements.get(docId)) != null)) {
          RelevancePair relevancePair = (RelevancePair) value;
          if (relevancePair.getBinaryRelevance() == 1.0) {
            retrievedRelevants += 1.0;
            if (!foundRelevant) {
              reciprocalRank = 1.0 / (count + 1);
              foundRelevant = true;
            }
          }
          categoricalRelevance = relevancePair.getCategoricalRelevance();
        }
        recallN = retrievedRelevants / relevants;
        precisionN = retrievedRelevants / (count + 1);
        recallList.add(recallN);
        precisionList.add(precisionN);
        queryDCGList.add(categoricalRelevance);
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
    for (int i = 0; i < STANDARD_RECALL_LEVELS.length; i++) {
      output.append("\t"
          + getPrecisionAtStandardRecalls(STANDARD_RECALL_LEVELS[i],
              precisionList, recallList));
    }
    output.append("\t" + getAveragePrecision(precisionList));
    output.append("\t"
        + normalizedDiscountCumulativeGain(1, ideaDCGList, queryDCGList));
    output.append("\t"
        + normalizedDiscountCumulativeGain(5, ideaDCGList, queryDCGList));
    output.append("\t"
        + normalizedDiscountCumulativeGain(10, ideaDCGList, queryDCGList));
    output.append("\t" + reciprocalRank);
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

  private static double getPrecisionAtStandardRecalls(double standardRecall,
      ArrayList<Double> precisionList, ArrayList<Double> recallList) {
    double precision = 0.0;

    for (int i = 0; i < recallList.size(); i++) {
      if (recallList.get(i) >= standardRecall) {
        precision = Math.max(precisionList.get(i), precision);
      }
    }

    return precision;
  }

  private static double normalizedDiscountCumulativeGain(int rankPosition,
      ArrayList<Double> ideaDCGList, ArrayList<Double> queryDCGList) {
    double ndcg = 0.0;
    double dcg = 0.0;
    double idcg = 0.0;

    for (int i = 0; i < rankPosition; i++) {
      dcg += discountCumulativeGain(rankPosition, queryDCGList);
      idcg += discountCumulativeGain(rankPosition, ideaDCGList);
    }

    if (idcg != 0.0) {
      ndcg = dcg / idcg;
    }
    return ndcg;
  }

  private static double discountCumulativeGain(int rankPosition,
      ArrayList<Double> relevanceDCGList) {
    double dcg = relevanceDCGList.get(0);
    for (int i = 0; i < rankPosition; i++) {
      dcg += relevanceDCGList.get(i) / (Math.log(i) / Math.log(2));
    }
    return dcg;
  }
}

class RelevancePair {
  private final double binaryRelevance;
  private final double categoricalRelevance;

  public RelevancePair(double binaryRelevance, double categoricalRelevance) {
    this.binaryRelevance = binaryRelevance;
    this.categoricalRelevance = categoricalRelevance;
  }

  public double getBinaryRelevance() {
    return this.binaryRelevance;
  }

  public double getCategoricalRelevance() {
    return this.categoricalRelevance;
  }
}
