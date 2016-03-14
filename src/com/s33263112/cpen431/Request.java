/*
 * Request parses the received packet's contents.
 */

package com.s33263112.cpen431;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

public class Request {

    public static final int MAX_VALUE_LENGTH = 10000;
    public static final int MAX_REQUEST_LENGTH = 16 + 1 + 32 + 2 + MAX_VALUE_LENGTH + 4 + 4;

    private byte[] requestId;
    private byte command;
    private byte[] key = null;
    private short valueLength = 0;
    private byte[] value = null;
    
    private InetAddress replyAddress = null;
    private int replyPort = 0;
    
    private byte errorCode = ErrorCode.SUCCESS;
    
    private Request() {}
    
    public Request(DatagramPacket packet) {
        byte[] data = packet.getData();
        int dataLength = packet.getLength();
        
        ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        if (dataLength >= 16) {
            requestId = new byte[16];
            byteBuffer.get(requestId, 0, 16);
            dataLength -= 16;
        } else {
            errorCode = ErrorCode.INVALID_REQUEST_ID_LENGTH;
            return;
        }

        if (dataLength >= 1) {
            command = byteBuffer.get();
            dataLength -= 1;
        } else {
            errorCode = ErrorCode.MISSING_COMMAND;
            return;
        }
        
        if (command == Command.INTERNAL_BROADCAST) {
            return;
        }
        
        if (command == Command.GET || command == Command.PUT || command == Command.REMOVE || command == Command.INTERNAL_GET
                || command == Command.INTERNAL_PUT || command == Command.INTERNAL_REMOVE) {
            if (dataLength >= 32) {
                key = new byte[32];
                byteBuffer.get(key, 0, 32);
                dataLength -= 32;
            } else {
                errorCode = ErrorCode.INVALID_KEY_LENGTH;
                return;
            }
        }

        if (command == Command.PUT || command == Command.INTERNAL_PUT) {
            if (dataLength >= 2) {
                valueLength = byteBuffer.getShort();
                dataLength -= 2;
            } else {
                errorCode = ErrorCode.MISSING_VALUE_LENGTH;
                return;
            }
            if (valueLength <= 0 || valueLength > MAX_VALUE_LENGTH) {
                errorCode = ErrorCode.INVALID_VALUE_LENGTH;
                return;
            } else if (dataLength < valueLength) {
                errorCode = ErrorCode.VALUE_TOO_SHORT;
                return;
            } else {
                value = new byte[valueLength];
                byteBuffer.get(value, 0, valueLength);
                dataLength -= valueLength;
            }
        }
        
        if (command == Command.INTERNAL_PUT || command == Command.INTERNAL_GET || command == Command.INTERNAL_REMOVE) {
            if (dataLength >= 6) {
                byte[] buf = new byte[4];
                byteBuffer.get(buf, 0, 4);
                try {
                    replyAddress = InetAddress.getByAddress(buf);
                } catch (UnknownHostException e) {
                    errorCode = ErrorCode.INVALID_INTERNAL_COMMAND;
                    return;
                }
                replyPort = byteBuffer.getInt();
                dataLength -= 8;
            } else {
                errorCode = ErrorCode.INVALID_INTERNAL_COMMAND;
                return;
            }
        }
    }
    
    public byte[] toByteArray() {
        if (command == Command.INTERNAL_GET || command == Command.INTERNAL_REMOVE) {
            return ByteBuffer.allocate(57).order(ByteOrder.LITTLE_ENDIAN)
                    .put(requestId)
                    .put(command)
                    .put(key)
                    .put(replyAddress.getAddress())
                    .putInt(replyPort)
                    .array();
        } else if (command == Command.INTERNAL_PUT) {
            return ByteBuffer.allocate(59 + valueLength).order(ByteOrder.LITTLE_ENDIAN)
                    .put(requestId)
                    .put(command)
                    .put(key)
                    .putShort(valueLength)
                    .put(value)
                    .put(replyAddress.getAddress())
                    .putInt(replyPort)
                    .array();
        } else if (command == Command.INTERNAL_BROADCAST) {
            return ByteBuffer.allocate(17).order(ByteOrder.LITTLE_ENDIAN)
                    .put(requestId)
                    .put(command)
                    .array();
        } else {
            return null;
        }
    }
    
    public byte getErrorCode() {
        return errorCode;
    }
    
    public byte[] getRequestId() {
        return requestId;
    }
    
    public byte getCommand() {
        return command;
    }
    
    public byte[] getKey() {
        return key;
    }
    
    public short getValueLength() {
        return valueLength;
    }
    
    public byte[] getValue() {
        return value;
    }
    
    public InetAddress getReplyAddress() {
        return replyAddress;
    }
    
    public int getReplyPort() {
        return replyPort;
    }
    
    public void setCommand(byte command) {
        this.command = command;
    }
    
    public void setReplyAddress(InetAddress address) {
        replyAddress = address;
    }
    
    public void setReplyPort(int port) {
        replyPort = port;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.byteArrayToHexString(requestId));
        sb.append(" ");
        sb.append(command);
        if (key != null) {
            sb.append(" ");
            sb.append(StringUtils.byteArrayToHexString(key));
            sb.append("|");
            sb.append(Router.hash(key));
        }
        if (value != null) {
            sb.append(" ");
            sb.append(valueLength);
            //sb.append(StringUtils.byteArrayToHexString(value));
        }
        sb.append(" ");
        sb.append(errorCode);
        
        if (replyAddress != null) {
            sb.append(" ");
            sb.append(replyAddress.toString());
            sb.append(":");
            sb.append(replyPort);
        }
        return sb.toString();
    }
    
    public static Request createBroadcastRequest() {
        Request request = new Request();
        request.requestId = new byte[16];
        new Random().nextBytes(request.requestId);
        request.command = Command.INTERNAL_BROADCAST;
        return request;
    }
}
