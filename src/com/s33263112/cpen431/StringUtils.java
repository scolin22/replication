package com.s33263112.cpen431;

/**
 * Various static routines to help with strings
 */
public class StringUtils {

    // Copied from http://www.ece.ubc.ca/~matei/EECE411.10/UTILS/com/matei/eece411/util/ByteOrder.java
    private static int ubyte2int(byte x) {
        return ((int)x) & 0x000000FF;
    }

    // Mostly copied from http://www.ece.ubc.ca/~matei/EECE411.10/UTILS/com/matei/eece411/util/StringUtils.java
    public static String byteArrayToHexString(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        StringBuffer buf=new StringBuffer();
        String       str;
        int val;

        for (int i=0; i<bytes.length; i++) {
            val = ubyte2int(bytes[i]);
            str = Integer.toHexString(val);
            while ( str.length() < 2 )
                str = "0" + str;
            buf.append( str );
        }
        return buf.toString().toUpperCase();
    }
}