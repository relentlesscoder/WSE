package edu.nyu.cs.cs2580.spellCheck;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MisspellDataSet {
  // Key: misspell word
  // Value: a list of correct word
  private ListMultimap<String, String> misspellMap;

  public MisspellDataSet() {
    misspellMap = ArrayListMultimap.create();
  }

  /**
   * Populate the misspell map via a file. The file need to be in a specific
   * format:
   * Each line contains a word.
   * If a line starts with $, then it's the correct word, following with one
   * or more words start without $ which are misspell words.
   *
   * Note: This function assume the file is already in a nice format....
   */
  public void addData(File file) throws IOException {
    String content = Files.toString(file, StandardCharsets.UTF_8);
    String[] lines = content.split("\n");
    String correctTerm = "";
    int i = 0;

    while (i < lines.length) {
      if (lines[i].charAt(0) == '$') {
        correctTerm = lines[i].replace("$", "");
      } else {
        String misspellWord = lines[i];
        misspellMap.put(misspellWord, correctTerm);
      }
      i++;
    }
  }

  /**
   * Get the correct words for a specific misspelled word, an empty
   * list will be returned if no misspelled word is found.
   */
  public List<String> getCorrectWords(String misspellWord) {
    if (misspellMap.containsKey(misspellWord)) {
      return misspellMap.get(misspellWord);
    } else {
      return new ArrayList<String>();
    }
  }
}
