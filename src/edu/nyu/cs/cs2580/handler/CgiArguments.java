package edu.nyu.cs.cs2580.handler;

/**
 * CGI arguments provided by the user through the URL. This will determine
 * which Ranker to use and what output format to adopt. For simplicity, all
 * arguments are publicly accessible.
 */
public class CgiArguments {
  // The type of the ranker we will be using.
  public enum RankerType {
    NONE, CONJUNCTIVE, COSINE, QL, COMPREHENSIVE, NEWS
  }

  // The output format.
  public enum OutputFormat {
    TEXT, HTML, JSON
  }

  // The raw user query
  public String _query = "";
  // How many results/documents to return, default is 10
  public int _numResults = 10;
  // How many documents for PRF, default is 10
  public int _numDocs = 10;
  // How many terms for PRF, default is 5
  public int _numTerms = 5;
  // The response format, default is plain text
  public OutputFormat _outputFormat = OutputFormat.TEXT;
  // The ranker type, default is none....
  public RankerType _rankerType = RankerType.NONE;

  public CgiArguments(String uriQuery) {
    String[] params = uriQuery.split("&");
    for (String param : params) {
      String[] keyVal = param.split("=", 2);
      if (keyVal.length != 2) {
        continue;
      }
      String key = keyVal[0].toLowerCase();
      String val = keyVal[1];
      if (key.equals("query")) {
        _query = val;
      } else if (key.equals("num")) {
        try {
          _numResults = Integer.parseInt(val);
        } catch (NumberFormatException e) {
          // Ignored, search engine should never fail upon invalid user input.
        }
      } else if (key.equals("ranker")) {
        try {
          _rankerType = RankerType.valueOf(val.toUpperCase());
        } catch (IllegalArgumentException e) {
          // Ignored, search engine should never fail upon invalid user input.
        }
      } else if (key.equals("numdocs")) {
        try {
          _numDocs = Integer.parseInt(val);
        } catch (IllegalArgumentException e) {
          // Ignored, search engine should never fail upon invalid user input.
        }
      } else if (key.equals("numterms")) {
        try {
          _numTerms = Integer.parseInt(val);
        } catch (IllegalArgumentException e) {
          // Ignored, search engine should never fail upon invalid user input.
        }
      } else if (key.equals("format")) {
        try {
          _outputFormat = OutputFormat.valueOf(val.toUpperCase());
        } catch (IllegalArgumentException e) {
          // Ignored, search engine should never fail upon invalid user input.
        }
      }
    } // End of iterating over params
  }
}
