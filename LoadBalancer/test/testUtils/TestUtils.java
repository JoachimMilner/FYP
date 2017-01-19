package testUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import commsModel.RemoteLoadBalancer;
import commsModel.Server;
import connectionUtils.MessageType;

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
	
	/**
	 * Starts a thread for each {@link Server} in the set passed in that will
	 * mock the remote server's socket accept and return a random CPU load
	 * (double %) when requested. Maps the randomly generated load value onto
	 * each server in a map to return, so that the calling test can check the
	 * values are correct.
	 * 
	 * @param servers
	 *            the servers to be mocked.
	 * @return a Map of each server linked to the double that it has sent as its
	 *         CPU load.
	 */
	public static Map<Server, Double> mockServerSockets(Set<Server> servers) {
		Map<Server, Double> serverLoadMap = new ConcurrentHashMap<>();
		for (Server server : servers) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					double cpuUsage = ThreadLocalRandom.current().nextDouble(0.1, 99.9);
					serverLoadMap.put(server, cpuUsage);
					ServerSocketChannel mockServerSocketChannel = null;
					try {
						mockServerSocketChannel = ServerSocketChannel.open();
						mockServerSocketChannel.socket().bind(new InetSocketAddress(server.getAddress().getPort()));
						mockServerSocketChannel.configureBlocking(false);
						Selector acceptSelector = Selector.open();
						mockServerSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
						SocketChannel acceptedSocketChannel = null;
						if (acceptSelector.select(1000) == 0) {
							throw new SocketTimeoutException();
						}
						acceptedSocketChannel = mockServerSocketChannel.accept();
						ByteBuffer buffer = ByteBuffer.allocate(9);
						Selector readSelector = Selector.open();
						acceptedSocketChannel.configureBlocking(false);
						acceptedSocketChannel.register(readSelector, SelectionKey.OP_READ);
						if (readSelector.select(1000) == 0) {
							throw new SocketTimeoutException();
						}
						acceptedSocketChannel.read(buffer);
						buffer.flip();
						MessageType responseMessageType = MessageType.values()[buffer.get()];
						if (!responseMessageType.equals(MessageType.SERVER_CPU_REQUEST)) {
							throw new UnsupportedOperationException();
						}

						// Return a random double representing the CPU load from
						// the mocked server
						buffer.clear();
						buffer.put((byte) MessageType.SERVER_CPU_NOTIFY.getValue());

						buffer.putDouble(cpuUsage);
						buffer.flip();
						while (buffer.hasRemaining()) {
							acceptedSocketChannel.write(buffer);
						}
						acceptSelector.close();
						readSelector.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						mockServerSocketChannel.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
		return serverLoadMap;
	}
}
