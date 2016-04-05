package com.s33263112.cpen431;

import java.math.BigInteger;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by colin_000 on 3/29/2016.
 */
//TODO: Smart way is to create a new DataStore class so we don't repeat put and get methods
public class Backup {
    private static final Map<BigInteger, Map<ByteKey, byte[]>> backups = new ConcurrentHashMap<>();

    private static final int MIN_FREE_MEMORY = 8388608;
    private static final int MAX_STORE_SIZE = 100000;

    public static synchronized void checkNodePrime() {
        boolean looped = false;
        for (ListIterator<Node> iter = Router.getAllNodes().listIterator(); iter.hasNext() && !looped;) {
            Node node = iter.next();
            if (!iter.hasNext()) {
                iter = Router.getAllNodes().listIterator();
                looped = true; // Only cycle through the list once
            }
            if (Router.isMe(node)) {
                continue;
            }
            if (System.currentTimeMillis() - node.getLastUpdateTime() > 30000) {
                // Remove this dead node from the all nodes list
                BigInteger backupID = Router.hash(node.getAddress().getAddress(), node.getPort());
                //System.out.println("Node: " + backupID + " died.");
                if (Router.isMe(iter.next())) {
                    //System.out.println("It was a client node.");
                    Backup.merge(backupID, RequestHandler.getStore());
                    Backup.replicate(RequestHandler.getStore(), Router.getReplicateServers(Router.getMyNode()));
                    Router.destroy(node.getAddress(), node.getPort());
                } else if (System.currentTimeMillis() - node.getLastUpdateTime() > 60000) {
                    Router.destroy(node.getAddress(), node.getPort()); // Don't remove the node unless it's really dead. We still might need to save it.
                }
            }
        }
    }

    public static synchronized void pruneReplicates() {
        List<BigInteger> replicateIDs = Router.getReplicateClientIDs(Router.getMyNode()); // Supposed to have
        Set<BigInteger> backups = Backup.getBackupList(); // Currently have
        for (BigInteger backupID : backups) {
            // Go through this node's replication list, remove all irrelevant nodes
            if (!replicateIDs.contains(backupID)) { // We have a replicate that's no longer useful
                Backup.delete(backupID);
            }
        }
    }

    public static synchronized void fetchReplicates() {
        List<BigInteger> replicateIDs = Router.getReplicateClientIDs(Router.getMyNode()); // Supposed to have
        Set<BigInteger> backups = Backup.getBackupList(); // Currently have
        for (BigInteger replicateID : replicateIDs) {
            // Request replication data from missing nodes
            if (!backups.contains(replicateID)) { // We do not currently have a replicate of a node that we're supposed to have.
                Backup.request(Router.getNodeFromHashID(replicateID));
            }
        }
    }

    public static synchronized void printReplicates() {
        System.out.println("Holding replicates for:");
        for (BigInteger replicateID : Backup.getBackupList()) {
            System.out.println("-- " + replicateID);
        }
        System.out.println("Supposed to have replicates for:");
        for (BigInteger replicateID : Router.getReplicateClientIDs(Router.getMyNode())) {
            System.out.println("-- " + replicateID);
        }
    }

    public static synchronized boolean put(BigInteger backupID, ByteKey key, byte[] value) {
        Map<ByteKey, byte[]> store;
        if (backups.containsKey(backupID)) {
            store = backups.get(backupID);
        } else {
            backups.put(backupID, new ConcurrentHashMap<>());
            store = backups.get(backupID);
        }

        if (Runtime.getRuntime().freeMemory() <= MIN_FREE_MEMORY) {
            System.out.println("OUT OF MEMORY IN BACKUP");
            // Stop adding more items if we have less than MIN_FREE_MEMORY of memory left
            return false;
        } else if (store.size() >= MAX_STORE_SIZE) { //TODO: need to get total size across all stores
            // Don't add more items because limit of 100000 items reached
            return false;
        } else {
            store.put(key, value);
            return true;
        }
    }

    public static synchronized void replicate(Map<ByteKey, byte[]> src, List<Node> replicates) {
        for (Node node : replicates) {
            replicate(src, node);
        }
    }

    public static synchronized void replicate(Map<ByteKey, byte[]> src, Node replicate) {
        if (replicate == null) return;
        //System.out.println("Sending replicates to: " + Router.hash(replicate));
        Server.pool.execute(new RequestRunnable(replicate, Command.REPLICATE_PUT, src));
    }

    public static synchronized void request(Node node) {
        if (node == null) return;
        //System.out.println("Requesting replicate from: " + Router.hash(node));
        Server.pool.execute(new RequestRunnable(node, Command.REPLICATE_GET));
    }

    public static synchronized void placeHolder(BigInteger backupID) {
        if (!backups.containsKey(backupID)) {
            //System.out.println("Creating placeholder for: " + backupID);
            backups.put(backupID, new ConcurrentHashMap<>());
        }
    }

    public static synchronized void delete(BigInteger backupID) {
        //System.out.println("Deleting: " + backupID);
        backups.remove(backupID);
    }

    public static synchronized void remove(ByteKey key, BigInteger backupID) {
        System.out.println("Deleting: " + backupID);
        backups.get(backupID).remove(key);
    }

    public static synchronized void merge(BigInteger backupID, Map<ByteKey, byte[]> dst) {
        Map<ByteKey, byte[]> source;
        if (backups.containsKey(backupID)) {
            //System.out.println("Merging with: " + backupID);
            source = backups.get(backupID);
            dst.putAll(source);
            delete(backupID);
        } else {
            return;
        }
    }

    public static synchronized void clear(BigInteger backupID) {
        backups.get(backupID).clear();
    }

    public static synchronized Set<BigInteger> getBackupList() {
        return backups.keySet();
    }
}
