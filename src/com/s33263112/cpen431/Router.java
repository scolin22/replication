package com.s33263112.cpen431;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map.Entry;
import java.util.TreeMap;

public class Router {

    private static final String SERVER_LIST = "/util/server.list";
    
    private static final InetAddress myIp;

    private static final TreeMap<Integer, Node> nodes = new TreeMap<>();
    static {
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");

            BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            myIp = InetAddress.getByName(in.readLine());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        InputStream in = Router.class.getResourceAsStream(SERVER_LIST);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        
        try {
            while ((line = br.readLine()) != null) {
                InetAddress ip = InetAddress.getByName(line.split(":")[0]);
                Integer key = hash(ip.getAddress());
                nodes.put(key, new Node(ip));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static Integer hash(byte[] b) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(b);
            return ByteBuffer.wrap(md.digest()).getInt();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isMe(InetAddress ip) {
        return ip.equals(myIp);
    }

    public static synchronized InetAddress forward(byte[] key) {
        int hashedKey = hash(key).intValue();
        int previousKey = 0;
        for (Entry<Integer, Node> entry : nodes.entrySet()) {
            if (previousKey < hashedKey && entry.getKey().intValue() >= hashedKey) {
                Node node = entry.getValue();
                if (System.currentTimeMillis() - node.getLastUpdateTime() <= 60000) {
                    return node.getAddress();                    
                } else {
                    // This node hasn't broadcasted in over 60s, so check next one
                    continue;
                }
            } else {
                previousKey = entry.getKey().intValue();
            }
        }
        return nodes.firstEntry().getValue().getAddress();
    }
    
    public static synchronized void update(InetAddress ip) {
        Integer key = hash(ip.getAddress());
        Node node = nodes.get(key);
        if (node == null) {
            nodes.put(key, new Node(ip));
        } else {
            node.updateLastUpdateTime();
        }
    }
    
    public static TreeMap<Integer, Node> getNodes() {
        return nodes;
    }
}
