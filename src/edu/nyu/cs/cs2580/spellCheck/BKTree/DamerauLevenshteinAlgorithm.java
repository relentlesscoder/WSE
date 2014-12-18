package edu.nyu.cs.cs2580.spellCheck.BKTree;
import java.util.Arrays;

/**
 * This is modified based on the codes from:
 * https://github.com/KevinStern/software-and-algorithms/blob/master/src/main/java/blogspot/software_and_algorithms/stern_library/string/DamerauLevenshteinAlgorithm.java
 */
public class DamerauLevenshteinAlgorithm<E> implements DistanceAlgo<E> {
  static final private int CHARACTER_LENGTH = 128;

  public DamerauLevenshteinAlgorithm() {}

  /**
   * Compute the Damerau-Levenshtein distance between the specified source
   * string and the specified target string.
   */
  public int getDistance(E sourceElem, E targetElem) {
    String sourceStr = sourceElem.toString();
    String targetStr = targetElem.toString();

    if (sourceStr.length() == 0) {
      return targetStr.length();
    }

    if (targetStr.length() == 0) {
      return sourceStr.length();
    }

    final int INFINITY = sourceStr.length() + targetStr.length();

    int[][] table = new int[sourceStr.length() + 2][targetStr.length() + 2];
    table[0][0] = INFINITY;

    for(int i = 0; i <= sourceStr.length(); i++) {
      table[i + 1][1] = i;
      table[i + 1][0] = INFINITY;
    }
    for(int j = 0; j <= targetStr.length(); j++) {
      table[1][j + 1] = j;
      table[0][j + 1] = INFINITY;
    }

    int[] CL = new int[CHARACTER_LENGTH];
    Arrays.fill(CL, 0);

    for(int i = 1; i <= sourceStr.length(); i++) {
      int DB = 0;
      for(int j = 1; j <= targetStr.length(); j++) {
        int i1 = CL[targetStr.charAt(j-1)];
        int j1 = DB;
        int d = ((sourceStr.charAt(i - 1) == targetStr.charAt(j - 1)) ? 0 : 1);
        if(d == 0) {
          DB = j;
        }
        table[i + 1][j + 1] =
            min(
                table[i][j]+d,
                table[i + 1][j] + 1,
                table[i][j + 1] + 1,
                table[i1][j1] + (i - i1 - 1) + 1 + (j - j1 - 1)
            );
      }
      CL[sourceStr.charAt(i - 1)] = i;
    }

    return table[sourceStr.length() + 1][targetStr.length() + 1];
  }

  private int min(int ... numbers) {
    int min = Integer.MAX_VALUE;
    for (int num : numbers) {
      min = Math.min(min, num);
    }
    return min;
  }
}