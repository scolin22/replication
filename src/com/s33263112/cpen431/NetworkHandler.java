package com.s33263112.cpen431;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class NetworkHandler {

    private DatagramSocket socket;
    
    public NetworkHandler(int port) {
        socket = createUdpSocket(port);
    }

    public void close() {
        if (!socket.isClosed()) {
            socket.close();
        }
    }
    

    public DatagramPacket getNextPacket() throws IOException {
        byte[] buffer = new byte[Request.MAX_REQUEST_LENGTH];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }
    
    public void sendBytes(byte[] buffer, InetAddress address, int port) {
        if (buffer == null) {
            return;
        }
        DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, address, port);
        try {
            socket.send(sendPacket);
        } catch (IOException ioe) {
            // socket was likely closed by another thread. We can safely proceed to let this thread terminate.
        }
    }
        
    private DatagramSocket createUdpSocket(int port) {
        try {
            return new DatagramSocket(port);
        } catch (SocketException se) {
            throw new RuntimeException(se);
        }
    }
}
