package test;

import com.s33263112.cpen431.ErrorCode;
import com.s33263112.cpen431.Node;
import com.s33263112.cpen431.Router;
import org.junit.Test;

public class AliveTests {

    @Test
    public void testAll() {
        for (Node node : Router.getAllNodes()) {
            System.out.println(node.getAddress().toString());
            TestClient client = new TestClient(node.getAddress(), node.getPort());
            try {
                ClientReply reply = client.deleteAll();
                if (reply.getErrorCode() == ErrorCode.SUCCESS) {
                    System.out.println(node.getAddress().toString() + " UP");
                } else {
                    System.out.println(node.getAddress().toString() + " DOWN");
                }
            } catch (RuntimeException e) {
                System.out.println(node.getAddress().toString() + " DOWN");
            }
            client.close();
        }
    }
}
