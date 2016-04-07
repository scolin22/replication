/*
 * ByteKey is a wrapper class around a byte array to allow the byte array to be used as a key for a Set/Map.
 */

package com.s33263112.cpen431;

import java.util.Arrays;

public class ByteKey {
    private byte[] key;
    
    public ByteKey(byte[] key) {
        this.key = key;
    }
    
    public byte[] getKey() {
        return key;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof ByteKey) {
            return Arrays.equals(key, ((ByteKey)other).key);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }

    @Override
    public String toString() {
        return StringUtils.byteArrayToHexString(key);
    }
}
