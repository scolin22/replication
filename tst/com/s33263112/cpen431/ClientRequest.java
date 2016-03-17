package com.s33263112.cpen431;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

public class ClientRequest {
    private byte[] requestId;
    private byte command = Command.NONE;
    private ByteKey key = null;
    private short valueLength = 0;
    private byte[] value = null;
    private InetAddress address;
    private int port;

    private static byte[] randomByteArray(int n) {
        byte[] b = new byte[n];
        new Random().nextBytes(b);
        return b;
    }

    // This is an invalid request
    public ClientRequest() {
        requestId = randomByteArray(8);
    }
    
    public ClientRequest(byte[] requestId, byte command) {
        this.requestId = requestId;
        this.command = command;
    }
    
    public ClientRequest(byte command) {
        requestId = randomByteArray(16);
        this.command = command;
    }
    
    public ClientRequest(byte[] requestId, byte command, byte[] key) {
        this.requestId = requestId;
        this.command = command;
        this.key = new ByteKey(key);
    }
    
    public ClientRequest(byte command, byte[] key) {
        requestId = randomByteArray(16);
        this.command = command;
        this.key = new ByteKey(key);
    }
    
    public ClientRequest(byte[] requestId, byte command, byte[] key, byte[] value) {
        this.requestId = requestId;
        this.command = command;
        this.key = new ByteKey(key);
        this.valueLength = (short) value.length;
        this.value = value;
    }
    
    public ClientRequest(byte command, byte[] key, byte[] value) {
        requestId = randomByteArray(16);
        this.command = command;
        this.key = new ByteKey(key);
        this.valueLength = (short) value.length;
        this.value = value;
    }
    
    public ClientRequest(byte command, byte[] key, byte[] value, int valueLength) {
        requestId = randomByteArray(16);
        this.command = command;
        this.key = new ByteKey(key);
        this.valueLength = (short) valueLength;
        this.value = value;
    }

    public byte[] getRequest() {
        if (command == Command.NONE) {
            return ByteBuffer.allocate(requestId.length).order(ByteOrder.LITTLE_ENDIAN)
                    .put(requestId)
                    .array();
        } else if (key == null) {
            return ByteBuffer.allocate(17).order(ByteOrder.LITTLE_ENDIAN)
                    .put(requestId)
                    .put(command)
                    .array();
        } else if (valueLength == 0) {
            return ByteBuffer.allocate(17 + key.getKey().length).order(ByteOrder.LITTLE_ENDIAN)
                    .put(requestId)
                    .put(command)
                    .put(key.getKey())
                    .array();
        } else {
            return ByteBuffer.allocate(51 + value.length).order(ByteOrder.LITTLE_ENDIAN)
                    .put(requestId)
                    .put(command)
                    .put(key.getKey())
                    .putShort(valueLength)
                    .put(value)
                    .array();
        }
    }

    public byte[] getRequestId() {
        return requestId;
    }

    public byte getCommand() {
        return command;
    }
    
    public ByteKey getKey() {
        return key;
    }
    
    public short getValueLength() {
        return valueLength;
    }
    
    public byte[] getValue() {
        return value;
    }
    
    public InetAddress getAddress() {
        return address;
    }
    
    public int getPort() {
        return port;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.byteArrayToHexString(requestId));
        sb.append(" ");
        sb.append(command);
        sb.append(" ");
        if (key != null) {
            sb.append(StringUtils.byteArrayToHexString(key.getKey()));
        }
        sb.append(" ");
        if (value != null) {
            sb.append(valueLength);
            //sb.append(" ");
            //sb.append(StringUtils.byteArrayToHexString(value));
        }
        return sb.toString();
    }
}
