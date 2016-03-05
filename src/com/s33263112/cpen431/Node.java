package com.s33263112.cpen431;

import java.net.InetAddress;

public class Node {

    private InetAddress address = null;
    private long lastUpdateTime = System.currentTimeMillis();
    
    public Node(InetAddress address) {
        this.address = address;
    }
    
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public void updateLastUpdateTime() {
        lastUpdateTime = System.currentTimeMillis();
    }
    
    public InetAddress getAddress() {
        return address;
    }
}
