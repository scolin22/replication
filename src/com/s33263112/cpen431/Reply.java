/*
 * Reply creates a reply for the provided request based on the ErrorCode.
 * Reply.getReply returns the byte array which should be sent.
 */

package com.s33263112.cpen431;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Reply {

    private byte[] requestId;
    private byte errorCode;
    private short valueLength = 0;
    private byte[] value = null;
    private long timestamp = System.currentTimeMillis();
    
    public Reply(Request request, byte errorCode) {
        this.requestId = request.getRequestId();
        this.errorCode = errorCode;
    }
    
    public Reply(byte[] requestId, byte errorCode) {
        this.requestId = requestId;
        this.errorCode = errorCode;
    }
    
    public Reply(Request request, byte errorCode, byte[] value) {
        this.requestId = request.getRequestId();
        this.errorCode = errorCode;
        this.valueLength = (short) value.length;
        this.value = value;
    }
    
    public byte[] getReply() {
        if (valueLength == 0) {
            return ByteBuffer.allocate(17).order(ByteOrder.LITTLE_ENDIAN)
                    .put(requestId)
                    .put(errorCode)
                    .array();
        } else {
            return ByteBuffer.allocate(19 + valueLength).order(ByteOrder.LITTLE_ENDIAN)
                    .put(requestId)
                    .put(errorCode)
                    .putShort(valueLength)
                    .put(value)
                    .array();
        }
    }

    public byte[] getRequestId() {
        return requestId;
    }
    
    public byte getErrorCode() {
        return errorCode;
    }
    
    public short getValueLength() {
        return valueLength;
    }
    
    public byte[] getValue() {
        return value;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.byteArrayToHexString(requestId));
        sb.append(" ");
        sb.append(errorCode);
        sb.append(" ");
        if (value != null) {
            sb.append(valueLength);
            //sb.append(" ");
            //sb.append(StringUtils.byteArrayToHexString(value));
        }
        return sb.toString();
    }
}
