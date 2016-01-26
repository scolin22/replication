package com.s33263112.cpen431;

public class ErrorCode {
    public static byte SUCCESS = 0;
    public static byte NON_EXISTANT_KEY = 0x1;
    public static byte OUT_OF_SPACE = 0x2;
    public static byte SYSTEM_OVERLOAD = 0x3;
    public static byte INTERNAL_KVSTORE_FAILURE = 0x4;
    public static byte UNRECOGNIZED_COMMAND = 0x5;
    public static byte INVALID_VALUE_LENGTH = 0x6;
    public static byte INVALID_REQUEST_ID_LENGTH = 0x21;
    public static byte MISSING_COMMAND = 0x22;
    public static byte INVALID_KEY_LENGTH = 0x23;
    public static byte MISSING_VALUE_LENGTH = 0x24;
    public static byte VALUE_TOO_SHORT = 0x25;
}
