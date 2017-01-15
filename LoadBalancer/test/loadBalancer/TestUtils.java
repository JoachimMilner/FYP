package loadBalancer;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import commsModel.RemoteLoadBalancer;
import commsModel.Server;

/**
 * @author Joachim
 *         <p>
 *         Utility class containing shared methods for load balancer test
 *         classes.
 *         </p>
 *
 */
public class TestUtils {

	/**
	 * Convenience method for creating a set of {@link Server} objects. As the
	 * network communication in this project's tests will be run on localhost,
	 * this method will create server objects starting with port 8000, and then
	 * incrementing the port for each Server object created.
	 * 
	 * @param numberOfServers
	 *            the size of the set of servers to be returned.
	 * @return a HashSet containing the number of servers specified.
	 */
	public static Set<Server> getServerSet(int numberOfServers) {
		Set<Server> servers = new HashSet<>();
		for (int i = 0; i < numberOfServers; i++) {
			servers.add(new Server(new InetSocketAddress("localhost", 8000 + i)));
		}
		return servers;
	}

	/**
	 * Convenience method for creating a set of {@link RemoteLoadBalancer}
	 * objects. As the network communication in this project's tests will be run
	 * on localhost, this method will create remote objects starting with port
	 * 8000, and then incrementing the port for each RemoteLoadBalancer object
	 * created.
	 * 
	 * @param numberOfRemotes
	 *            the size of the set of remotes to be returned.
	 * @returna a HashSet containing the number of RemoteLoadBalancers
	 *          specified.
	 */
	public static Set<RemoteLoadBalancer> getRemoteLoadBalancerSet(int numberOfRemotes) {
		Set<RemoteLoadBalancer> remoteLoadBalancers = new HashSet<>();
		for (int i = 0; i < numberOfRemotes; i++) {
			remoteLoadBalancers.add(new RemoteLoadBalancer(new InetSocketAddress("localhost", 8000 + i)));
		}
		return remoteLoadBalancers;
	}
}
