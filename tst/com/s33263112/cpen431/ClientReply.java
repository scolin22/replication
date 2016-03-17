package com.s33263112.cpen431;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ClientReply {
    
    private byte[] requestId;
    private byte errorCode;
    private short valueLength = 0;
    private byte[] value = null;
    
    public ClientReply(byte[] data) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        requestId = new byte[16];
        byteBuffer.get(requestId, 0, 16);
        
        errorCode = byteBuffer.get();
        
        if (byteBuffer.hasRemaining()) {
            valueLength = byteBuffer.getShort();
            value = new byte[valueLength];
            byteBuffer.get(value, 0, valueLength);
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.byteArrayToHexString(requestId));
        sb.append(" ");
        sb.append(errorCode);
        if (value != null) {
            sb.append(" ");
            sb.append(valueLength);
            //sb.append(" ");
            //sb.append(StringUtils.byteArrayToHexString(value));
        }
        return sb.toString();
    }
}
