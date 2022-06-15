package com.yxz.cli;

public class Application {

    public static void main(String[] args) {
        Cli cli = new Cli(args);
        cli.parse();
    }
}
