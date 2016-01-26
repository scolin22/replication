/*
 * RequestHandler determines the command, executes the command, and replies.
 * Items won't be added to the store if the store size is MAX_STORE_SIZE or if the
 * amount of free memory is less than MIN_FREE_MEMORY.
 */

package com.s33263112.cpen431;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestHandler implements Runnable {

    private static Map<ByteKey, byte[]> store = new ConcurrentHashMap<>();
    private static Map<ByteKey, Reply> requestCache = Collections.synchronizedMap(new LinkedHashMap<ByteKey, Reply>());
    private static final int MAX_STORE_SIZE = 100000;
    // TODO: instead of MAX_CACHE_SIZE, give each entry a TTL and remove from set after the time
    private static final int MAX_CACHE_SIZE = 1000;
    // 8MB is used as a limit because the programs begins throwing OutOfMemoryError when
    // the amount of free memory reaches 7MB.
    private static final int MIN_FREE_MEMORY = 8388608;
    
    private DatagramSocket socket;
    private DatagramPacket packet;

    public RequestHandler(DatagramSocket socket, DatagramPacket packet) {
        this.socket = socket;
        this.packet = packet;
    }
    
    @Override
    public void run() {
        Request request = new Request(packet);
        System.out.println("Request: " + request.toString());
        Reply reply = null;
        if (request.getErrorCode() == ErrorCode.INVALID_REQUEST_ID_LENGTH) {
            // Just drop this request
        } else if (returnIfCached(request)) {
            // Request Id was in cache
        } else if (request.getErrorCode() != ErrorCode.SUCCESS) {
            reply = new Reply(request, request.getErrorCode());
            sendReply(reply);
        } else if (request.getCommand() == Command.GET) {
            reply = handleGet(request);
        } else if (request.getCommand() == Command.PUT) {
            reply = handlePut(request);
        } else if (request.getCommand() == Command.REMOVE) {
            reply = handleRemove(request);
        } else if (request.getCommand() == Command.SHUTDOWN) {
            handleShutdown(request);
        } else if (request.getCommand() == Command.DELETE_ALL) {
            reply = handleDeleteAll(request);
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
            // Stop adding more items if we have less than MIN_FREE_MEMORY of memory left
        } else if (requestCache.size() >= MAX_CACHE_SIZE) {
            // Remove the first item and add the new item
            requestCache.remove(requestCache.keySet().iterator().next());
            requestCache.put(new ByteKey(request.getRequestId()), reply);
        } else {
            requestCache.put(new ByteKey(request.getRequestId()), reply);
        }
    }

    private Reply handleGet(Request request) {
        ByteKey key = new ByteKey(request.getKey());
        byte[] value = store.get(key);
        if (value == null) {
            Reply reply = new Reply(request, ErrorCode.NON_EXISTANT_KEY);
            sendReply(reply);
            return reply;
        } else {
            Reply reply = new Reply(request, ErrorCode.SUCCESS, value);
            sendReply(reply);
            return reply;
        }
    }
    
    private Reply handlePut(Request request) {
        ByteKey key = new ByteKey(request.getKey());
        if (put(key, request.getValue())) {
            Reply reply = new Reply(request, ErrorCode.SUCCESS);
            sendReply(reply);
            return reply;
        } else {
            Reply reply = new Reply(request, ErrorCode.OUT_OF_SPACE);
            sendReply(reply);
            return reply;
        }
    }
    
    private Reply handleRemove(Request request) {
        ByteKey key = new ByteKey(request.getKey());
        byte[] value = store.remove(key);
        if (value == null) {
            Reply reply = new Reply(request, ErrorCode.NON_EXISTANT_KEY);
            sendReply(reply);
            return reply;
        } else {
            Reply reply = new Reply(request, ErrorCode.SUCCESS);
            sendReply(reply);
            return reply;
        }
    }
    
    private void handleShutdown(Request request) {
        sendReply(new Reply(request, ErrorCode.SUCCESS));
        socket.close();
    }
    
    private Reply handleDeleteAll(Request request) {
        store.clear();
        Reply reply = new Reply(request, ErrorCode.SUCCESS);
        sendReply(reply);
        return reply;
    }
    
    private boolean returnIfCached(Request request) {
        ByteKey requestId = new ByteKey(request.getRequestId());
        Reply reply = requestCache.get(requestId);
        if (reply != null) {
            sendReply(reply);
            return true;
        } else {
            return false;
        }
    }
    
    private void sendReply(Reply reply) {
        System.out.println("Reply: " + reply.toString());
        byte[] buffer = reply.getReply();
        DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
        try {
            socket.send(sendPacket);
        } catch (IOException ioe) {
            // socket was likely closed by another thread. We can safely proceed to let this thread terminate.
        }
    }
}
