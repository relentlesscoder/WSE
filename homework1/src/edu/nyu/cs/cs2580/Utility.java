package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Utility {

  private static final Logger logger = LogManager.getLogger(Utility.class);
  private static final String RESULT_FOLDER_NAME = "results";

  public static Map<String, String> getQueryMap(String query) {
    String[] params = query.split("&");
    Map<String, String> map = new HashMap<String, String>();
    for (String param : params) {
      String name = param.split("=")[0];
      String value = param.split("=")[1];
      map.put(name, value);
    }
    return map;
  }

  public static boolean WriteToFile(String text, String fileName, boolean append) {
    if (text == null || text.isEmpty() || fileName == null
        || fileName.isEmpty()) {
      return false;
    }
    boolean result = false;
    String filePath = System.getProperty("user.dir");
    if (filePath != null && !filePath.isEmpty()) {
      filePath += "/" + RESULT_FOLDER_NAME + "/" + fileName;

      File file;
      FileOutputStream outputStream = null;
      try {
        file = new File(filePath);
        if (!file.exists()) {
          file.createNewFile();
        }
        outputStream = new FileOutputStream(file, append);
        byte[] contentInBytes = text.getBytes();

        outputStream.write(contentInBytes);
        outputStream.flush();
        outputStream.close();

        result = true;
      } catch (IOException e) {
        logger.error("Wrtie to file " + filePath + " error, due to: " + e);
      } finally {
        try {
          if (outputStream != null) {
            outputStream.close();
          }
        } catch (IOException e2) {
          logger.error("Close output stream error, due to: " + e2);
        }
      }
    }

    return result;
  }

}
