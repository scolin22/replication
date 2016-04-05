package test;

import com.s33263112.cpen431.Node;
import com.s33263112.cpen431.Router;
import org.junit.Test;

import java.math.BigInteger;
import java.util.List;

/**
 * Created by colin_000 on 4/2/2016.
 */
public class RouterTest {
    @Test
    public void getNodeWithHashedKey() throws Exception {
        List<Node> nodes = Router.getAllNodes();
        for (Node node : nodes) {
            System.out.println(node.getAddress().getHostAddress() + ":" + node.getPort());
        }

//        Integer nodeKey = Router.hash(InetAddress.getByName("134.197.113.3").getAddress(), 45111);

        BigInteger nodeKey = BigInteger.ONE;

        Node node = Router.findNodeForKey(nodeKey);

        System.out.println("Key: " + nodeKey + " belongs to node: " + node.getAddress().getHostAddress() + ":" + node.getPort());
    }

    @Test
    public void forward() throws Exception {

    }

    @Test
    public void getRing() throws Exception {
        List<Node> nodes = Router.getAllNodes();
        for (Node node : nodes) {
            System.out.println(node.getAddress().getHostAddress() + ":" + node.getPort() + " " + Router.hash(node.getAddress().getAddress(), node.getPort()));
        }
    }

    @Test
    public void getReplicateServerIDs() throws Exception {
        List<Node> nodes = Router.getAllNodes();
        for (Node node : nodes) {
            System.out.println(node.getAddress().getHostAddress() + ":" + node.getPort() + " ID:" + Router.hash(node));
            for (BigInteger replicateID : Router.getReplicateServerIDs(node)) {
                System.out.println("-- " + replicateID);
            }
        }
    }

    @Test
    public void getReplicateClientIDs() throws Exception {
        List<Node> nodes = Router.getAllNodes();
        for (Node node : nodes) {
            System.out.println(node.getAddress().getHostAddress() + ":" + node.getPort() + " ID:" + Router.hash(node));
            for (BigInteger replicateID : Router.getReplicateClientIDs(node)) {
                System.out.println("-- " + replicateID);
            }
        }
    }
}