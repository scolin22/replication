package test;

import com.s33263112.cpen431.Command;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class TestClient {

    private InetAddress serverAddress;
    private int serverPort;
    private DatagramSocket socket;

    public TestClient(String ip, int port) {
        serverAddress = getRemoteAddress(ip);
        serverPort = port;
        socket = createUdpSocket();
        try {
            socket.setSoTimeout(5000);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }
    
    public TestClient(InetAddress address, int port) {
        this.serverAddress = address;
        this.serverPort = port;
        socket = createUdpSocket();
        try {
            socket.setSoTimeout(5000);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void close() {
        socket.close();
    }
    
    public ClientReply invalidRequestId() {
        ClientRequest request = new ClientRequest();
        sendBytes(request.getRequest());
        DatagramPacket receivePacket = new DatagramPacket(new byte[17], 17);
        try {
            socket.setSoTimeout(100);
            socket.receive(receivePacket);
            return new ClientReply(receivePacket.getData());
        } catch (SocketTimeoutException ste) {
            return null;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    
    public ClientReply noCommand() {
        ClientRequest request = new ClientRequest(Command.NONE);
        sendBytes(request.getRequest());
        DatagramPacket receivePacket = new DatagramPacket(new byte[17], 17);
        try {
            socket.receive(receivePacket);
            return new ClientReply(receivePacket.getData());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    
    public ClientReply invalidCommand() {
        ClientRequest request = new ClientRequest((byte) 99);
        sendBytes(request.getRequest());
        DatagramPacket receivePacket = new DatagramPacket(new byte[17], 17);
        try {
            socket.receive(receivePacket);
            return new ClientReply(receivePacket.getData());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public ClientReply put(byte[] key, byte[] value) {
        ClientRequest request = new ClientRequest(Command.PUT, key, value, value.length);
        sendBytes(request.getRequest());
        DatagramPacket receivePacket = new DatagramPacket(new byte[17], 17);
        try {
            socket.receive(receivePacket);
            return new ClientReply(receivePacket.getData());
        } catch (IOException ioe) {
//            throw new RuntimeException(ioe);
            return null;
        }
    }
    
    public ClientReply put(byte[] requestId, byte[] key, byte[] value) {
        ClientRequest request = new ClientRequest(requestId, Command.PUT, key, value);
        sendBytes(request.getRequest());
        DatagramPacket receivePacket = new DatagramPacket(new byte[17], 17);
        try {
            socket.receive(receivePacket);
            return new ClientReply(receivePacket.getData());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    
    public ClientReply invalidPut(byte[] key, byte[] value, int valueLength) {
        ClientRequest request = new ClientRequest(Command.PUT, key, value, valueLength);
        sendBytes(request.getRequest());
        DatagramPacket receivePacket = new DatagramPacket(new byte[17], 17);
        try {
            socket.receive(receivePacket);
            return new ClientReply(receivePacket.getData());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    
    public ClientReply invalidPut(byte[] key) {
        ClientRequest request = new ClientRequest(Command.PUT, key);
        sendBytes(request.getRequest());
        DatagramPacket receivePacket = new DatagramPacket(new byte[17], 17);
        try {
            socket.receive(receivePacket);
            return new ClientReply(receivePacket.getData());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    
    public ClientReply get(byte[] key) {
        ClientRequest request = new ClientRequest(Command.GET, key);
        sendBytes(request.getRequest());
        DatagramPacket receivePacket = new DatagramPacket(new byte[10019], 10019);
        try {
            socket.receive(receivePacket);
            return new ClientReply(receivePacket.getData());
        } catch (IOException ioe) {
//            throw new RuntimeException(ioe);
            return null;
        }
    }
    
    public ClientReply remove(byte[] key) {
        ClientRequest request = new ClientRequest(Command.REMOVE, key);
        sendBytes(request.getRequest());
        DatagramPacket receivePacket = new DatagramPacket(new byte[17], 17);
        try {
            socket.receive(receivePacket);
            return new ClientReply(receivePacket.getData());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    
    public ClientReply shutdown() {
        ClientRequest request = new ClientRequest(Command.SHUTDOWN);
        sendBytes(request.getRequest());
        DatagramPacket receivePacket = new DatagramPacket(new byte[17], 17);
        try {
            socket.receive(receivePacket);
            return new ClientReply(receivePacket.getData());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    
    public ClientReply deleteAll() {
        ClientRequest request = new ClientRequest(Command.DELETE_ALL);
        sendBytes(request.getRequest());
        DatagramPacket receivePacket = new DatagramPacket(new byte[17], 17);
        try {
            socket.receive(receivePacket);
            return new ClientReply(receivePacket.getData());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private void sendBytes(byte[] buffer) {
        DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
        try {
            socket.send(sendPacket);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public InetAddress getLocalAddress() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        }
    }
    
    private InetAddress getRemoteAddress(String ip) {
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        }
    }
    
    private DatagramSocket createUdpSocket() {
        try {
            DatagramSocket socket = new DatagramSocket();
            return socket;
        } catch (SocketException se) {
            throw new RuntimeException(se);
        }
    }
}
