package edu.nyu.cs.cs2580;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import edu.nyu.cs.cs2580.utils.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class Spearman {

    private Spearman(){
    }

    private static void checkCommandLine(String[] args) throws IOException,
            NumberFormatException {
        Util.Check(args.length == 2, "Invalid argument number!");
        Util.Check(args[0] != null || !args[0].isEmpty(), "Must provide pagerank file path!");
        Util.Check(args[1] != null || !args[1].isEmpty(), "Must provide numviews file path!");

    }

    public static void main(String[] args) {
        try{
            checkCommandLine(args);
            HashMap<Integer, Double> pageRanks = loadPageRankFile(args[0]);
            HashMap<Integer, Integer> numViews = loadNumViewsFile(args[1]);

            //sort the HashMap and saved into LinkedHashMap which guarantees the insertion order
            LinkedHashMap<Integer, Double> sortedPageRanks = Util.sortHashMapByValues(pageRanks, true);
            LinkedHashMap<Integer, Integer> sortedNumViews = Util.sortHashMapByValues(numViews, true);

            HashMap<Integer, Integer> docPageRankMap = BuildDocRankingMap(sortedPageRanks);
            HashMap<Integer, Integer> docNumViewsMap = BuildDocRankingMap(sortedNumViews);

            double rankCoefficient = getSpearmanCoefficient(docPageRankMap, docNumViewsMap);
            System.out.println("Spearman rank coefficient: " + rankCoefficient);
        }
        catch (Exception e){
            System.err.println(e.getMessage());
        }
    }

    private static double getSpearmanCoefficient(Map<Integer, Integer> docPageRankMap, Map<Integer, Integer> docNumViewsMap){
        double result = 0.0;

        double zLeft = getZvalue(docPageRankMap);
        double zRight = getZvalue(docNumViewsMap);
        double upper = 0.0;
        double bottomLeft = 0.0;
        double bottomRight = 0.0;
        int docNum = docPageRankMap.size();

        for(int i = 0; i < docNum; i++){
            double left = docPageRankMap.get(i) - zLeft;
            double right = docNumViewsMap.get(i) - zRight;
            upper += left * right;
            bottomLeft += left * left;
            bottomRight += right * right;
        }

        result = upper / Math.sqrt (bottomLeft * bottomRight);
        return result;
    }

    private static double getZvalue(Map<Integer, Integer> docPageRankMap){
        double result = 0.0;
        int count = 0;
        for(Integer value : docPageRankMap.values()){
            result += value;
            count++;
        }
        result = result / count;
        return result;
    }

    private static HashMap<Integer, Integer> BuildDocRankingMap(Map sortedPageRanks){
        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();

        Set entrySet = sortedPageRanks.entrySet();

        Iterator it = entrySet.iterator();

        int ranking = 0;
        while (it.hasNext()){
            ranking++;
            Map.Entry pairs = (Map.Entry)it.next();
            map.put((Integer)pairs.getKey(), ranking);
        }

        return map;
    }

    private static HashMap<Integer, Double> loadPageRankFile(String filePath) throws IOException{
        System.out.println("Loading PageRank file");

        HashMap<Integer, Double> pageRanks = new HashMap<Integer, Double>();
        File file = new File(filePath);
        Kryo kryo = new Kryo();
        Input input = new Input(new FileInputStream(file));

        pageRanks.clear();
        pageRanks = kryo.readObject(input, HashMap.class);

        input.close();

        return pageRanks;
    }

    private static HashMap<Integer, Integer> loadNumViewsFile(String filePath) throws IOException{
        System.out.println("Loading NumViews");

        HashMap<Integer, Integer> docNumView = new HashMap<Integer, Integer>();
        File file = new File(filePath);
        Kryo kryo = new Kryo();
        Input input = new Input(new FileInputStream(file));

        docNumView.clear();
        docNumView = kryo.readObject(input, HashMap.class);

        input.close();

        return docNumView;
    }
}
