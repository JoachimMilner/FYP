package loadBalancer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.junit.Test;

import commsModel.RemoteLoadBalancer;
import commsModel.Server;
import testUtils.TestUtils;

/**
 * @author Joachim
 *         <p>
 *         Tests for the {@link ActiveLoadBalancer} class and its instance
 *         methods.
 *         </p>
 */
public class ActiveLoadBalancerTests {

	/**
	 * Test successful creation of a new {@link ActiveLoadBalancer} instance.
	 */
	@Test
	public void testCreateActiveLoadBalancer_successful() {
		AbstractLoadBalancer activeLoadBalancer = new ActiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1),
				TestUtils.getServerSet(1), new InetSocketAddress("localhost", 8000));
		assertNotNull(activeLoadBalancer);
	}

	/**
	 * Test creating a new {@link ActiveLoadBalancer} instance with null passed
	 * for the set of remote load balancers. Should throw
	 * {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateActiveLoadBalancer_nullRemoteLBSet() {
		new ActiveLoadBalancer(8000, null, TestUtils.getServerSet(1), new InetSocketAddress("localhost", 8000));
	}

	/**
	 * Test creating a new {@link ActiveLoadBalancer} instance with null passed
	 * for the set of remote servers. Should throw
	 * {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateActiveLoadBalancer_nullServerSet() {
		new ActiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1), null,
				new InetSocketAddress("localhost", 8000));
	}

	/**
	 * Test creating a new {@link ActiveLoadBalancer} instance with an empty set
	 * of remote load balancers. Should throw {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateActiveLoadBalancer_emptyRemoteLBSet() {
		new ActiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(0), TestUtils.getServerSet(1),
				new InetSocketAddress("localhost", 8000));
	}

	/**
	 * Test creating a new {@link ActiveLoadBalancer} instance with an empty set
	 * of remote servers. Should throw {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateActiveLoadBalancer_emptyServerSet() {
		new ActiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1), TestUtils.getServerSet(0),
				new InetSocketAddress("localhost", 8000));
	}

	/**
	 * Test creating a new {@link ActiveLoadBalancer} instance with the name
	 * service address passed as null. Should throw
	 * {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateActiveLoadBalancer_nullNameServiceAddress() {
		new ActiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1), TestUtils.getServerSet(1), null);
	}

	/**
	 * Tests that the <code>acceptPort</code> property of the
	 * {@link ActiveLoadBalancer} is set correctly in the class constructor. Use
	 * reflection to check the value after the constructor is called.
	 */
	@Test
	public void testActiveLoadBalancerConstructor_portIsSet()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		int expectedAcceptPort = 8000;
		AbstractLoadBalancer activeLoadBalancer = new ActiveLoadBalancer(expectedAcceptPort,
				TestUtils.getRemoteLoadBalancerSet(1), TestUtils.getServerSet(1),
				new InetSocketAddress("localhost", 8000));

		Field portField = activeLoadBalancer.getClass().getSuperclass().getDeclaredField("acceptPort");
		portField.setAccessible(true);
		assertEquals(expectedAcceptPort, portField.get(activeLoadBalancer));
	}

	/**
	 * Tests that the <code>remoteLoadBalancers</code> property of the
	 * {@link ActiveLoadBalancer} is set correctly in the class constructor. Use
	 * reflection to check the value after the constructor is called.
	 */
	@Test
	public void testActiveLoadBalancerConstructor_remoteLBsAreSet()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Set<RemoteLoadBalancer> expectedRemoteLoadBalancers = TestUtils.getRemoteLoadBalancerSet(1);
		AbstractLoadBalancer activeLoadBalancer = new ActiveLoadBalancer(8000, expectedRemoteLoadBalancers,
				TestUtils.getServerSet(1), new InetSocketAddress("localhost", 8000));

		Field remoteLBField = activeLoadBalancer.getClass().getSuperclass().getDeclaredField("remoteLoadBalancers");
		remoteLBField.setAccessible(true);
		assertEquals(expectedRemoteLoadBalancers, remoteLBField.get(activeLoadBalancer));
	}

	/**
	 * Tests that the <code>servers</code> property of the
	 * {@link ActiveLoadBalancer} is set correctly in the class constructor. Use
	 * reflection to check the value after the constructor is called.
	 */
	@Test
	public void testActiveLoadBalancerConstructor_serversAreSet()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Set<Server> expectedServers = TestUtils.getServerSet(1);
		AbstractLoadBalancer activeLoadBalancer = new ActiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1),
				expectedServers, new InetSocketAddress("localhost", 8000));

		Field serversField = activeLoadBalancer.getClass().getSuperclass().getDeclaredField("servers");
		serversField.setAccessible(true);
		assertEquals(expectedServers, serversField.get(activeLoadBalancer));
	}

	/**
	 * Tests that the <code>nameServiceAddress</code> property of the
	 * {@link ActiveLoadBalancer} is set correctly in the class constructor. Use
	 * reflection to check the value after the constructor is called.
	 */
	@Test
	public void testActiveLoadBalancerConstructor_nameServiceAddressIsSet()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		InetSocketAddress expectedNameServiceAddress = new InetSocketAddress("localhost", 8000);
		AbstractLoadBalancer activeLoadBalancer = new ActiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1),
				TestUtils.getServerSet(1), expectedNameServiceAddress);

		Field nameServiceAddressField = activeLoadBalancer.getClass().getSuperclass().getDeclaredField("nameServiceAddress");
		nameServiceAddressField.setAccessible(true);
		assertEquals(expectedNameServiceAddress, nameServiceAddressField.get(activeLoadBalancer));
	}

	/**
	 * Tests that the {@link ActiveLoadBalancer}'s <code>run</code> method creates a <code>ServerSocketChannel</code> 
	 * by creating a mock client <code>SocketChannel</code> and attempting to connect. 
	 * @throws IOException 
	 */
	@Test
	public void testActiveLoadBalancer_socketCreation() throws IOException {
		AbstractLoadBalancer activeLoadBalancer = new ActiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1),
				TestUtils.getServerSet(1), new InetSocketAddress("localhost", 8001));
		Thread activeLoadBalancerThread = new Thread(activeLoadBalancer);
		activeLoadBalancerThread.start();
		SocketChannel mockClient = SocketChannel.open();
		mockClient.connect(new InetSocketAddress("localhost", 8000));
		assertTrue(mockClient.isConnected());
		activeLoadBalancerThread.interrupt();
		mockClient.close();
	}
	
	/**
	 * Tests execution of the thread pool. Connecting a mock client should increase the active thread count
	 * by one, as well as returning a correct response to a request.
	 * @throws IOException
	 */
	@Test
	public void testActiveLoadBalancer_createNewThreads() throws IOException {
		Set<Thread> threadSetDefault = Thread.getAllStackTraces().keySet();
		Set<Server> servers = TestUtils.getServerSet(1);
		AbstractLoadBalancer activeLoadBalancer = new ActiveLoadBalancer(8001, TestUtils.getRemoteLoadBalancerSet(1),
				servers, new InetSocketAddress("localhost", 8002));
		TestUtils.mockServerSockets(servers);
		Thread activeLoadBalancerThread = new Thread(activeLoadBalancer);
		activeLoadBalancerThread.start();
		
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Set<Thread> threadSetLBInit = Thread.getAllStackTraces().keySet();
		assertEquals(threadSetDefault.size() + 2, threadSetLBInit.size());
		
		SocketChannel mockClient = SocketChannel.open();
		mockClient.connect(new InetSocketAddress("localhost", 8001));
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Set<Thread> threadSetPoolIncremented = Thread.getAllStackTraces().keySet();
		assertEquals(threadSetLBInit.size() + 1, threadSetPoolIncremented.size());
		activeLoadBalancerThread.interrupt();
		mockClient.close();
	}
}
