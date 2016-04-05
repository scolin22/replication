package test;

import com.s33263112.cpen431.Node;
import com.s33263112.cpen431.Router;
import org.junit.Test;

import java.math.BigInteger;
import java.net.InetAddress;
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
    public void getReplicates() throws Exception {
        List<Node> nodes = Router.getAllNodes();
        for (Node node : nodes) {
            System.out.println(node.getAddress().getHostAddress() + ":" + node.getPort());
        }

        InetAddress ip = InetAddress.getByName("192.168.1.2");
        Integer port = 45111;

        System.out.println("REPLICATES OF " + ip.getHostAddress() + ":" + port);
        List<Node> replicates = Router.getReplicateServers(Router.getNodeFromHashID(Router.hash(ip.getAddress(), port)));
        for (Node replicate : replicates) {
            System.out.println(replicate.getAddress().getHostAddress() + ":" + replicate.getPort());
        }
    }

    @Test
    public void getReplicateIDs() throws Exception {
        List<Node> nodes = Router.getAllNodes();
        for (Node node : nodes) {
            System.out.println(node.getAddress().getHostAddress() + ":" + node.getPort() + " ID:" + Router.hash(node));
            for (BigInteger replicateID : Router.getReplicateServerIDs(node)) {
                System.out.println("-- " + replicateID);
            }
        }
    }
}