package edu.nyu.cs.cs2580.utils; /**
 * Created by tanis on 10/9/14.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class WriteFile {

    private static final String RESULT_FOLDER_NAME = "data/news";
    private static boolean stream = false;

    public static boolean WriteToFile(String text, String fileName, boolean append) {
        if (text == null || text.isEmpty() || fileName == null
                || fileName.isEmpty()) {
            return false;
        }
        boolean result = false;
        String filePath = RESULT_FOLDER_NAME;
        if (filePath != null && !filePath.isEmpty()) {
            filePath += "/" + fileName;
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
                System.err.println("Wrtie to file " + filePath + " error, due to: " + e);
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e2) {
                    System.err.println("Close output stream error, due to: " + e2);
                }
            }
        }

        return result;
    }

}
