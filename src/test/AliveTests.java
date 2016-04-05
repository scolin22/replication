package test;

import com.s33263112.cpen431.ErrorCode;
import com.s33263112.cpen431.Node;
import com.s33263112.cpen431.Router;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Test;

public class AliveTests {

    //@Test
    public void testAll() {
        System.out.println("testAll");
        for (Node node : Router.getAllNodes()) {
            System.out.println(node.getAddress().toString());
            TestClient client = new TestClient(node.getAddress(), node.getPort());
            try {
                ClientReply reply = client.deleteAll();
                if (reply.getErrorCode() == ErrorCode.SUCCESS) {
                    System.out.println(node.getAddress().toString() + " UP");
                } else {
                    System.out.println(node.getAddress().toString() + " NOT SUCCESS");
                }
            } catch (RuntimeException e) {
                System.out.println(node.getAddress().toString() + " DOWN");
            }
            client.close();
        }
    }
    
    @Test
    public void testSize() {
        System.out.println("testSize");
        for (Node node : Router.getAllNodes()) {
            TestClient client = new TestClient(node.getAddress(), node.getPort());
            try {
                ClientReply reply = client.getStoreSize();
                if (reply.getErrorCode() == ErrorCode.SUCCESS) {
                    System.out.println(node.getAddress().toString() + "\t" + ByteBuffer.wrap(reply.getValue()).order(ByteOrder.LITTLE_ENDIAN).getInt());
                } else {
                    System.out.println(node.getAddress().toString() + " NOT SUCCESS");
                }
            } catch (RuntimeException e) {
                System.out.println(node.getAddress().toString() + " DOWN");
            }
            client.close();
        }
    }
    
    @Test
    public void testBackupSize() {
        System.out.println("testBackupSize");
        for (Node node : Router.getAllNodes()) {
            TestClient client = new TestClient(node.getAddress(), node.getPort());
            try {
                ClientReply reply = client.getBackupSize();
                if (reply.getErrorCode() == ErrorCode.SUCCESS) {
                    System.out.println(node.getAddress().toString() + "\t" + ByteBuffer.wrap(reply.getValue()).order(ByteOrder.LITTLE_ENDIAN).getInt());
                } else {
                    System.out.println(node.getAddress().toString() + " NOT SUCCESS");
                }
            } catch (RuntimeException e) {
                System.out.println(node.getAddress().toString() + " DOWN");
            }
            client.close();
        }
    }
    
    @Test
    public void testFreeMemory() {
        System.out.println("testFreeMemory");
        for (Node node : Router.getAllNodes()) {
            TestClient client = new TestClient(node.getAddress(), node.getPort());
            try {
                ClientReply reply = client.getFreeMemory();
                if (reply.getErrorCode() == ErrorCode.SUCCESS) {
                    System.out.println(node.getAddress().toString() + "\t" + ByteBuffer.wrap(reply.getValue()).order(ByteOrder.LITTLE_ENDIAN).getLong());
                } else {
                    System.out.println(node.getAddress().toString() + " NOT SUCCESS");
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                System.out.println(node.getAddress().toString() + " DOWN");
            }
            client.close();
        }
    }
}
