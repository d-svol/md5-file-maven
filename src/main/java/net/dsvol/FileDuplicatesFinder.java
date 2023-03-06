package net.dsvol;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

class FileDuplicatesFinder implements IFileDuplicatesFinder {
  private final String directory;
  private final int threadsAmount;

  public FileDuplicatesFinder(String directory, int threadsAmount) {
    this.directory = directory;
    this.threadsAmount = threadsAmount;
  }

  private Map<String, String> calculateMd5SumsOfFiles(List<String> files) {
    ConcurrentHashMap<String, String> md5HashSums = new ConcurrentHashMap<>();
    CountDownLatch stopLatch = new CountDownLatch(files.size());
    ExecutorService executorService = Executors.newFixedThreadPool(threadsAmount);

    files.stream()
      .map(file -> (Runnable) () -> {
        try {
          String fileMd5Sum = IoUtils.calculateMd5SumOfFile(file);
          md5HashSums.put(file, fileMd5Sum);
        } catch (NoSuchAlgorithmException | IOException e) {
          System.out.println("Can't calculate MD5 sum for file: " + file + ", continue..");
        } finally {
          stopLatch.countDown();
        }
      })
      .forEach(executorService::execute);

    boolean workIsDone = false;
    try {
      // TODO: need to configure this via CLI args/config_file/env_variables.
      workIsDone = stopLatch.await(5, TimeUnit.MINUTES);
      if (!workIsDone) {
        System.out.println("Work is not done in time, exit!");
      }
    } catch (InterruptedException ie) {
      System.out.println("Interrupted while working on files, exit!");
    }

    executorService.shutdownNow();
    if (!workIsDone) {
      md5HashSums.clear();
    }

    return md5HashSums;
  }

  @Override
  public void showDuplicateFiles() {
    List<String> files;
    try {
      files = IoUtils.getListOfFilesInDirectory(directory);
    } catch (IOException ioe) {
      System.out.println("I/O error occurred: " + ioe.getMessage());
      return;
    }

    Map<String, String> md5SumPerFile = calculateMd5SumsOfFiles(files);

    Map<String, List<String>> filesPerMd5HashSum = md5SumPerFile.entrySet()
      .stream()
      .collect(groupingBy(
        Map.Entry::getValue,
        mapping(Map.Entry::getKey, Collectors.toList()))
      );

    long spaceToFree = 0;
    StringBuilder sb = new StringBuilder();
    for (List<String> duplicateFiles : filesPerMd5HashSum.values()) {
      if (duplicateFiles.size() > 1) {
        long duplicateFileSize = new File(duplicateFiles.get(0)).length();
        spaceToFree += duplicateFileSize * duplicateFiles.size();

        sb.append("Found ").append(duplicateFiles.size()).append(" duplicate files.");
        duplicateFiles.forEach(file -> sb.append(file).append("\n"));
        sb.append("\n");
      }
    }
    System.out.print(sb);

    if (spaceToFree >= 1048576) {
      System.out.printf("You may free up to %.2f MB, if you delete duplicate files.\n", spaceToFree / (1024.0 * 1024));
    } else {
      System.out.printf("You may free up to %.2f KB, if you delete duplicate files.\n", spaceToFree / 1024.0);
    }
  }
}
