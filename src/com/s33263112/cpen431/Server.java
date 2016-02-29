/*
 * Server waits for requests and then sends them to the ExecutorService.
 * RequestHandler handles the request and replies accordingly.
 */

package com.s33263112.cpen431;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server implements Runnable {
    
    private final int NUM_CORE_THREADS = 8;
    private final int NUM_MAX_THREADS = 16;
    private final int NUM_MAX_QUEUE = 32;
    
    private static volatile boolean running = true;
    public static NetworkHandler networkHandler;
    public static Cacher cacher;

    public Server(int port) {
        networkHandler = new NetworkHandler(port);
        cacher = new Cacher(5000);
    }
    
    public static void close() {
        running = false;
        networkHandler.close();
        cacher.close();
    }

    @Override
    public void run() {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(NUM_CORE_THREADS, NUM_MAX_THREADS, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(NUM_MAX_QUEUE));
        while (running) {
            DatagramPacket receivePacket;
            try {
                receivePacket = networkHandler.getNextPacket();
            } catch (IOException e) {
                // socket was likely closed by RequestHandler.handleShutdown.
                // The running threads will close on their own.
                pool.shutdown();
                break;
            }
            try {
                pool.execute(new RequestHandler(receivePacket));
            } catch (RejectedExecutionException ree) {
                // There are too many requests coming in. Don't process them normally otherwise the server could crash.
            }
        }
        close();
    }
}
