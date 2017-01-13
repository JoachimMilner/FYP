package loadBalancer;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

import connectionUtils.MessageType;

/**
 * @author Joachim</br>
 *         <p>
 * 		Tests for the {@link ServerManager} class and its instance methods.
 *         </p>
 */
public class ServerManagerTests {

	/**
	 * Test successful creation of a new {@link ServerManager} object.
	 */
	@Test
	public void testServerManager_createServerManagerSuccessful() {
		ServerManager serverManager = new ServerManager(getServerList(1));
		assertNotNull(serverManager);
	}

	/**
	 * Test that the {@link ServerManager} constructor throws an
	 * IllegalArgumentException when passed a null set of servers.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testServerManager_createServerManagerNullServers() {
		new ServerManager(null);
	}

	/**
	 * Test that the {@link ServerManager} constructor throws an
	 * IllegalArgumentException when passed an empty set of servers.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testServerManager_createServerManagerZeroServers() {
		new ServerManager(getServerList(0));
	}

	/**
	 * Test that the {@link ServerManager} calls each {@link Server}'s
	 * <code>updateServerState</code> method when started in a new thread. This test
	 * mocks the ServerSocketChannels so that they can be messaged for the load
	 * request.
	 */
	@Test
	public void testServerManager_testUpdateServerCPULoads() {
		Set<Server> servers = getServerList(3);
		ServerManager serverManager = new ServerManager(servers);
		Map<Server, Double> serverLoadMap = mockServerSockets(servers);
		Thread serverManagerThread = new Thread(serverManager);
		serverManagerThread.start();
		try {
			Thread.sleep(750);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for (Server server : servers) {
			assertEquals(serverLoadMap.get(server).doubleValue(), server.getCPULoad(), 0);
		}
		serverManagerThread.interrupt();
	}

	/**
	 * Test the {@link ServerManager}'s <code>getAvailableServer</code> method.
	 * The method should return the server with the lowest CPU usage that is
	 * alive which will in turn be passed to a client. Here we use reflection to
	 * set the {@link Server} object's <code>cpuLoad</code> field so we can test
	 * the functionality of the method in isolation. In this case the method should
	 * return <code>server2</code> as it has the lowest CPU load.
	 */
	@Test
	public void testServerManager_testGetAvailableServerAllAlive() {
		Server server1 = new Server(new InetSocketAddress("localhost", 8000));
		Server server2 = new Server(new InetSocketAddress("localhost", 8001));
		Server server3 = new Server(new InetSocketAddress("localhost", 8002));
		try {
			Field cpuLoadField = Server.class.getDeclaredField("cpuLoad");
			cpuLoadField.setAccessible(true);
			cpuLoadField.set(server1, new Double(56.35));
			cpuLoadField.set(server2, new Double(15.035));
			cpuLoadField.set(server3, new Double(30.09));
			
			Field isAliveField = Server.class.getDeclaredField("isAlive");
			isAliveField.setAccessible(true);
			isAliveField.set(server1, true);
			isAliveField.set(server2, true);
			isAliveField.set(server3, true);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		ServerManager serverManager = new ServerManager(new HashSet<Server>(Arrays.asList(server1, server2, server3)));
		Server availableServer = serverManager.getAvailableServer();
		assertEquals(server2, availableServer);
	}
	
	/**
	 * Test the {@link ServerManager}'s <code>getAvailableServer</code> method.
	 * The method should return the server with the lowest CPU usage that is
	 * alive which will in turn be passed to a client. Here we use reflection to
	 * set the {@link Server} object's <code>cpuLoad</code> field so we can test
	 * the functionality of the method in isolation. In this case the method should
	 * return null as none of the servers are available. 
	 */
	@Test
	public void testServerManager_testGetAvailableServerNoneAlive() {
		Server server1 = new Server(new InetSocketAddress("localhost", 8000));
		Server server2 = new Server(new InetSocketAddress("localhost", 8001));
		Server server3 = new Server(new InetSocketAddress("localhost", 8002));
		try {
			Field cpuLoadField = Server.class.getDeclaredField("cpuLoad");
			cpuLoadField.setAccessible(true);
			cpuLoadField.set(server1, new Double(76.143));
			cpuLoadField.set(server3, new Double(38.702));
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		ServerManager serverManager = new ServerManager(new HashSet<Server>(Arrays.asList(server1, server2, server3)));
		Server availableServer = serverManager.getAvailableServer();
		assertNull(availableServer);
	}
	
	/**
	 * Test the {@link ServerManager}'s <code>getAvailableServer</code> method.
	 * The method should return the server with the lowest CPU usage that is
	 * alive which will in turn be passed to a client. Here we use reflection to
	 * set the {@link Server} object's <code>cpuLoad</code> field so we can test
	 * the functionality of the method in isolation. In this case the method should
	 * return null as none of the servers are available. 
	 */
	@Test
	public void testServerManager_testGetAvailableServerOnlyOneAlive() {
		Server server1 = new Server(new InetSocketAddress("localhost", 8000));
		Server server2 = new Server(new InetSocketAddress("localhost", 8001));
		Server server3 = new Server(new InetSocketAddress("localhost", 8002));
		try {
			Field cpuLoadField = Server.class.getDeclaredField("cpuLoad");
			cpuLoadField.setAccessible(true);
			cpuLoadField.set(server1, new Double(12.63));
			cpuLoadField.set(server2, new Double(50.43));
			cpuLoadField.set(server3, new Double(26.34));
			
			Field isAliveField = Server.class.getDeclaredField("isAlive");
			isAliveField.setAccessible(true);
			isAliveField.set(server2, true);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		ServerManager serverManager = new ServerManager(new HashSet<Server>(Arrays.asList(server1, server2, server3)));
		Server availableServer = serverManager.getAvailableServer();
		assertEquals(server2, availableServer);
	}
	

	/**
	 * Convenience method for creating a set of {@link Server} objects to be
	 * passed to the {@link ServerManager}'s constructor. As the network
	 * communication in this class' tests will be run on localhost, this method
	 * will create server objects starting with port 8000, and then incrementing
	 * the port for each Server object created.
	 * 
	 * @param numberOfServers
	 *            the size of the set of servers to be returned.
	 * @return a HashSet containing the number of servers specified.
	 */
	private Set<Server> getServerList(int numberOfServers) {
		Set<Server> servers = new HashSet<>();
		for (int i = 0; i < numberOfServers; i++) {
			servers.add(new Server(new InetSocketAddress("localhost", 8000 + i)));
		}
		return servers;
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
	private Map<Server, Double> mockServerSockets(Set<Server> servers) {
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
