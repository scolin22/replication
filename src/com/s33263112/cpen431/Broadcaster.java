package com.s33263112.cpen431;

import java.util.Map.Entry;

public class Broadcaster implements Runnable {

    private volatile boolean running = true;
    private int port;

    public Broadcaster(int port) {
        this.port = port;
        new Thread(this).start();
    }
    
    public void close() {
        running = false;
    }
    
    @Override
    public void run() {
        while (running) {
            synchronized (Router.class) {
                for (Entry<Integer, Node> entry : Router.getNodes().entrySet()) {
                    Node node = entry.getValue();
                    if (System.currentTimeMillis() - node.getLastUpdateTime() <= 60000) {
                        Server.networkHandler.sendBytes(
                                Request.createBroadcastRequest().toByteArray(), node.getAddress(), port);
                    }
                }
            }
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                Server.close();
                throw new RuntimeException(e);
            }
        }
    }
}
