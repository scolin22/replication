package test;

import com.s33263112.cpen431.Backup;
import com.s33263112.cpen431.ByteKey;
import com.s33263112.cpen431.Request;
import com.s33263112.cpen431.Router;
import org.junit.Test;

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * Created by colin_000 on 4/3/2016.
 */
public class RequestHandlerTest {
    private byte[] randomByteArray(int n) {
        byte[] b = new byte[n];
        new Random().nextBytes(b);
        return b;
    }

    @Test
    public void handleReplicatePut() throws Exception {
        byte[] buffer = new byte[Request.MAX_REQUEST_LENGTH];
        DatagramPacket p = new DatagramPacket(buffer, buffer.length);
        Request request = new Request(p);

        request.setReplyAddress(Router.getMyIp());
        request.setReplyPort(Router.getMyPort());
        byte[] key = randomByteArray(32);
        byte[] value = randomByteArray(50);
        request.setKey(key);
        request.setValue(value);

        ByteKey hashkey = new ByteKey(request.getKey());
        BigInteger backupID = Router.hash(request.getReplyAddress().getAddress(), request.getReplyPort());
        Backup.put(backupID, hashkey, request.getValue());
        Map<ByteKey, byte[]> m = new ConcurrentHashMap<>();
        Backup.merge(backupID, m);
//        Backup.delete(backupID);
    }

}