/*
 * Request parses the received packet's contents.
 */

package com.s33263112.cpen431;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Request {

    private final int MAX_VALUE_LENGTH = 10000;

    private byte[] requestId;
    private byte command;
    private byte[] key = null;
    private short valueLength = 0;
    private byte[] value = null;
    
    private byte errorCode = ErrorCode.SUCCESS;
    
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
        
        if (command == Command.GET || command == Command.PUT || command == Command.REMOVE) {
            if (dataLength >= 32) {
                key = new byte[32];
                byteBuffer.get(key, 0, 32);
                dataLength -= 32;
            } else {
                errorCode = ErrorCode.INVALID_KEY_LENGTH;
                return;
            }
        }

        if (command == Command.PUT) {
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.byteArrayToHexString(requestId));
        sb.append(" ");
        sb.append(command);
        sb.append(" ");
        if (key != null) {
            sb.append(StringUtils.byteArrayToHexString(key));
        }
        if (value != null) {
            sb.append(" ");
            sb.append(valueLength);
            //sb.append(" ");
            //sb.append(StringUtils.byteArrayToHexString(value));
        }
        sb.append(" ");
        sb.append(errorCode);
        return sb.toString();
    }
}
