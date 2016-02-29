package com.s33263112.cpen431;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Cacher implements Runnable {
    private Map<ByteKey, Reply> requestCache = Collections.synchronizedMap(new LinkedHashMap<ByteKey, Reply>());
    private int timeout;
    private volatile boolean running = true;
    
    public Cacher(int timeout) {
        this.timeout = timeout;
        new Thread(this).start();
    }
    
    public void close() {
        running = false;
    }

    public void cache(Request request, Reply reply) {
        requestCache.put(new ByteKey(request.getRequestId()), reply);
    }

    public Reply get(Request request) {
        ByteKey key = new ByteKey(request.getRequestId());
        return requestCache.get(key);
    }

    @Override
    public void run() {
        while (running) {
            if (requestCache.size() != 0) {
                ByteKey key;
                synchronized (requestCache) {
                    key = requestCache.keySet().iterator().next();
                }
                Reply reply = requestCache.get(key);
                long sleepFor = (reply.getTimestamp() + timeout) - System.currentTimeMillis();
                if (sleepFor > 0) {
                    try {
                        Thread.sleep(sleepFor);
                    } catch (InterruptedException e) {
                        // No one is going to interrupt me
                    }
                }
                requestCache.remove(key);
            } else {
                try {
                    // There's nothing in the cache so there will be no cleaning up
                    // to do for the next *timeout* period.
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    // No one is going to interrupt me
                }
            }
        }
    }
}
