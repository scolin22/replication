/*
 * RequestHandler determines the command, executes the command, and replies.
 * Items won't be added to the store if the store size is MAX_STORE_SIZE or if the
 * amount of free memory is less than MIN_FREE_MEMORY.
 */

package com.s33263112.cpen431;

import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class RequestHandler implements Runnable {

    private static final Map<ByteKey, byte[]> store = new ConcurrentHashMap<>();
    private static final int MAX_STORE_SIZE = 100000;

    // 8MB is used as a limit because the programs begins throwing OutOfMemoryError when
    // the amount of free memory reaches 7MB.
    private static final int MIN_FREE_MEMORY = 8388608;

    private DatagramPacket packet;

    public static Map<ByteKey, byte[]> getStore() {
        return store;
    }

    public RequestHandler(DatagramPacket packet) {
        this.packet = packet;
    }

    private static byte[] randomByteArray(int n) {
        byte[] b = new byte[n];
        new Random().nextBytes(b);
        return b;
    }
    
    @Override
    public void run() {
        Request request = new Request(packet);
        //System.out.println("Request: " + request.toString());
        Reply reply = null;
        if (request.getErrorCode() == ErrorCode.INVALID_REQUEST_ID_LENGTH) {
            // Just drop this request
        } else if (returnIfCached(request)) {
            // Request Id was in cache
        } else if (request.getErrorCode() != ErrorCode.SUCCESS) {
            reply = new Reply(request, request.getErrorCode());
            sendReply(reply);
        } else if (request.getCommand() == Command.GET || request.getCommand() == Command.PUT || request.getCommand() == Command.REMOVE) {
            Node forwardNode = Router.forward(request.getKey());
            if (Router.isMe(forwardNode)) {
                if (request.getCommand() == Command.GET) {
                    reply = handleGet(request);
                } else if (request.getCommand() == Command.PUT) {
                    reply = handlePut(request);
                    if (reply.getErrorCode() == ErrorCode.SUCCESS) {
                        replicatePutRequest(request);
                    }
                } else if (request.getCommand() == Command.REMOVE) {
                    reply = handleRemove(request);
                    if (reply.getErrorCode() == ErrorCode.SUCCESS) {
                        replicateRemoveRequest(request);
                    }
                }
                sendReply(reply);
            } else {
                request.setReplyAddress(packet.getAddress());
                request.setReplyPort(packet.getPort());
                if (request.getCommand() == Command.GET) {
                    request.setCommand(Command.INTERNAL_GET);
                } else if (request.getCommand() == Command.PUT) {
                    request.setCommand(Command.INTERNAL_PUT);
                } else if (request.getCommand() == Command.REMOVE) {
                    request.setCommand(Command.INTERNAL_REMOVE);
                }
                forwardRequest(request, forwardNode);
            }
        } else if (request.getCommand() == Command.INTERNAL_GET) {
            reply = handleGet(request);
            sendReply(reply, request.getReplyAddress(), request.getReplyPort());
        } else if (request.getCommand() == Command.INTERNAL_PUT) {
            reply = handlePut(request);
            sendReply(reply, request.getReplyAddress(), request.getReplyPort());
            if (reply.getErrorCode() == ErrorCode.SUCCESS) {
                replicatePutRequest(request);
            }
        } else if (request.getCommand() == Command.INTERNAL_REMOVE) {
            reply = handleRemove(request);
            sendReply(reply, request.getReplyAddress(), request.getReplyPort());
            if (reply.getErrorCode() == ErrorCode.SUCCESS) {
                replicateRemoveRequest(request);
            }
        } else if (request.getCommand() == Command.REPLICATE_PUT) {
            handleReplicatePut(request);
        } else if (request.getCommand() == Command.REPLICATE_GET) {
            handleReplicateGet(request);
        } else if (request.getCommand() == Command.REPLICATE_REMOVE) {
            handleReplicateRemove(request);
        } else if (request.getCommand() == Command.REPLICATE_CLEAR) {
            handleReplicateClear(request);
        } else if (request.getCommand() == Command.REPLICATE_PLACEHOLDER) {
            handleReplicatePlaceholder(request);
        } else if (request.getCommand() == Command.SHUTDOWN) {
            handleShutdown(request);
        } else if (request.getCommand() == Command.DELETE_ALL) {
            reply = handleDeleteAll(request);
            sendReply(reply);
            if (reply.getErrorCode() == ErrorCode.SUCCESS) {
                replicateClearRequest(request);
            }
            System.gc();
        } else if (request.getCommand() == Command.GET_PID) {
            sendReply(handleGetPid(request));
        } else if (request.getCommand() == Command.INTERNAL_BROADCAST) {
            Router.update(packet.getAddress(), packet.getPort());
        } else if (request.getCommand() == Command.GET_STORE_SIZE) {
            sendReply(handleGetStoreSize(request));
        } else if (request.getCommand() == Command.GET_BACKUP_SIZE) {
            sendReply(handleGetBackupSize(request));
        } else if (request.getCommand() == Command.GET_FREE_MEMORY) {
            Reply r = handleGetFreeMemory(request);
            sendReply(r);
        } else {
            reply = new Reply(request, ErrorCode.UNRECOGNIZED_COMMAND);
            sendReply(reply);
        }
        
        if (reply != null) {
            cache(request, reply);
        }
    }
    
    /*
     * put and cache are static synchronized so only one thread can access either function
     * at a time. Both of these functions add items to the store and cache so they can
     * indefinitely increase in size and so I need to make sure there won't be any race
     * conditions where multiple threads add items at the same time.
     */
    private static synchronized boolean put(ByteKey key, byte[] value) {
        if (Runtime.getRuntime().freeMemory() <= MIN_FREE_MEMORY) {
            System.out.println("OUT OF MEMORY IN REQUESTHANDLER");
            // Stop adding more items if we have less than MIN_FREE_MEMORY of memory left
            return false;
        } else if (store.size() >= MAX_STORE_SIZE) {
            // Don't add more items because limit of 100000 items reached
            return false;
        } else {
            store.put(key, value);
            return true;
        }
    }

    private static synchronized void cache(Request request, Reply reply) {
        if (Runtime.getRuntime().freeMemory() <= MIN_FREE_MEMORY) {
            // Stop caching more items if we have less than MIN_FREE_MEMORY of memory left.
            // The garbage collector doesn't always run right away so we need to be safe.
        } else {
            Server.cacher.cache(request, reply);
        }
    }

    private Reply handleGet(Request request) {
        ByteKey key = new ByteKey(request.getKey());
        byte[] value = store.get(key);
        if (value == null) {
            return new Reply(request, ErrorCode.NON_EXISTANT_KEY);
        } else {
            return new Reply(request, ErrorCode.SUCCESS, value);
        }
    }
    
    private Reply handlePut(Request request) {
        ByteKey key = new ByteKey(request.getKey());
        if (put(key, request.getValue())) {
            return new Reply(request, ErrorCode.SUCCESS);
        } else {
            return new Reply(request, ErrorCode.OUT_OF_SPACE);
        }
    }

    public void handleReplicatePut(Request request) {
        ByteKey key = new ByteKey(request.getKey());
        BigInteger backupID = Router.hash(request.getReplyAddress().getAddress(), request.getReplyPort());
        Backup.put(backupID, key, request.getValue());
    }

    private void replicatePutRequest(Request r) {
        Request replicateRequest = new Request(Command.REPLICATE_PUT);
        replicateRequest.setRequestId(randomByteArray(16));
        replicateRequest.setKey(r.getKey());
        replicateRequest.setValue(r.getValue());
        replicateRequest.setValueLength(r.getValueLength());
        replicateRequest.setReplyAddress(Router.getMyIp());
        replicateRequest.setReplyPort(Router.getMyPort());
        for (Node replicateNode : Router.getReplicateServers(Router.getMyNode())) {
            forwardRequest(replicateRequest, replicateNode);
        }
    }

    private void replicateRemoveRequest(Request r) {
        Request replicateRequest = new Request(Command.REPLICATE_REMOVE);
        replicateRequest.setRequestId(randomByteArray(16));
        replicateRequest.setKey(r.getKey());
        replicateRequest.setReplyAddress(Router.getMyIp());
        replicateRequest.setReplyPort(Router.getMyPort());
        for (Node replicateNode : Router.getReplicateServers(Router.getMyNode())) {
            forwardRequest(replicateRequest, replicateNode);
        }
    }

    private void handleReplicateGet(Request request) {
        Backup.replicate(store, Router.findNodeForKey(Router.hash(request.getReplyAddress().getAddress(), request.getReplyPort())));
    }

    private void handleReplicateRemove(Request request) {
        Backup.remove(new ByteKey(request.getKey()), Router.hash(request.getReplyAddress().getAddress(), request.getReplyPort()));
    }

    private void handleReplicatePlaceholder(Request request) {
        Backup.placeHolder(Router.hash(request.getReplyAddress().getAddress(), request.getReplyPort()));
    }
    
    private Reply handleRemove(Request request) {
        ByteKey key = new ByteKey(request.getKey());
        byte[] value = store.remove(key);
        if (value == null) {
            return new Reply(request, ErrorCode.NON_EXISTANT_KEY);
        } else {
            return new Reply(request, ErrorCode.SUCCESS);
        }
    }

    private void handleShutdown(Request request) {
        Server.close1();
        sendReply(new Reply(request, ErrorCode.SUCCESS));
        System.out.println("Shutting down because of shutdown command");
        Server.close2();
    }
    
    private Reply handleDeleteAll(Request request) {
        store.clear();
        System.gc();
        return new Reply(request, ErrorCode.SUCCESS);
    }

    private void replicateClearRequest(Request r) {
        Request replicateRequest = new Request(Command.REPLICATE_CLEAR);
        replicateRequest.setRequestId(randomByteArray(16));
        replicateRequest.setReplyAddress(Router.getMyIp());
        replicateRequest.setReplyPort(Router.getMyPort());
        for (Node replicateNode : Router.getReplicateServers(Router.getMyNode())) {
            forwardRequest(replicateRequest, replicateNode);
        }
    }

    private void handleReplicateClear(Request request) {
        Backup.clear(Router.hash(request.getReplyAddress().getAddress(), request.getReplyPort()));
        System.gc();
    }

    private Reply handleGetPid(Request request) {
        int pid = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        return new Reply(request, ErrorCode.SUCCESS, ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(pid).array());
    }

    private Reply handleGetStoreSize(Request request) {
        return new Reply(request, ErrorCode.SUCCESS, ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(store.size()).array());
    }
    
    private Reply handleGetBackupSize(Request request) {
        return new Reply(request, ErrorCode.SUCCESS, ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(Backup.getTotalSize()).array());
    }
    
    private Reply handleGetFreeMemory(Request request) {
        return new Reply(request, ErrorCode.SUCCESS, ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(Runtime.getRuntime().freeMemory()).array());
    }

    private boolean returnIfCached(Request request) {
        Reply reply = Server.cacher.get(request);
        if (reply != null) {
            //System.out.println("Request: " + request.toString() + " CACHED");
            if (request.getCommand() == Command.INTERNAL_GET || request.getCommand() == Command.INTERNAL_PUT || request.getCommand() == Command.INTERNAL_REMOVE) {
                sendReply(reply, request.getReplyAddress(), request.getReplyPort());
            } else {
                sendReply(reply);
            }
            return true;
        } else {
            return false;
        }
    }
    
    private void sendReply(Reply reply) {
        //System.out.println("Reply: " + reply.toString());
        Server.networkHandler.sendBytes(reply.toByteArray(), packet.getAddress(), packet.getPort());
    }
    
    private void sendReply(Reply reply, InetAddress address, int port) {
        //System.out.println("Reply to " + address.toString() + ":" + port + ": " + reply.toString());
        Server.networkHandler.sendBytes(reply.toByteArray(), address, port);
    }
    
    private void forwardRequest(Request request, Node node) {
        //System.out.println("Forwarding to " + node.getAddress().toString() + ":" + node.getPort() + ": " + request.toString());
        Server.networkHandler.sendBytes(request.toByteArray(), node.getAddress(), node.getPort());
    }

    public static void printStoreSize() {
        System.out.println("Holding: " + store.size() + " keys");
    }
}
