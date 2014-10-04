package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class Evaluator {

  private static final Logger logger = LogManager.getLogger(Evaluator.class);
  private static final double[] STANDARD_RECALL_LEVELS = new double[] { 0.0,
      0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 };
  private static final int[] K = new int[] {1, 5, 10};
  private static final double ALPHA = 0.50;

  public static void main(String[] args) throws IOException {
    // <String: query, <Integer: documentID, RelevancePair: binary relevance and categorical relevance pair>>
    Map<String, HashMap<Integer, RelevancePair>> relevanceJudgments = new HashMap<String, HashMap<Integer, RelevancePair>>();
    if (args.length < 1) {
      System.out.println("Need to provide relevanceJudgments...");
      return;
    }
    String judgePath = args[0];

    // First read the relevance judgments into the map
    readRelevanceJudgments(judgePath, relevanceJudgments);

    // now evaluate the results from stdin
    String output = evaluateStdin(relevanceJudgments);
    // TODO call write to file utility method
  }

  public static void readRelevanceJudgments(String judgePath,
  // <String: query, <Integer: documentID, RelevancePair: binary relevance and categorical relevance pair>>
      Map<String, HashMap<Integer, RelevancePair>> relevanceJudgments) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(judgePath));
      try {
        String line;
        while ((line = reader.readLine()) != null) {
          // parse the query,docId,relevance line
          Scanner s = new Scanner(line).useDelimiter("\t");
          String query = s.next();
          int docId = Integer.parseInt(s.next());
          String grade = s.next();

          if (!relevanceJudgments.containsKey(query)) {
            HashMap<Integer, RelevancePair> tmpMap = new HashMap<Integer, RelevancePair>();
            RelevancePair relevancePair = getRelevancePair(grade);
            tmpMap.put(docId, relevancePair);
            relevanceJudgments.put(query, tmpMap);
          } else {
            RelevancePair relevancePair = getRelevancePair(grade);
            relevanceJudgments.get(query).put(docId, relevancePair);
          }
        }
      } finally {
        reader.close();
      }
    } catch (IOException ioe) {
      System.err.println("Oops " + ioe.getMessage());
    }
  }

  private static RelevancePair getRelevancePair(String grade) {
    double rel = 0.0;
    double crel = 0.0;

    // Get the relevance points. Both binary and category
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

    return new RelevancePair(rel, crel);
  }

  private static Map<Integer, Double> getPrecisionMap(HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    Map<Integer, Double> precisionMap = new HashMap<Integer, Double>();

    for (int i = 0; i < K.length; i++) {
      int k = K[i];
      double rr = 0.0;
      for (int j = 0; j < k; j++) {
        int docId = rankedList.get(j);
        if (relevanceJudgments.containsKey(docId) && relevanceJudgments.get(docId).getBinaryRelevance() == 1) {
          rr++;
        }
      }
      precisionMap.put(k, rr/k);
    }

    return precisionMap;
  }

  private static Map<Integer, Double> getRecallMap(HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    Map<Integer, Double> recallMap = new HashMap<Integer, Double>();
    int countOfRelevant = 0;

    for (Entry entry : relevanceJudgments.entrySet()) {
      RelevancePair relevancePair = (RelevancePair) entry.getValue();
      if (relevancePair.getBinaryRelevance() == 1) {
        countOfRelevant++;
      }
    }

    for (int i = 0; i < K.length; i++) {
      int k = K[i];
      double rr = 0.0;
      for (int j = 0; j < k; j++) {
        int docId = rankedList.get(j);
        if (relevanceJudgments.containsKey(docId) && relevanceJudgments.get(docId).getBinaryRelevance() == 1) {
          rr++;
        }
      }
      recallMap.put(k, rr/countOfRelevant);
    }

    return recallMap;
  }

  private static Map<Integer, Double> getFMap (Map<Integer, Double> precisionMap, Map<Integer, Double> recallMap) {
    Map<Integer, Double> FMap = new HashMap<Integer, Double>();

    for (int i = 0; i < K.length; i++) {
      int k = K[i];
      double p = precisionMap.get((Integer) k);
      double r = recallMap.get((Integer) k);
      double f = 1 / (ALPHA * (1 / p) + (1 - ALPHA) * (1 / r));
      FMap.put(k, f);
    }

    return FMap;
  }

  private static double getAveragePrecision(HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    int lengthOfRankedList = rankedList.size();
    double ap = 0.0;
    double rr = 0.0;

    for (int i = 0; i < lengthOfRankedList; i++) {
      int docId = rankedList.get(i);
      if (relevanceJudgments.containsKey((Integer) docId) && relevanceJudgments.get(docId).getBinaryRelevance() == 1) {
        rr += 1.0;
        ap += rr / i;
      }
    }

    return ap/rr;
  }

  private static double getReciprocal(HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    double i = 0.0;
    for (int docId : rankedList) {
      i++;
      if (relevanceJudgments.containsKey((Integer) docId) && relevanceJudgments.get(docId).getBinaryRelevance() == 1) {
        return 1 / i;
      }
    }

    return 0;
  }

  private static Map<Integer, Double> getDCG(HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    Map<Integer, Double> DCG = new HashMap<Integer, Double>();
    double dcg = 0.0;

    for (int i = 0; i < K.length; i++) {
      int k = K[i];
      for (int j = 0; j < k; j++) {
        int docId = rankedList.get(j);
        if (relevanceJudgments.containsKey((Integer) docId)) {
          dcg += relevanceJudgments.get(docId).getCategoricalRelevance() / Math.log(j + 2);
        }
      }

      DCG.put(k, dcg);
    }

    return DCG;
  }

  private static Map<Integer, Double> getIDCG(HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    Map<Integer, Double> IDCG = new HashMap<Integer, Double>();
    List<Double> ideaRankedList = new ArrayList<Double>();
    double idcg = 0.0;

    for (Entry entry : relevanceJudgments.entrySet()) {
      RelevancePair relevancePair = (RelevancePair) entry.getValue();
      double categoryRel = relevancePair.getCategoricalRelevance();
      if (categoryRel > 0) {
        ideaRankedList.add(relevancePair.getCategoricalRelevance());
      }
    }

    Collections.sort(ideaRankedList, new Comparator<Double>() {
      @Override
      public int compare(Double o1, Double o2) {
        return o1 > o2 ? -1 :
            o1 < o2 ? 1 : 0;
      }
    });

    for (int i = 0; i < K.length; i++) {
      int k = K[i];
      for (int j = 0; j < k; j++) {
        idcg += ideaRankedList.get(j) / Math.log(j + 2);
      }

      IDCG.put(k, idcg);
    }

    return IDCG;
  }

  private static Map<Integer, Double> getNDCG(HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    Map<Integer, Double> NDCG = new HashMap<Integer, Double>();
    Map<Integer, Double> DCG = getDCG(relevanceJudgments, rankedList);
    Map<Integer, Double> IDCG = getIDCG(relevanceJudgments, rankedList);

    for (int i = 0; i < K.length; i++) {
      int k = K[i];
      double ndcg = DCG.get((Integer) k) / IDCG.get((Integer) k);

      NDCG.put(k, ndcg);
    }

    return NDCG;
  }

  private static List<Double> getPrecisionAtStandardRecalls (HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    List<Double> precisionAtStandardRecalls = new ArrayList<Double>();

    List<PRPair> precisionRecallPairList = new ArrayList<PRPair>();

    // Get all recalls...
    int countOfRelevant = 0;

    for (Entry entry : relevanceJudgments.entrySet()) {
      RelevancePair relevancePair = (RelevancePair) entry.getValue();
      if (relevancePair.getBinaryRelevance() == 1) {
        countOfRelevant++;
      }
    }

    for (int i = 0; i < rankedList.size(); i++) {
      double rr = 0.0;

      for (int j = 0; j <= i; j++) {
        int docId = rankedList.get(j);
        if (relevanceJudgments.containsKey(docId) && relevanceJudgments.get(docId).getBinaryRelevance() == 1) {
          rr++;
        }
      }

      PRPair prPair = new PRPair();
      prPair.setRecall(rr / countOfRelevant);
      precisionRecallPairList.add(prPair);
    }


    // Get all precision
    for (int i = 0; i < rankedList.size(); i++) {
      double rr = 0.0;
      for (int j = 0; j <= i; j++) {
        int docId = rankedList.get(j);
        if (relevanceJudgments.containsKey(docId) && relevanceJudgments.get(docId).getBinaryRelevance() == 1) {
          rr++;
        }
      }
      System.out.println(rr / (i + 1) + "");
      precisionRecallPairList.get(i).setPrecision(rr / (i + 1));
    }

    // Sort the pairs by recall increasingly
    Collections.sort(precisionRecallPairList, new Comparator<PRPair>() {
      @Override
      public int compare(PRPair o1, PRPair o2) {
        return o1.getRecall() > o2.getRecall() ? 1 :
            o1.getRecall() < o2.getRecall() ? -1 : 0;
      }
    });

    for (int i = 0; i < STANDARD_RECALL_LEVELS.length; i++) {
      double recall = STANDARD_RECALL_LEVELS[i];
      double precision = 0.0;

      for (int j = 0; j < precisionRecallPairList.size(); j++) {
        PRPair prPair = precisionRecallPairList.get(j);

        if (prPair.getRecall() > recall) {
          break;
        }

        if (j > 0 && prPair.getRecall() == precisionRecallPairList.get(j - 1).getRecall()) {
          precision = Math.max(precision, prPair.getPrecision());
        } else {
          precision = prPair.getPrecision();
        }
      }

      precisionAtStandardRecalls.add(precision);
    }

    return precisionAtStandardRecalls;
  }

  public static String evaluateStdin(
  // <String: query, <Integer: documentID, RelevancePair: binary relevance and categorical relevance pair>>
      Map<String, HashMap<Integer, RelevancePair>> relevanceJudgments) {

    Map<Integer, Double> precisionMap;
    Map<Integer, Double> recallMap;
    Map<Integer, Double> FMap;
    Map<Integer, Double> NDCGMap;
    List<Double> precisionAtStandardRecalls;

    double averagePrecision;
    double reciprocal;

    StringBuilder output = new StringBuilder();
    ArrayList<Double> recallList = new ArrayList<Double>();
    ArrayList<Double> precisionList = new ArrayList<Double>();
    ArrayList<Double> ideaDCGList = new ArrayList<Double>();
    ArrayList<Double> queryDCGList = new ArrayList<Double>();
    double reciprocalRank = 0.0;

    String query = "";
    // Index: rank, Integer: docId
    List<Integer> rankedList = new ArrayList<Integer>();

    try {
      String path = "/Users/youlongli/Documents/Dropbox/cs/WS/WSE/WSE/homework1/data/test.tsv";
      BufferedReader reader = new BufferedReader(new FileReader(path));
      try {
        String line;
        while ((line = reader.readLine()) != null) {
          Scanner scanner = new Scanner(line);
          scanner.useDelimiter("\t");
          query = scanner.next();
          int docId = Integer.parseInt(scanner.next());
          rankedList.add(docId);
        }
      } finally {
        logger.info("Closing the file...");
        reader.close();
      }
    } catch (IOException ioe) {
      logger.error("Oops... {}", ioe.getMessage());
    }

    // Get the precision and recall evaluations
    precisionMap = getPrecisionMap(relevanceJudgments.get(query), rankedList);
    recallMap = getRecallMap(relevanceJudgments.get(query), rankedList);
    FMap = getFMap(precisionMap, recallMap);
    averagePrecision = getAveragePrecision(relevanceJudgments.get(query), rankedList);
    reciprocal = getReciprocal(relevanceJudgments.get(query), rankedList);
    NDCGMap = getNDCG(relevanceJudgments.get(query), rankedList);
    precisionAtStandardRecalls = getPrecisionAtStandardRecalls(relevanceJudgments.get(query), rankedList);


    // TODO: return the result

//    output.append("\t" + precisionList.get(0));
//    output.append("\t" + precisionList.get(4));
//    output.append("\t" + precisionList.get(9));
//    output.append("\t" + recallList.get(0));
//    output.append("\t" + recallList.get(4));
//    output.append("\t" + recallList.get(9));
//    output.append("\t" + getFMeasure(precisionList.get(0), recallList.get(0)));
//    output.append("\t" + getFMeasure(precisionList.get(4), recallList.get(4)));
//    output.append("\t" + getFMeasure(precisionList.get(9), recallList.get(9)));
//    for (int i = 0; i < STANDARD_RECALL_LEVELS.length; i++) {
//      output.append("\t"
//          + getPrecisionAtStandardRecalls(STANDARD_RECALL_LEVELS[i],
//              precisionList, recallList));
//    }
//    output.append("\t" + getAveragePrecision(precisionList));
//    output.append("\t"
//        + normalizedDiscountCumulativeGain(1, ideaDCGList, queryDCGList));
//    output.append("\t"
//        + normalizedDiscountCumulativeGain(5, ideaDCGList, queryDCGList));
//    output.append("\t"
//        + normalizedDiscountCumulativeGain(10, ideaDCGList, queryDCGList));
//    output.append("\t" + reciprocalRank);
//    return output.toString();

    return null;
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

class PRPair {
  private double precision;
  private double recall;

  public PRPair() {
    this.precision = 0.0;
    this.recall = 0.0;
  }

  public double getRecall() {
    return recall;
  }

  public double getPrecision() {
    return precision;
  }

  public void setRecall(double recall) {
    this.recall = recall;
  }

  public void setPrecision(double precision) {
    this.precision = precision;
  }
}
