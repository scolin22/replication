/*
 * RequestHandler determines the command, executes the command, and replies.
 * Items won't be added to the store if the store size is MAX_STORE_SIZE or if the
 * amount of free memory is less than MIN_FREE_MEMORY.
 */

package com.s33263112.cpen431;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestHandler implements Runnable {

    private static final Map<ByteKey, byte[]> store = new ConcurrentHashMap<>();
    private static final int MAX_STORE_SIZE = 100000;

    // 8MB is used as a limit because the programs begins throwing OutOfMemoryError when
    // the amount of free memory reaches 7MB.
    private static final int MIN_FREE_MEMORY = 8388608;

    private DatagramPacket packet;

    public RequestHandler(DatagramPacket packet) {
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
        } else if (request.getCommand() == Command.GET || request.getCommand() == Command.PUT || request.getCommand() == Command.REMOVE) {
            InetAddress forwardIp = Router.forward(request.getKey());
            if (Router.isMe(forwardIp)) {
                if (request.getCommand() == Command.GET) {
                    reply = handleGet(request);
                } else if (request.getCommand() == Command.PUT) {
                    reply = handlePut(request);
                } else if (request.getCommand() == Command.REMOVE) {
                    reply = handleRemove(request);
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
                forwardRequest(request, forwardIp);
            }
        } else if (request.getCommand() == Command.INTERNAL_GET) {
            reply = handleGet(request);
            sendReply(reply, request.getReplyAddress(), request.getReplyPort());
        } else if (request.getCommand() == Command.INTERNAL_PUT) {
            reply = handlePut(request);
            sendReply(reply, request.getReplyAddress(), request.getReplyPort());
        } else if (request.getCommand() == Command.INTERNAL_REMOVE) {
            reply = handleRemove(request);
            sendReply(reply, request.getReplyAddress(), request.getReplyPort());
        } else if (request.getCommand() == Command.SHUTDOWN) {
            handleShutdown(request);
        } else if (request.getCommand() == Command.DELETE_ALL) {
            reply = handleDeleteAll(request);
            sendReply(reply);
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
        sendReply(new Reply(request, ErrorCode.SUCCESS));
        Server.close();
    }
    
    private Reply handleDeleteAll(Request request) {
        store.clear();
        System.gc();
        return new Reply(request, ErrorCode.SUCCESS);
    }
    
    private boolean returnIfCached(Request request) {
        Reply reply = Server.cacher.get(request);
        if (reply != null) {
            //System.out.println("Request: " + request.toString() + " CACHED");
            sendReply(reply);
            return true;
        } else {
            return false;
        }
    }
    
    private void sendReply(Reply reply) {
        System.out.println("Reply: " + reply.toString());
        Server.networkHandler.sendBytes(reply.toByteArray(), packet.getAddress(), packet.getPort());
    }
    
    private void sendReply(Reply reply, InetAddress address, int port) {
        System.out.println("Reply: " + reply.toString());
        Server.networkHandler.sendBytes(reply.toByteArray(), address, port);
    }
    
    private void forwardRequest(Request request, InetAddress forwardIp) {
        System.out.println("Forwarding Request: " + request.toString());
        Server.networkHandler.sendBytes(request.toByteArray(), forwardIp, packet.getPort());
    }
}
