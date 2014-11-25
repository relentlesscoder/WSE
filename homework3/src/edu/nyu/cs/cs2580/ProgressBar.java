package edu.nyu.cs.cs2580;

/**
 * Copied from: http://masterex.github.io/archive/2011/10/23/java-cli-progress-bar.html
 */

/**
 * Ascii progress meter. On completion this will reset itself,
 * so it can be reused
 * <br /><br />
 * 100% ************************************************** |
 */
public class ProgressBar {
  private StringBuilder progress;

  /**
   * initialize progress bar properties.
   */
  public ProgressBar() {
    init();
  }

  /**
   * called whenever the progress bar needs to be updated.
   * that is whenever progress was made.
   *
   * @param done  an int representing the work done so far
   * @param total an int representing the total work
   */
  public void update(int done, int total) {
    char[] workingChars = {'|', '/', '-', '\\'};
    String format = "\r%3d%% %s %c";

    int percent = (++done * 100) / total;
    int doneChars = (percent / 2) - this.progress.length();

    while (doneChars-- > 0) {
      progress.append('*');
    }

    System.out.printf(format, percent, progress,
        workingChars[done % workingChars.length]);

    if (done == total) {
      System.out.flush();
      System.out.println();
      init();
    }
  }

  private void init() {
    this.progress = new StringBuilder(60);
  }
}