slice_name: ubc_cpen431_1  
groupid: 1B  
members:  
Amitoj Kooner (33263112)  
Bojan Stefanovic (14842124)  
Colin Stone (31645112)  
Kevin Hui (38604112)  

A list of all the nodes which are currently running the service are located in `server.list`.

The shutdown command is handled in `src/com/s33263112/cpen431/RequestHandler.java:149` in the `handleShutdown` function.

The jar file can be run as
`java -Xmx64m -jar A5.jar 13112` where the last argument is **port**.

The java code can also be run instead. The main function is in *src/com/s33263112/cpen431/A5.java*.

This code works with Java 8.

# Test client

We are running the test client on an Amazon T2.micro instance

# Testing

Most of the testing was done by the a2_tests.jar provided by the instructors. I also ran my own tests to test several corner cases and to flood the service with requests to see how it would perform under that load but didn't commit that code because it was extremely ugly.

# Design Choices

The Request and Reply classes hide all the byte-level details of the packets.

The Server class waits for an incoming request and creates a new thread to handle the request. A buffer of size 10051 is used because that is the maximum allowed size of a request (unique id (16) + command (1) + key (32) + value-length (2) + value (10000) = 10051).

The RequestHandler is where the majority of the logic is. The methods in this class are self explanatory from their names. The RequestHandler.put method is static synchronized so only one thread can enter at a time (to prevent a race condition). A ConcurrentHashMap is used as the key-value store. Also, instead of a hard coded limit at 64MB, the program will determine the max heap size at runtime. This allows more configurability if we want to change the max heap size in the future.

The ByteKey class is just a wrapper around a byte array to allow a byte array to be used as a key for a set/map.

The use of DatagramSocket across multiple threads is safe and there is no chance of packets get interleaved.

At-most-once policy is implemented. Requests are cached by their request id. Currently, only up to 1000 request ids are cached at a time. Requests which return System Overload are NOT cached and can be safely repeated with the same request id.

# Custom Error Codes

0x21: Invalid Request Id Length. The request will silently be dropped by the service so the client won't ever receive it.  
0x22: Missing Command. Only the request id was received by the service.  
0x23: Invalid Key Length. The key length is less than 32 bytes.  
0x24: Missing Value Length. A PUT command was issued without a value length.  
0x25: Value Too Short. A PUT command issued with a value length which is greater than the length of the actual value.  
