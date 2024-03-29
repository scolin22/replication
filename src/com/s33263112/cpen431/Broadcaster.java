package com.s33263112.cpen431;

public class Broadcaster implements Runnable {
    
    public static final int INTERVAL = 5000;

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

            Backup.checkNodePrime();

            Backup.pruneReplicates();

            Backup.fetchReplicates();

            //Backup.printReplicates();

            //RequestHandler.printStoreSize();

            try {
                Thread.sleep(INTERVAL);
            } catch (InterruptedException e) {
                Server.close1();
                Server.close2();
                throw new RuntimeException(e);
            }
        }
    }
}
