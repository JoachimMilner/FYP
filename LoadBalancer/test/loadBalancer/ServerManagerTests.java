package loadBalancer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import commsModel.Server;
import testUtils.TestUtils;

/**
 * @author Joachim
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
		ServerManager serverManager = new ServerManager(TestUtils.getServerSet(1));
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
		new ServerManager(TestUtils.getServerSet(0));
	}

	/**
	 * Test that the {@link ServerManager} calls each {@link Server}'s
	 * <code>updateServerState</code> method when started in a new thread. This test
	 * mocks the ServerSocketChannels so that they can be messaged for the load
	 * request.
	 */
	@Test
	public void testServerManager_testUpdateServerCPULoads() {
		Set<Server> servers = TestUtils.getServerSet(3);
		ServerManager serverManager = new ServerManager(servers);
		Map<Server, Double> serverLoadMap = TestUtils.mockServerSockets(servers);
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
			
			Field isAliveField = Server.class.getSuperclass().getDeclaredField("isAlive");
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
			
			Field isAliveField = Server.class.getSuperclass().getDeclaredField("isAlive");
			isAliveField.setAccessible(true);
			isAliveField.set(server2, true);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		ServerManager serverManager = new ServerManager(new HashSet<Server>(Arrays.asList(server1, server2, server3)));
		Server availableServer = serverManager.getAvailableServer();
		assertEquals(server2, availableServer);
	}
}
