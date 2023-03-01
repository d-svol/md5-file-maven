package org.example;


import java.io.IOException;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        String fileDirectory = args[0];
        int treadsAmount = Integer.parseInt(args[1]);
        MD5Duplicate first = new MD5Duplicate(fileDirectory, treadsAmount);
        first.printDuplicateValue();
        System.out.println("Done");
    }
}
