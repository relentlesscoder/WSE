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
    Date last = Calendar.getInstance().getTime();
    try {
      DateFormat format = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
      Date date = format.parse(string);
      last = date;
      return date;
    }catch (Exception e){
      System.err.print(e.toString());
      return last;
    }
  }
}
