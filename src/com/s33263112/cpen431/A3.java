package com.s33263112.cpen431;

public class A3 {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Required arguments: port");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.out.println("Invalid Port: " + args[0]);
            return;
        }
        new Server(port).run();
    }
}
