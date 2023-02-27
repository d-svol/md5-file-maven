package org.example;


import java.io.IOException;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        String fileDirectory = args[0];
        int treadsAmount = Integer.parseInt(args[1]);
        MD5dubl first = new MD5dubl(fileDirectory, treadsAmount);
        first.printMd5DirTree();
        System.out.println("Done");
    }
}
