package edu.nyu.cs.cs2580.preprocess;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by tanis on 12/13/14.
 */
public class FilePreprocess {
  static Date last = Calendar.getInstance().getTime();
  final static Date start = toData("2014-11-01");

  public static int countLines(File filename) throws IOException {
    InputStream is = new BufferedInputStream(new FileInputStream(filename));
    try {
      byte[] c = new byte[1024];
      int count = 0;
      int readChars = 0;
      boolean empty = true;
      while ((readChars = is.read(c)) != -1) {
        empty = false;
        for (int i = 0; i < readChars; ++i) {
          if (c[i] == '\n') {
            ++count;
          }
        }
      }
      return (count == 0 && !empty) ? 1 : count;
    } finally {
      is.close();
    }
  }

  public static Date toData(String string) {
    try {
      Date date = new Date();
      if (string.matches(".*?\\-.*?\\-.*?")){
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        date = format.parse(string);
      }else if(string.matches(".*2014$")){
        DateFormat format = new SimpleDateFormat("E, dd MMM yyyy", Locale.ENGLISH);
        date = format.parse(string);
      }else if (string.matches("\\d+.*GMT$")){
        DateFormat format = new SimpleDateFormat("dd MMM yyy HH:mm:ss zzz", Locale.ENGLISH);
        date = format.parse(string);
      }else {
        DateFormat format = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
        date = format.parse(string);
      }
      last = date;
      return date;
    }catch (Exception e){
      System.err.println(e.toString() + " -> " + last.toString());
      return last;
    }
  }
}
