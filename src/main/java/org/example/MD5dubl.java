package org.example;

import java.io.*;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class MD5Duplicate {
    private final String directory;
    private final int threadsAmount;

    public MD5Duplicate(String directory, int threadsAmount) {
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
        for (String file : files) {
            executorService.execute(() -> {
                try {
                    String hasCode = getMd5(file);
                    String out = "File directory: " + file + " ===>>>  " + hasCode;
                    System.out.println(out);
                } catch (NoSuchAlgorithmException | IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdownNow();
        System.out.println("Num files: " + files.size());

    }

    public ConcurrentHashMap<String, String> getMd5Map() throws InterruptedException, IOException {
        List<String> files = getListFromPath();
        ConcurrentHashMap<String, String> hashMd5 = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(files.size());
        ExecutorService executorService = Executors.newFixedThreadPool(threadsAmount);

        for (String file : files) {
            executorService.execute(new Runnable() {
                public void run() {
                    try {
                        String md5HasSum = getMd5(file);
                        hashMd5.put(file, md5HasSum);
                    } catch (NoSuchAlgorithmException | IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        latch.await();
        executorService.shutdownNow();
        return hashMd5;
    }


    public void printDuplicateValue() throws IOException, InterruptedException {
        var map = getMd5Map();
        Set<String> seen = new HashSet<>();
        for (String key : map.keySet()) {
            String value = map.get(key);
            if (!seen.add(value)) {
                System.out.println("Duplicate Name: " + key + " Duplicate has: " + value);
            }
        }
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