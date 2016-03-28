package com.s33263112.cpen431;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import java.util.List;
import java.util.Set;

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

            Backup.checkNodePrime();

            Backup.pruneReplicates();

            Backup.fetchReplicates();

            Backup.printReplicates();

            RequestHandler.printStoreSize();

            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                Server.close();
                throw new RuntimeException(e);
            }
        }
    }
}
