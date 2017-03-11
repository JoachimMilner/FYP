package faultModule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import commsModel.LoadBalancerState;
import commsModel.RemoteLoadBalancer;
import connectionUtils.MessageType;
import testUtils.TestUtils;

/**
 * @author Joachim
 *         <p>
 *         Tests for the {@link HeartbeatBroadcaster} class and its methods.
 *         </p>
 *
 */
public class HeartbeatBroadcasterTests {

	/**
	 * Tests successful creation of a new {@link HeartbeatBroadcaster}.
	 */
	@Test
	public void testCreateHeartbeatBroadcaster_successful() {
		HeartbeatBroadcaster heartbeatBroadcaster = new HeartbeatBroadcaster(TestUtils.getRemoteLoadBalancerSet(1), 3, LoadBalancerState.ACTIVE);
		assertNotNull(heartbeatBroadcaster);
	}

	/**
	 * Tests creating a new {@link HeartbeatBroadcaster} instance with a null
	 * <code>remoteLoadBalancers</code> set passed in. Should throw
	 * IllegalArgumentException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateHeartbeatBroadcaster_nullNodeSet() {
		new HeartbeatBroadcaster(null, 3, LoadBalancerState.ACTIVE);
	}

	/**
	 * Tests creating a new {@link HeartbeatBroadcaster} instance with an empty
	 * <code>remoteLoadBalancers</code> set passed in. Should throw
	 * IllegalArgumentException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateHeartbeatBroadcaster_emptyNodeSet() {
		new HeartbeatBroadcaster(new HashSet<RemoteLoadBalancer>(), 3, LoadBalancerState.ACTIVE);
	}
	
	/**
	 * Tests creating a new {@link HeartbeatBroadcaster} instance with 
	 * <code>heartbeatIntervalSecs</code> set to less than 1. Should throw
	 * IllegalArgumentException
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateHeartbeatBroadcaster_invalidHBInterval() {
		new HeartbeatBroadcaster(TestUtils.getRemoteLoadBalancerSet(1), 0, LoadBalancerState.ACTIVE);
	}
	
	/**
	 * Tests that the <code>remoteLoadBalancers</code> property of the
	 * {@link HeartbeatBroadcaster} is set correctly in the class constructor. Use
	 * reflection to check the value after the constructor is called.
	 */
	@Test
	public void testHeartbeatBroadcasterConstructor_remoteLBsAreSet()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Set<RemoteLoadBalancer> expectedLBSet = TestUtils.getRemoteLoadBalancerSet(1);
		HeartbeatBroadcaster heartbeatBroadcaster = new HeartbeatBroadcaster(expectedLBSet, 3, LoadBalancerState.ACTIVE);

		Field remoteLBField = heartbeatBroadcaster.getClass().getDeclaredField("remoteLoadBalancers");
		remoteLBField.setAccessible(true);
		assertEquals(expectedLBSet, remoteLBField.get(heartbeatBroadcaster));
	}
	
	/**
	 * Tests that the <code>heartbeatIntervalSecs</code> property of the
	 * {@link HeartbeatBroadcaster} is set correctly in the class constructor. Use
	 * reflection to check the value after the constructor is called.
	 */
	@Test
	public void testHeartbeatBroadcasterConstructor_heartbeatIntervalIsSet()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		int expectedHeartbeatInterval = 5;
		HeartbeatBroadcaster heartbeatBroadcaster = new HeartbeatBroadcaster(TestUtils.getRemoteLoadBalancerSet(1), expectedHeartbeatInterval, LoadBalancerState.ACTIVE);

		Field heartbeatIntervalField = heartbeatBroadcaster.getClass().getDeclaredField("heartbeatIntervalMillis");
		heartbeatIntervalField.setAccessible(true);
		assertEquals(expectedHeartbeatInterval, heartbeatIntervalField.get(heartbeatBroadcaster));
	}
	
	/**
	 * Test that the {@link HeartbeatBroadcaster} periodically transmits an <code>ALIVE_CONFIRM</code>
	 * message to a single remote passive load balancer. 
	 * @throws IOException 
	 */
	@Test
	public void testHeartbeatBroadcaster_broadcastToOneRemote() throws IOException {
		ServerSocketChannel mockRemoteSocketChannel = ServerSocketChannel.open();
		mockRemoteSocketChannel.socket().bind(new InetSocketAddress(8000));
		mockRemoteSocketChannel.configureBlocking(false);
		
		HeartbeatBroadcaster heartbeatBroadcaster = new HeartbeatBroadcaster(TestUtils.getRemoteLoadBalancerSet(1), 1, LoadBalancerState.ACTIVE);
		Thread hbThread = new Thread(heartbeatBroadcaster);
		hbThread.start();
		
		Selector acceptSelector = Selector.open();
		mockRemoteSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
		if (acceptSelector.select(1000) == 0) {
			throw new SocketTimeoutException();
		}
		SocketChannel acceptedSocketChannel = mockRemoteSocketChannel.accept();
		acceptSelector.close();

		ByteBuffer buffer = ByteBuffer.allocate(1);
		acceptedSocketChannel.socket().setSoTimeout(1000);
		int bytesRead = acceptedSocketChannel.read(buffer);
		assertEquals(1, bytesRead);
		
		buffer.flip();
		MessageType messageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.ACTIVE_ALIVE_CONFIRM, messageType);
		buffer.clear();
		
		bytesRead = acceptedSocketChannel.read(buffer);
		assertEquals(1, bytesRead);
		
		buffer.flip();
		messageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.ACTIVE_ALIVE_CONFIRM, messageType);
		
		mockRemoteSocketChannel.close();
		acceptedSocketChannel.close();
	}
	
	/**
	 * Test that the {@link HeartbeatBroadcaster} periodically transmits an <code>ALIVE_CONFIRM</code>
	 * message to multiple remote passive load balancer. 
	 * @throws IOException 
	 */
	@Test
	public void testHeartbeatBroadcaster_broadcastToMultipleRemotes() throws IOException {
		HeartbeatBroadcaster heartbeatBroadcaster = new HeartbeatBroadcaster(TestUtils.getRemoteLoadBalancerSet(3), 1, LoadBalancerState.ACTIVE);
		Thread hbThread = new Thread(heartbeatBroadcaster);
		hbThread.start();
		
		Thread[] mockServerThreads = new Thread[3];
		
		for (int i = 0; i < 3; i++) {
			final int j = i;
			mockServerThreads[i] = new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						ServerSocketChannel mockRemoteSocketChannel = ServerSocketChannel.open();
						mockRemoteSocketChannel.socket().bind(new InetSocketAddress(8000 + j));
						mockRemoteSocketChannel.configureBlocking(false);
						
						Selector acceptSelector = Selector.open();
						mockRemoteSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
						if (acceptSelector.select(1000) == 0) {
							throw new SocketTimeoutException();
						}
						SocketChannel acceptedSocketChannel = mockRemoteSocketChannel.accept();
						acceptSelector.close();
						
						ByteBuffer buffer = ByteBuffer.allocate(1);
						acceptedSocketChannel.socket().setSoTimeout(1000);
						
						for (int i = 0; i < 3; i++) {
							int bytesRead = acceptedSocketChannel.read(buffer);
							assertEquals(1, bytesRead);
							
							buffer.flip();
							MessageType messageType = MessageType.values()[buffer.get()];
							assertEquals(MessageType.ACTIVE_ALIVE_CONFIRM, messageType);
							buffer.clear();
						}

						mockRemoteSocketChannel.close();
						acceptedSocketChannel.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
			});
			mockServerThreads[i].start();
		}
		
		for (int i = 0; i < 3; i++) {
			try {
				mockServerThreads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		hbThread.interrupt();
	}
	
	/**
	 * Test that the {@link HeartbeatBroadcaster} correctly responds to an 
	 * <code>ALIVE_REQUEST</code> message from a remote (mocked) load balancer.
	 * @throws IOException 
	 */
	@Test
	public void testHeartbeatBroadcaster_respondToAliveRequest() throws IOException {
		ServerSocketChannel mockRemoteSocketChannel = ServerSocketChannel.open();
		mockRemoteSocketChannel.socket().bind(new InetSocketAddress(8000));
		mockRemoteSocketChannel.configureBlocking(false);
		
		HeartbeatBroadcaster heartbeatBroadcaster = new HeartbeatBroadcaster(TestUtils.getRemoteLoadBalancerSet(1), 10, LoadBalancerState.ACTIVE);
		Thread hbThread = new Thread(heartbeatBroadcaster);
		hbThread.start();
		
		Selector acceptSelector = Selector.open();
		mockRemoteSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
		if (acceptSelector.select(1000) == 0) {
			throw new SocketTimeoutException();
		}
		SocketChannel acceptedSocketChannel = mockRemoteSocketChannel.accept();
		acceptSelector.close();
		
		ByteBuffer buffer = ByteBuffer.allocate(1);
		acceptedSocketChannel.socket().setSoTimeout(500);
		acceptedSocketChannel.read(buffer);
		
		buffer.flip();
		MessageType messageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.ACTIVE_ALIVE_CONFIRM, messageType);
		buffer.clear();
		
		buffer.put((byte) MessageType.ALIVE_REQUEST.getValue());
		buffer.flip();
		while (buffer.hasRemaining()) {
			acceptedSocketChannel.write(buffer);
		}
		
		buffer.clear();
		
		acceptedSocketChannel.configureBlocking(false);
		Selector readSelector = Selector.open();
		acceptedSocketChannel.register(readSelector, SelectionKey.OP_READ);
		
		if (readSelector.select(1000) == 0) {
			throw new SocketTimeoutException();
		}
		int bytesRead = acceptedSocketChannel.read(buffer);
		assertEquals(1, bytesRead);
		
		buffer.flip();
		messageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.ACTIVE_ALIVE_CONFIRM, messageType);
		
		mockRemoteSocketChannel.close();
		acceptedSocketChannel.close();
		
		hbThread.interrupt();
	}
}
