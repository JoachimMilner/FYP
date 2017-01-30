package faultModule;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.junit.Test;

import commsModel.LoadBalancerState;
import commsModel.RemoteLoadBalancer;
import commsModel.Server;
import connectionUtils.MessageType;
import loadBalancer.AbstractLoadBalancer;
import testUtils.TestUtils;

/**
 * @author Joachim
 *         <p>
 *         Test class for the {@link PassiveLoadBalancer} class and its methods.
 *         </p>
 *
 */
public class PassiveLoadBalancerTests {

	/**
	 * Test successful instantiation of the {@link PassiveLoadBalancer} class.
	 */
	@Test
	public void testCreatePassiveLoadBalancer_successful() {
		AbstractLoadBalancer loadBalancer = new PassiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1),
				TestUtils.getServerSet(1), new InetSocketAddress("localhost", 8080), 5);
		assertNotNull(loadBalancer);
	}

	/**
	 * Test creating a new {@link PassiveLoadBalancer} instance with null passed
	 * for the set of remote load balancers. Should throw
	 * {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreatePassiveLoadBalancer_nullRemoteLBSet() {
		new PassiveLoadBalancer(8000, null, TestUtils.getServerSet(1), new InetSocketAddress("localhost", 8000), 5);
	}

	/**
	 * Test creating a new {@link PassiveLoadBalancer} instance with null passed
	 * for the set of remote servers. Should throw
	 * {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreatePassiveLoadBalancer_nullServerSet() {
		new PassiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1), null,
				new InetSocketAddress("localhost", 8000), 5);
	}

	/**
	 * Test creating a new {@link PassiveLoadBalancer} instance with an empty
	 * set of remote load balancers. Should throw
	 * {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreatePassiveLoadBalancer_emptyRemoteLBSet() {
		new PassiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(0), TestUtils.getServerSet(1),
				new InetSocketAddress("localhost", 8000), 5);
	}

	/**
	 * Test creating a new {@link PassiveLoadBalancer} instance with an empty
	 * set of remote servers. Should throw {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreatePassiveLoadBalancer_emptyServerSet() {
		new PassiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1), TestUtils.getServerSet(0),
				new InetSocketAddress("localhost", 8000), 5);
	}

	/**
	 * Test creating a new {@link PassiveLoadBalancer} instance with the name
	 * service address passed as null. Should throw
	 * {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreatePassiveLoadBalancer_nullNameServiceAddress() {
		new PassiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1), TestUtils.getServerSet(1), null, 5);
	}

	/**
	 * Test creating a new {@link PassiveLoadBalancer} instance with the timeout
	 * value set to zero. Should throw {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreatePassiveLoadBalancer_TimeoutSecsLessThanOne() {
		new PassiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1), TestUtils.getServerSet(1),
				new InetSocketAddress("localhost", 8080), 0);
	}

	/**
	 * Tests that the <code>acceptPort</code> property of the
	 * {@link PassiveLoadBalancer} is set correctly in the class constructor.
	 * Use reflection to check the value after the constructor is called.
	 */
	@Test
	public void testPassiveLoadBalancerConstructor_portIsSet()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		int expectedAcceptPort = 8000;
		AbstractLoadBalancer passiveLoadBalancer = new PassiveLoadBalancer(expectedAcceptPort,
				TestUtils.getRemoteLoadBalancerSet(1), TestUtils.getServerSet(1),
				new InetSocketAddress("localhost", 8000), 5);

		Field portField = passiveLoadBalancer.getClass().getSuperclass().getDeclaredField("acceptPort");
		portField.setAccessible(true);
		assertEquals(expectedAcceptPort, portField.get(passiveLoadBalancer));
	}

	/**
	 * Tests that the <code>remoteLoadBalancers</code> property of the
	 * {@link PassiveLoadBalancer} is set correctly in the class constructor.
	 * Use reflection to check the value after the constructor is called.
	 */
	@Test
	public void testPassiveLoadBalancerConstructor_remoteLBsAreSet()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Set<RemoteLoadBalancer> expectedRemoteLoadBalancers = TestUtils.getRemoteLoadBalancerSet(1);
		AbstractLoadBalancer passiveLoadBalancer = new PassiveLoadBalancer(8000, expectedRemoteLoadBalancers,
				TestUtils.getServerSet(1), new InetSocketAddress("localhost", 8000), 5);

		Field remoteLBField = passiveLoadBalancer.getClass().getSuperclass().getDeclaredField("remoteLoadBalancers");
		remoteLBField.setAccessible(true);
		assertEquals(expectedRemoteLoadBalancers, remoteLBField.get(passiveLoadBalancer));
	}

	/**
	 * Tests that the <code>servers</code> property of the
	 * {@link PassiveLoadBalancer} is set correctly in the class constructor.
	 * Use reflection to check the value after the constructor is called.
	 */
	@Test
	public void testPassiveLoadBalancerConstructor_serversAreSet()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Set<Server> expectedServers = TestUtils.getServerSet(1);
		AbstractLoadBalancer passiveLoadBalancer = new PassiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1),
				expectedServers, new InetSocketAddress("localhost", 8000), 5);

		Field serversField = passiveLoadBalancer.getClass().getSuperclass().getDeclaredField("servers");
		serversField.setAccessible(true);
		assertEquals(expectedServers, serversField.get(passiveLoadBalancer));
	}

	/**
	 * Tests that the <code>nameServiceAddress</code> property of the
	 * {@link PassiveLoadBalancer} is set correctly in the class constructor.
	 * Use reflection to check the value after the constructor is called.
	 */
	@Test
	public void testPassiveLoadBalancerConstructor_nameServiceAddressIsSet()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		InetSocketAddress expectedNameServiceAddress = new InetSocketAddress("localhost", 8000);
		AbstractLoadBalancer passiveLoadBalancer = new PassiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1),
				TestUtils.getServerSet(1), expectedNameServiceAddress, 5);

		Field nameServiceAddressField = passiveLoadBalancer.getClass().getSuperclass()
				.getDeclaredField("nameServiceAddress");
		nameServiceAddressField.setAccessible(true);
		assertEquals(expectedNameServiceAddress, nameServiceAddressField.get(passiveLoadBalancer));
	}

	/**
	 * Tests that the <code>defaultTimeoutSecs</code> property of the
	 * {@link PassiveLoadBalancer} is set correctly in the class constructor.
	 * Use reflection to check the value after the constructor is called.
	 */
	@Test
	public void testPassiveLoadBalancerConstructor_defaultTimeoutIsSet()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		int expectedTimeoutValue = 6;
		AbstractLoadBalancer passiveLoadBalancer = new PassiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1),
				TestUtils.getServerSet(1), new InetSocketAddress("localhost", 8000), expectedTimeoutValue);

		Field timeoutAddressField = passiveLoadBalancer.getClass().getDeclaredField("defaultTimeoutSecs");
		timeoutAddressField.setAccessible(true);
		assertEquals(expectedTimeoutValue, timeoutAddressField.get(passiveLoadBalancer));
	}

	/**
	 * Test the {@link PassiveLoadBalancer} monitoring a heartbeat under normal
	 * operation. That is, the active load balancer does not fail and sends
	 * heartbeat messages at regular intervals. Currently no assertions in this
	 * test as we simply want to ensure that the passive load balancer does not
	 * attempt to take any action while the active is healthy.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPassiveLoadBalancer_monitorHeartbeat() throws IOException {
		Set<RemoteLoadBalancer> remoteLoadBalancers = TestUtils.getRemoteLoadBalancerSet(1);
		remoteLoadBalancers.iterator().next().setState(LoadBalancerState.ACTIVE);
		AbstractLoadBalancer passiveLoadBalancer = new PassiveLoadBalancer(8001, remoteLoadBalancers,
				TestUtils.getServerSet(1), new InetSocketAddress("localhost", 8080), 1);

		Thread passiveLBThread = new Thread(passiveLoadBalancer);
		passiveLBThread.start();

		SocketChannel activeLBSocketChannel = SocketChannel.open();
		activeLBSocketChannel.connect(new InetSocketAddress("localhost", 8001));
		ByteBuffer buffer = ByteBuffer.allocate(1);
		for (int i = 0; i < 4; i++) {
			buffer.put((byte) MessageType.ALIVE_CONFIRM.getValue());
			buffer.flip();
			while (buffer.hasRemaining()) {
				activeLBSocketChannel.write(buffer);
			}

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			buffer.clear();
		}
		activeLBSocketChannel.close();
		passiveLBThread.interrupt();
	}
}
