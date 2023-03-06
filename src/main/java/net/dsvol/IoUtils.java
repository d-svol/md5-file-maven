package net.dsvol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class IoUtils {

  public static List<String> getListOfFilesInDirectory(String directory) throws IOException {
    List<String> collectedFiles = new ArrayList<>();

    Files.walkFileTree(Path.of(directory), new FileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (dir.toFile().canRead() && dir.toFile().canExecute()) {
          return FileVisitResult.CONTINUE;
        } else {
          return FileVisitResult.SKIP_SUBTREE;
        }
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        File ioFile = file.toFile();
        if (ioFile.canRead() && attrs.isRegularFile()) {
          collectedFiles.add(ioFile.toString());
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        return FileVisitResult.CONTINUE;
      }
    });

    return collectedFiles;
  }

  public static String calculateMd5SumOfFile(String file) throws NoSuchAlgorithmException, IOException {
    MessageDigest md5 = MessageDigest.getInstance("MD5");
    try (InputStream in = new FileInputStream(file)) {
      int nRead;
      byte[] buffer = new byte[1024 * 1024];
      while ((nRead = in.read(buffer)) != -1) {
        md5.update(buffer, 0, nRead);
      }
    }
    return new BigInteger(1, md5.digest()).toString(16);
  }
}
