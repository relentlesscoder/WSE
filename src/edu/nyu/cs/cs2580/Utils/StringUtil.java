package edu.nyu.cs.cs2580.utils;

public class StringUtil {

  public static float getLevensteinDistance(String target, String other){
    char[] sa;
    int n;
    int p[];
    int d[];
    int _d[];

    sa = target.toCharArray();
    n = sa.length;
    p = new int[n+1];
    d = new int[n+1];

    final int m = other.length();
    if (n == 0 || m == 0) {
      if (n == m) {
        return 1;
      }
      else {
        return 0;
      }
    }

    int i;
    int j;

    char t_j;

    int cost;

    for (i = 0; i<=n; i++) {
      p[i] = i;
    }

    for (j = 1; j<=m; j++) {
      t_j = other.charAt(j-1);
      d[0] = j;

      for (i=1; i<=n; i++) {
        cost = sa[i-1]==t_j ? 0 : 1;
        d[i] = Math.min(Math.min(d[i-1]+1, p[i]+1),  p[i-1]+cost);
      }

      _d = p;
      p = d;
      d = _d;
    }

    return 1.0f - ((float) p[n] / Math.max(other.length(), sa.length));
  }
}
