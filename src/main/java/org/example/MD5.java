package org.example;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.attribute.*;

public class MD5 {
    private final String directory;
    private final int threadsAmount;

    public MD5(String directory, int threadsAmount) {
        this.directory = directory;
        this.threadsAmount = threadsAmount;
    }

    public List<String> getListFromPath() throws IOException {
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

    public String getMd5(String file) throws NoSuchAlgorithmException, IOException {
        int nRead;
        byte[] buffer = new byte[1024 * 1024];
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        try (InputStream in = new FileInputStream(file)) {
            while ((nRead = in.read(buffer)) != -1) {
                md5.update(buffer, 0, nRead);
            }
        }
        return new BigInteger(1, md5.digest()).toString(16);
    }

    public void printMd5DirTree() throws InterruptedException, IOException {
        List<String> files = getListFromPath();
        CountDownLatch latch = new CountDownLatch(files.size());
        ExecutorService executorService = Executors.newFixedThreadPool(threadsAmount);
        //sleep 2 day

        for (String file : files) {
            executorService.execute(() -> {
                try {
                    String out = "File directory: " + file + " ===>>>  " + getMd5(file);
                    System.out.println(out);
                } catch (NoSuchAlgorithmException | IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        //list out
        latch.await();
        executorService.shutdownNow();
        System.out.println("Num files: " + files.size());
    }

    private static class Lock {
        private final Object internalLock = new Object();
        private int total;

        public Lock(int total) {
            if (total < 0) {
                throw new IllegalArgumentException("Total < 0");
            }
            this.total = total;
        }

        public void decrement() {
            synchronized (internalLock) {
                if (total > 0) {
                    total--;
                }
                if (total == 0) {
                    internalLock.notifyAll();
                }
            }
        }

        public void waitZero() throws InterruptedException {
            synchronized (internalLock) {
                while (total > 0) {
                    internalLock.wait();
                }
            }
        }
    }
}