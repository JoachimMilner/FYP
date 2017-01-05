package loadBalancer;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

import connectionUtils.MessageType;

/**
 * @author Joachim</br>
 * <p>Tests for the {@link ServerManager} class and its instance methods.</p>
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
	 * Test that the {@link ServerManager} constructor throws an IllegalArgumentException
	 * when passed a null set of servers.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testServerManager_createServerManagerNullServers() {
		new ServerManager(null);
	}
	
	/**
	 * Test that the {@link ServerManager} constructor throws an IllegalArgumentException
	 * when passed an empty set of servers.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testServerManager_createServerManagerZeroServers() {
		new ServerManager(getServerList(0));
	}
	
	/**
	 * Test that the {@link ServerManager} calls each {@link Server}'s <code>updateCPULoad</code>
	 * method when started in a new thread. This test mocks the ServerSocketChannels so that they can be 
	 * pinged for the load request.
	 */
	@Test
	public void testServerManager_testUpdateServerCPULoads() {
		Set<Server> servers = getServerList(3);
		ServerManager serverManager = new ServerManager(servers);
		Map<Server, Double> serverLoadMap = mockServerSockets(servers);
		Thread serverManagerThread = new Thread(serverManager);
		serverManagerThread.start();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for (Server server : servers) {
			assertEquals(serverLoadMap.get(server).doubleValue(), server.getCPULoad(), 0);
			System.out.println("test");
		}
	}
		

	/**
	 * Convenience method for creating a set of {@link Server} objects to be passed to the 
	 * {@link ServerManager}'s constructor. As the network communication in this class' tests
	 * will be run on localhost, this method will create server objects starting with port 8000,
	 * and then incrementing the port for each Server object created. 
	 * @param numberOfServers the size of the set of servers to be returned.
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
	 * Starts a thread for each {@link Server} in the set passed in that will mock the
	 * remote server's socket accept and return a random CPU load (double %) when requested.
	 * Maps the randomly generated load value onto each server in a map to return, so that
	 * the calling test can check the values are correct. 
	 * @param servers the servers to be mocked.
	 * @return a Map of each server linked to the double that it has sent as its CPU load.
	 */
	private Map<Server, Double> mockServerSockets(Set<Server> servers) {
		Map<Server, Double> serverLoadMap = new HashMap<>();
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
						
						// Return a random double representing the CPU load from the mocked server
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
