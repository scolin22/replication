package com.s33263112.cpen431;

public class Command {
    public static byte NONE = 0;
    public static byte PUT = 1;
    public static byte GET = 2;
    public static byte REMOVE = 3;
    public static byte SHUTDOWN = 4;
    public static byte DELETE_ALL = 5;
    public static byte INTERNAL_PUT = 0x21;
    public static byte INTERNAL_GET = 0x22;
    public static byte INTERNAL_REMOVE = 0x23;
    public static byte INTERNAL_BROADCAST = 0x24;
}
