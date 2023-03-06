package net.dsvol;

import java.io.File;

public class Main {

  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("java -jar md5-file-maven-1.0.jar [TOP_DIRECTORY] [NUM_THREADS]");
      System.exit(1);
    }

    String topDirectory = args[0];
    if (!isValidDirectory(topDirectory)) {
      System.out.println("Invalid directory: " + topDirectory);
      System.exit(1);
    }
    int threadsCount = getValidNumThreadsOrExit(args[1]);

    IFileDuplicatesFinder finder = new FileDuplicatesFinder(topDirectory, threadsCount);
    finder.showDuplicateFiles();
    System.out.println("Done");
  }

  private static boolean isValidDirectory(String dir) {
    try {
      return new File(dir).isDirectory();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private static int getValidNumThreadsOrExit(String arg) {
    int numThreads = 0;
    try {
      numThreads = Integer.parseInt(arg);
    } catch (NumberFormatException e) {
      System.out.println("Invalid number of threads: " + arg);
      System.exit(1);
    }
    if (numThreads <= 0 || numThreads > 100) {
      System.out.println("Number of threads should be between 0 and 100, actual: " + numThreads);
      System.exit(1);
    }
    return numThreads;
  }
}
