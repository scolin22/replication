package test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.s33263112.cpen431.ByteKey;
import com.s33263112.cpen431.ErrorCode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CorrectnessTests {
    
    private static TestClient client;
    private static String serverIp = "127.0.0.1";
    private static int port = 45112;
    
    private byte[] randomByteArray(int n) {
        byte[] b = new byte[n];
        new Random().nextBytes(b);
        return b;
    }
    
    @BeforeClass
    public static void setUpBeforeClass() {
        client = new TestClient(serverIp, port);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        client.close();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    //@Test
    public void testShutdown() {
        //ClientReply reply = client.shutdown();
        //System.out.println(reply);
    }
    
    @Test
    public void testPutGet() {
        byte[] key = randomByteArray(32);
        byte[] value = randomByteArray(50);
        ClientReply reply = client.put(key, value);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
        
        reply = client.get(key);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
        assertTrue(Arrays.equals(value, reply.getValue()));
    }

    @Test
    public void sanityTest() {
        // PUT -> GET -> REMOVE -> GET
        byte[] key = randomByteArray(32);
        byte[] value = randomByteArray(50);
        ClientReply reply = client.put(key, value);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
        
        reply = client.get(key);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
        assertTrue(Arrays.equals(value, reply.getValue()));
        
        reply = client.remove(key);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());

        reply = client.get(key);
        assertEquals(ErrorCode.NON_EXISTANT_KEY, reply.getErrorCode());
        
        // PUT -> PUT -> GET
        key = randomByteArray(32);
        value = randomByteArray(50);
        reply = client.put(key, value);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());

        value = randomByteArray(60);
        reply = client.put(key, value);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());

        reply = client.get(key);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
        assertTrue(Arrays.equals(value, reply.getValue()));
        
        // PUT -> REMOVE -> REMOVE
        key = randomByteArray(32);
        value = randomByteArray(50);
        reply = client.put(key, value);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());

        reply = client.remove(key);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());

        reply = client.remove(key);
        assertEquals(ErrorCode.NON_EXISTANT_KEY, reply.getErrorCode());
        
        // Invalid Command
        reply = client.invalidCommand();
        assertEquals(ErrorCode.UNRECOGNIZED_COMMAND, reply.getErrorCode());
    }
    
    @Test
    public void put_NegativeValueLength_InvalidValueLength() {
        byte[] key = randomByteArray(32);
        byte[] value = randomByteArray(100);
        ClientReply reply = client.invalidPut(key, value, -100);
        assertEquals(ErrorCode.INVALID_VALUE_LENGTH, reply.getErrorCode());
    }
    
    @Test
    public void put_ShortValue_ValueTooShort() {
        byte[] key = randomByteArray(32);
        byte[] value = randomByteArray(100);
        ClientReply reply = client.invalidPut(key, value, 200);
        assertEquals(ErrorCode.VALUE_TOO_SHORT, reply.getErrorCode());
    }
    
    @Test
    public void put_LongValue_Success() {
        byte[] key = randomByteArray(32);
        byte[] value = randomByteArray(100);
        ClientReply reply = client.invalidPut(key, value, 50);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
        
        reply = client.get(key);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
        assertTrue(Arrays.equals(Arrays.copyOf(value, 50), reply.getValue()));
    }
    
    @Test
    public void put_NoValue_ValueTooShort() {
        byte[] key = randomByteArray(32);
        byte[] value = randomByteArray(0);
        ClientReply reply = client.invalidPut(key, value, 10);
        assertEquals(ErrorCode.VALUE_TOO_SHORT, reply.getErrorCode());
    }
    
    @Test
    public void put_NoValueLength_MissingValueLength() {
        byte[] key = randomByteArray(32);
        byte[] value = randomByteArray(0);
        ClientReply reply = client.put(key, value);
        assertEquals(ErrorCode.MISSING_VALUE_LENGTH, reply.getErrorCode());
    }
    
    @Test
    public void put_ShortKey_InvalidKeyLength() {
        byte[] key = randomByteArray(30);
        ClientReply reply = client.invalidPut(key);
        assertEquals(ErrorCode.INVALID_KEY_LENGTH, reply.getErrorCode());
    }
    
    @Test
    public void put_NoKey_InvalidKeyLength() {
        byte[] key = randomByteArray(0);
        ClientReply reply = client.invalidPut(key);
        assertEquals(ErrorCode.INVALID_KEY_LENGTH, reply.getErrorCode());
    }
    
    @Test
    public void client_NoCommand_MissingCommand() {
        ClientReply reply = client.noCommand();
        assertEquals(ErrorCode.MISSING_COMMAND, reply.getErrorCode());
    }
    
    @Test
    public void client_NoRequestId_InvalidRequestIdLength() {
        ClientReply reply = client.invalidRequestId();
        assertNull(reply);
    }
    
    @Test
    public void testCache() {
        byte[] requestId1 = randomByteArray(16);
        byte[] requestId2 = randomByteArray(16);
        byte[] key = randomByteArray(32);
        byte[] value1 = randomByteArray(10);
        byte[] value2 = randomByteArray(10);

        ClientReply reply = client.put(requestId1, key, value1);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
        
        reply = client.put(requestId2, key, value2);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
        
        reply = client.put(requestId1, key, value1);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
        
        reply = client.get(key);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
        assertTrue(Arrays.equals(value2, reply.getValue()));
    }
    
    @Test
    public void testDeleteAll() {
        byte[] key1 = randomByteArray(32);
        byte[] value1 = randomByteArray(new Random().nextInt(10000));
        byte[] key2 = randomByteArray(32);
        byte[] value2 = randomByteArray(new Random().nextInt(10000));

        ClientReply reply = client.put(key1, value1);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
        
        reply = client.put(key2, value2);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
        
        reply = client.get(key1);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());

        reply = client.get(key2);
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());

        //TODO: test will not pass on multi-servers unless you delete the node that actually has the keys
        reply = client.deleteAll();
        assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
        
        reply = client.get(key1);
        assertEquals(ErrorCode.NON_EXISTANT_KEY, reply.getErrorCode());

        reply = client.get(key2);
        assertEquals(ErrorCode.NON_EXISTANT_KEY, reply.getErrorCode());
    }

    @Test
    public void testCheckNodePrime() {
        int num_keys = 10000;
        int num_sent = 0;
        int num_persists = 0;
        HashMap<ByteKey, ByteKey> hm = new HashMap<>();
        for (int i = 0; i < num_keys; i++) {
            byte[] key = randomByteArray(32);
            byte[] value = randomByteArray(new Random().nextInt(10000));
            hm.put(new ByteKey(key), new ByteKey(value));
            ClientReply reply = client.put(key, value);
            if (reply != null && reply.getErrorCode() == ErrorCode.SUCCESS) {
                num_sent++;
            }
//            assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //client.shutdown();
        client.deleteAll();
//        client = new TestClient(serverIp, 45114);
//        client.shutdown();

//        try {
//            Thread.sleep(75000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        client = new TestClient(serverIp, 45113);
        client.deleteAll();
        for (ByteKey key : hm.keySet()) {
            ClientReply reply = client.get(key.getKey());
            if (reply != null && reply.getErrorCode() == ErrorCode.NON_EXISTANT_KEY) {
                num_persists++;
            }
//            assertEquals(ErrorCode.SUCCESS, reply.getErrorCode());
//            assertTrue(Arrays.equals(hm.get(key).getKey(), reply.getValue()));
        }
        System.out.println("Success rate: " + (float)num_persists/num_sent);
    }
}
