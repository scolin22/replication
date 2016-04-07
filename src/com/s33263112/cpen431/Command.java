package com.s33263112.cpen431;

public class Command {
    public static byte NONE = 0;
    public static byte PUT = 1;
    public static byte GET = 2;
    public static byte REMOVE = 3;
    public static byte SHUTDOWN = 4;
    public static byte DELETE_ALL = 5;
    public static byte GET_PID = 7;
    public static byte INTERNAL_PUT = 0x21;
    public static byte INTERNAL_GET = 0x22;
    public static byte INTERNAL_REMOVE = 0x23;
    public static byte INTERNAL_BROADCAST = 0x24;
    public static byte REPLICATE_PUT = 0x25;
    public static byte REPLICATE_GET = 0x26;
    public static byte REPLICATE_PLACEHOLDER = 0x27;
    public static byte REPLICATE_CLEAR = 0x28;
    public static byte REPLICATE_REMOVE = 0x29;
    public static byte GET_STORE_SIZE = 0x30;
    public static byte GET_BACKUP_SIZE = 0x31;
    public static byte GET_FREE_MEMORY = 0x32;
}
