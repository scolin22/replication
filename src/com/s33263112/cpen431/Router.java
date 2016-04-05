package com.s33263112.cpen431;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Map.Entry;

public class Router {

    private static final String SERVER_LIST = "/util/server.list";
    private static final Integer NUM_REPLICATES = 2; // Maintain 2 additional replicates

    private static Node myNode;
    private static final InetAddress myIp;
    private static final int myPort = Server.port;

    private static final TreeMap<BigInteger, Node> nodes = new TreeMap<>();
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
                if (isMe(node)) {
                    myNode = node;
                }
                BigInteger key = hash(node);
                nodes.put(key, node);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BigInteger hash(byte[] b) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(b);
            return new BigInteger(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static BigInteger hash(byte[] b, int i) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(b);
            md.update(ByteBuffer.allocate(4).putInt(i).array());
            return new BigInteger(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static BigInteger hash(Node node) {
        return hash(node.getAddress().getAddress(), node.getPort());
    }

    public static boolean isMe(Node node) {
        return node.getAddress().equals(myIp) && node.getPort() == myPort;
    }

    public static synchronized Node findNodeForKey(BigInteger hashedKey) {
        BigInteger previousKey = null;
        Node firstActiveNode = null;
        for (Entry<BigInteger, Node> entry : nodes.entrySet()) {
            Node node = entry.getValue();
            if (firstActiveNode == null) {
                if (System.currentTimeMillis() - node.getLastUpdateTime() <= 60000) {
                    firstActiveNode = node;
                }
            }

            if (previousKey != null && previousKey.compareTo(hashedKey) < 0 && entry.getKey().compareTo(hashedKey) >= 0) {
                if (System.currentTimeMillis() - node.getLastUpdateTime() <= 60000) {
                    return node;
                } else {
                    // This node hasn't broadcasted in over 60s, so check next one
                    continue;
                }
            }
            previousKey = entry.getKey();
        }
        return firstActiveNode;
    }

    public static synchronized Node getNodeFromHashID(BigInteger key) {
        return nodes.get(key);
    }

    public static synchronized Node forward(byte[] key) {
        BigInteger hashedKey = hash(key);
        return findNodeForKey(hashedKey);
    }

    //Get who has our replicates
    public static synchronized List<Node> getReplicateServers(Node mainNode) {
        List<Node> replicates = new ArrayList<>();
        if (mainNode == null) return replicates;

        ArrayList<Entry<BigInteger, Node>> nodeList = new ArrayList<>(nodes.entrySet());

        // Get successive nodes
        for (ListIterator<Entry<BigInteger, Node>> it = nodeList.listIterator(); it.hasNext();) {
            Node node = it.next().getValue();
            if (hash(node).equals(hash(mainNode))) {
                int i = 0;
                while (i < NUM_REPLICATES) {
                    if (!it.hasNext()) {
                        it = nodeList.listIterator();
                    }
                    Node n = it.next().getValue();
                    if (n.isAlive()) {
                        replicates.add(n);
                        i++;
                    }
                }
                break;
            }
        }

        return replicates;
    }

    //Get who we're replicating for
    public static synchronized List<Node> getReplicateClients(Node mainNode) {
        List<Node> replicates = new ArrayList<>();
        if (mainNode == null) return replicates;

        ArrayList<Entry<BigInteger, Node>> nodeList = new ArrayList<>(nodes.entrySet());

        // Get prior nodes
        Collections.reverse(nodeList);
        for (ListIterator<Entry<BigInteger, Node>> it = nodeList.listIterator(); it.hasNext();) {
            Node node = it.next().getValue();
            if (hash(node).equals(hash(mainNode))) {
                int i = 0;
                while (i < NUM_REPLICATES) {
                    if (!it.hasNext()) {
                        it = nodeList.listIterator();
                    }
                    Node n = it.next().getValue();
                    if (n.isAlive()) {
                        replicates.add(n);
                        i++;
                    }
                }
                break;
            }
        }

        return replicates;
    }

    public static synchronized List<BigInteger> getReplicateServerIDs(Node mainNode) {
        List<BigInteger> replicateIDs = new ArrayList<>();

        for (Node node : getReplicateServers(mainNode)) {
            replicateIDs.add(Router.hash(node));
        }

        return replicateIDs;
    }

    public static synchronized List<BigInteger> getReplicateClientIDs(Node mainNode) {
        List<BigInteger> replicateIDs = new ArrayList<>();

        for (Node node : getReplicateClients(mainNode)) {
            replicateIDs.add(Router.hash(node));
        }

        return replicateIDs;
    }

    public static synchronized void update(InetAddress ip, int port) {
        BigInteger key = hash(ip.getAddress(), port);
        Node node = nodes.get(key);
        if (node == null) {
            nodes.put(key, new Node(ip, port));
        } else {
            node.updateLastUpdateTime();
        }
    }

    public static synchronized void destroy(InetAddress ip, int port) {
        BigInteger key = hash(ip.getAddress(), port);
        nodes.remove(key);
    }

    public static synchronized List<Node> getActiveNodes() {
        List<Node> activeNodes = new ArrayList<>();
        //System.out.println("Active nodes:");
        for (Entry<BigInteger, Node> entry : nodes.entrySet()) {
            Node node = entry.getValue();
            if (System.currentTimeMillis() - node.getLastUpdateTime() <= 60000) {
                //System.out.println(node.getAddress().toString() + ":" + node.getPort() + " " + entry.getKey());
                activeNodes.add(node);
            }
        }
        return activeNodes;
    }

    public static synchronized List<Node> getAllNodes() {
        List<Node> activeNodes = new ArrayList<>();
        for (Entry<BigInteger, Node> entry : nodes.entrySet()) {
            activeNodes.add(entry.getValue());
        }
        return activeNodes;
    }
 
    public static TreeMap<BigInteger, Node> getNodes() {
        return nodes;
    }

    public static int getMyPort() {
        return myPort;
    }

    public static InetAddress getMyIp() {
        return myIp;
    }

    public static Node getMyNode() {
        return myNode;
    }
}
