/*
 * Server waits for requests and then sends them to the ExecutorService.
 * RequestHandler handles the request and replies accordingly.
 */

package com.s33263112.cpen431;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server implements Runnable {
    
    private final int NUM_CORE_THREADS = 8;
    private final int NUM_MAX_THREADS = 16;
    private final int NUM_MAX_QUEUE = 32;
    
    private DatagramSocket socket;
    
    public Server(int port) {
        socket = createUdpSocket(port);
    }
    
    public void close() {
        if (!socket.isClosed()) {
            socket.close();
        }
    }

    @Override
    public void run() {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(NUM_CORE_THREADS, NUM_MAX_THREADS, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(NUM_MAX_QUEUE));
        while (true) {
            byte[] buffer = new byte[10051];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(receivePacket);
            } catch (IOException e) {
                // socket was likely closed by RequestHandler.handleShutdown.
                // The running threads will close on their own.
                pool.shutdown();
                break;
            }
            try {
                pool.execute(new RequestHandler(socket, receivePacket));
            } catch (RejectedExecutionException ree) {
                // There are too many requests coming in. Don't process them normally otherwise
                // the server could crash.
                returnSystemOverload(receivePacket);
            }
        }
    }
    
    private void returnSystemOverload(DatagramPacket packet) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(packet.getData()).order(ByteOrder.LITTLE_ENDIAN);
        byte[] requestId = new byte[16];
        byteBuffer.get(requestId, 0, 16);
        Reply reply = new Reply(requestId, ErrorCode.SYSTEM_OVERLOAD);
        sendReply(reply, packet.getAddress(), packet.getPort());
    }
    
    private DatagramSocket createUdpSocket(int port) {
        try {
            return new DatagramSocket(port);
        } catch (SocketException se) {
            // Throw to terminate the program
            throw new RuntimeException(se);
        }
    }

    private void sendReply(Reply reply, InetAddress address, int port) {
        System.out.println("Reply: " + reply.toString());
        byte[] buffer = reply.getReply();
        DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, address, port);
        try {
            socket.send(sendPacket);
        } catch (IOException ioe) {
            // socket was likely closed by another thread. We can safely proceed to let this thread terminate.
        }
    }
}
