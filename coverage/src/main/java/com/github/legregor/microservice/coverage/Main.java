package com.github.legregor.microservice.coverage;

/**
 * Entry point for the Microservice Coverage Analysis Tool.
 * <p>Usage: {@code java -jar coverage-1.0.0.jar [<microservice-folder>...] [<test-file.java>...]}
 */
public class Main {

    public static void main(String[] args) {
        try {
            CoverageApp app = new CoverageApp();
            app.run(args);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
