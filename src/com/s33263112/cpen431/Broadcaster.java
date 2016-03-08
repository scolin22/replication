package com.s33263112.cpen431;

public class Broadcaster implements Runnable {

    private volatile boolean running = true;

    public Broadcaster() {
        new Thread(this).start();
    }
    
    public void close() {
        running = false;
    }
    
    @Override
    public void run() {
        while (running) {
            for (Node node : Router.getActiveNodes()) {
                Server.networkHandler.sendBytes(
                        Request.createBroadcastRequest().toByteArray(), node.getAddress(), node.getPort());
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
