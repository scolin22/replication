package com.s33263112.cpen431;

import java.util.Map;
import java.util.Random;

/**
 * Created by colin_000 on 3/31/2016.
 */
public class RequestRunnable implements Runnable {

    Map<ByteKey, byte[]> src;
    Node node;
    Request request;
    Random random;

    private static byte[] randomByteArray(int n) {
        byte[] b = new byte[n];
        new Random().nextBytes(b);
        return b;
    }

    public RequestRunnable(Node node, byte command, Map<ByteKey, byte[]> src) {
        this.src = src;
        this.node = node;
        this.request = new Request(command);
        this.random = new Random();
    }

    public RequestRunnable(Node node, byte command) {
        this.node = node;
        this.request = new Request(command);
        this.random = new Random();
    }

    @Override
    public void run() {
        if (this.request.getCommand() == Command.REPLICATE_PUT) {
            // Send every key to the node
            this.request.setReplyAddress(Router.getMyNode().getAddress());
            this.request.setReplyPort(Router.getMyNode().getPort());

            if (src.isEmpty()) {
                this.request.setRequestId(randomByteArray(16));
                this.request.setCommand(Command.REPLICATE_PLACEHOLDER);
                Server.networkHandler.sendBytes(request.toByteArray(), this.node.getAddress(), this.node.getPort());
            } else {
                for (ByteKey key : src.keySet()) {
                    this.request.setRequestId(randomByteArray(16));
                    this.request.setKey(key.getKey());
                    this.request.setValueLength((short) src.get(key).length);
                    this.request.setValue(src.get(key));
                    Server.networkHandler.sendBytes(request.toByteArray(), this.node.getAddress(), this.node.getPort());
                }
            }
        } else if (this.request.getCommand() == Command.REPLICATE_GET) {
            this.request.setRequestId(randomByteArray(16));
            this.request.setReplyAddress(Router.getMyNode().getAddress());
            this.request.setReplyPort(Router.getMyNode().getPort());
            Server.networkHandler.sendBytes(request.toByteArray(), this.node.getAddress(), this.node.getPort());
        }

    }
}
