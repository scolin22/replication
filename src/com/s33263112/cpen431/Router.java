package com.s33263112.cpen431;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

public class Router {

    private static final String SERVER_LIST = "/util/server.list";
    
    private static final InetAddress myIp;
    private static final int myPort = Server.port;

    private static final TreeMap<Integer, Node> nodes = new TreeMap<>();
    static {
        try {
            myIp = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        
        InputStream in = Router.class.getResourceAsStream(SERVER_LIST);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        
        try {
            while ((line = br.readLine()) != null) {
                InetAddress ip = InetAddress.getByName(line.split(":")[0]);
                int port = Integer.parseInt(line.split(":")[1]);
                Node node = new Node(ip, port);
                Integer key = hash(node);
                nodes.put(key, node);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Integer hash(byte[] b) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(b);
            return ByteBuffer.wrap(md.digest()).getInt();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static Integer hash(byte[] b, int i) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(b);
            md.update(ByteBuffer.allocate(4).putInt(i).array());
            return ByteBuffer.wrap(md.digest()).getInt();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static Integer hash(Node node) {
        return hash(node.getAddress().getAddress(), node.getPort());
    }

    public static boolean isMe(Node node) {
        return node.getAddress().equals(myIp) && node.getPort() == myPort;
    }

    public static synchronized Node forward(byte[] key) {
        int hashedKey = hash(key).intValue();
        int previousKey = 0;
        Node firstActiveNode = null;
        for (Entry<Integer, Node> entry : nodes.entrySet()) {
            Node node = entry.getValue();
            if (firstActiveNode == null) {
                if (System.currentTimeMillis() - node.getLastUpdateTime() <= 60000) {
                    firstActiveNode = node;
                }
            }
            if (previousKey < hashedKey && entry.getKey().intValue() >= hashedKey) {
                if (System.currentTimeMillis() - node.getLastUpdateTime() <= 60000) {
                    return node;                    
                } else {
                    // This node hasn't broadcasted in over 60s, so check next one
                    continue;
                }
            } else {
                previousKey = entry.getKey().intValue();
            }
        }
        return firstActiveNode;
    }
    
    public static synchronized void update(InetAddress ip, int port) {
        Integer key = hash(ip.getAddress(), port);
        Node node = nodes.get(key);
        if (node == null) {
            nodes.put(key, new Node(ip, port));
        } else {
            node.updateLastUpdateTime();
        }
    }
    
    public static synchronized void destory(InetAddress ip, int port) {
        Integer key = hash(ip.getAddress(), port);
        nodes.remove(key);
    }
    
    public static synchronized List<Node> getActiveNodes() {
        List<Node> activeNodes = new ArrayList<>();
        //System.out.println("Active nodes:");
        for (Entry<Integer, Node> entry : nodes.entrySet()) {
            Node node = entry.getValue();
            if (System.currentTimeMillis() - node.getLastUpdateTime() <= 60000) {
                //System.out.println(node.getAddress().toString() + ":" + node.getPort() + " " + entry.getKey());
                activeNodes.add(node);
            }
        }
        return activeNodes;
    }
}
