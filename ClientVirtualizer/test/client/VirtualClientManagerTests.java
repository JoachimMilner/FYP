package client;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.junit.Test;

import connectionUtils.MessageType;

/**
 * @author Joachim
 * <p>Tests for the {@link VirtualClientManager} class and its instance methods.</p>
 */
public class VirtualClientManagerTests {

	
	/**
	 * Tests creating an instance of the {@link VirtualClientManager} class.
	 */
	@Test
	public void testCreateVirtualClientManager_successful() {
		VirtualClientManager clientManager = new VirtualClientManager(1, 0, 0, 0, 0, new InetSocketAddress("localhost", 8000));
		assertNotNull(clientManager);
	}
	
	/**
	 * Tests creating an instance of the {@link VirtualClientManager} class with
	 * less than one client passed. Should throw IllegalArgumentException
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateVirtualClientManager_lessThanOneClient() {
		new VirtualClientManager(0, 0, 0, 0, 0, new InetSocketAddress("localhost", 8000));
	}
	
	/**
	 * Tests creating an instance of the {@link VirtualClientManager} class with
	 * invalid sending frequency values. Should throw IllegalArgumentException
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateVirtualClientManager_illegalFrequencyParams() {
		new VirtualClientManager(1, 1, 0, 0, 0, new InetSocketAddress("localhost", 8000));
	}
	
	/**
	 * Test creating an instance of the {@link VirtualClientManager} class with
	 * invalid min/max client request values. Should throw IllegalArgumentException
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateVirtualClientManager_illegalClientRequestParams() {
		new VirtualClientManager(1, 1, 1, 1, 0, new InetSocketAddress("localhost", 8000));
	}
	
	/**
	 * Test creating an instance of the {@link VirtualClientManager} class with
	 * a null {@link InetSocketAddress} passed in for the <code>nameServiceAddress</code>.
	 * Should throw IllegalArgumentException
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateVirtualClientManager_nullNameServiceAddress() {
		new VirtualClientManager(1, 0, 0, 0, 0, null);
	}
	
	/**
	 * Tests the {@link VirtualClientManager}'s <code>getNumberOfClients</code> method.
	 */
	@Test
	public void testVirtualClientManager_getNumberOfClients() {
		VirtualClientManager clientManager = new VirtualClientManager(1, 0, 0, 0, 0, new InetSocketAddress("localhost", 8000));
		assertEquals(1, clientManager.getNumberOfClients());
	}
	
	/**
	 * Tests the {@link VirtualClientManager}'s <code>getNumberOfLiveClients</code> method. Should return 0
	 * as we have not initialised the client pool.
	 */
	@Test
	public void testVirtualClientManager_getNumberOfLiveClients() {
		VirtualClientManager clientManager = new VirtualClientManager(1, 0, 0, 0, 0, new InetSocketAddress("localhost", 8000));
		assertEquals(0, clientManager.getNumberOfLiveClients());
	}

	/**
	 * Tests the {@link VirtualClientManager}'s <code>getNumberOfLiveClients</code> method. Should return
	 * the number that we pass in as <code>numberOfClients</code>.
	 * @throws IOException 
	 */
	@Test
	public void testVirtualClientManager_getNumberOfLiveClientsAfterInitialisation() throws IOException {
		ServerSocketChannel mockServerSocketChannel = null;
		mockServerSocketChannel = ServerSocketChannel.open();
		mockServerSocketChannel.socket().bind(new InetSocketAddress(8000));
		VirtualClientManager clientManager = new VirtualClientManager(2, 100, 200, 5, 10, new InetSocketAddress("localhost", 8004));
		clientManager.initialiseClientPool();
		assertEquals(2, clientManager.getNumberOfLiveClients());
		clientManager.stop();
		mockServerSocketChannel.close();
	}
	
	/**
	 * Tests that the {@link VirtualClientManager} is able to receive server replies on its
	 * <code>SocketChannel</code>. Sends an arbitrary response to the clientManager, then
	 * checks that its totalResponsesReceived has incremented.
	 * @throws IOException 
	 */
	@Test
	public void testVirtualClientManager_getServerResponse() throws IOException {
		ServerSocketChannel mockServerSocketChannel = null;
		mockServerSocketChannel = ServerSocketChannel.open();
		mockServerSocketChannel.socket().bind(new InetSocketAddress(8000));
		VirtualClientManager clientManager = new VirtualClientManager(1, 5, 25, 50, 100, new InetSocketAddress("localhost", 8004));

		clientManager.initialiseClientPool();
		
		SocketChannel serverSideClientSocket = mockServerSocketChannel.accept();
		
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		ByteBuffer buffer = ByteBuffer.allocate(81);
		buffer.clear();
		buffer.put((byte)MessageType.SERVER_RESPONSE.getValue());
		for (int i = 0; i < 10; i++) {
			long random = (long) (10000 + Math.random() * 100000);
			buffer.putLong(random);
		}
		buffer.flip();
		while(buffer.hasRemaining()) {
		    serverSideClientSocket.write(buffer);
		}
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		assertEquals(1, clientManager.getTotalResponsesReceived());
		clientManager.stop();
		mockServerSocketChannel.close();
	}
	
	/**
	 * Tests the {@link VirtualClientManager}'s <code>getTotalRequestsSent</code> method before client pool initialisation.
	 */
	@Test
	public void testVirtualClientManager_getTotalRequestsSentNoInitialisation() {
		VirtualClientManager clientManager = new VirtualClientManager(1, 0, 0, 0, 0, new InetSocketAddress("localhost", 8004));
		assertEquals(0, clientManager.getTotalRequestsSent());
	}
	
	/**
	 * Tests the {@link VirtualClientManager}'s <code>getTotalRequestsSent</code> method after client pool initialisation
	 * and a brief pause (to allow clients to begin sending requests).
	 * Difficult to predict the exact number of requests that have been sent here so just check that it is greater than 0.
	 * @throws IOException 
	 */
	@Test
	public void testVirtualClientManager_getTotalRequestsSentAfterInitialisation() throws IOException {
		ServerSocketChannel mockServerSocketChannel = null;
		mockServerSocketChannel = ServerSocketChannel.open();
		mockServerSocketChannel.socket().bind(new InetSocketAddress(8000));
		VirtualClientManager clientManager = new VirtualClientManager(3, 50, 150, 50, 100, new InetSocketAddress("localhost", 8004));
		clientManager.initialiseClientPool();
		
		mockNameServiceAndLoadBalancers(8004, 8003, 8000);
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		assertTrue(clientManager.getTotalRequestsSent() > 0);
		clientManager.stop();
		mockServerSocketChannel.close();
	}
	
	/**
	 * Tests the {@link VirtualClientManager}'s <code>getTotalResponsesReceived</code> method. Will
	 * return 0 in this instance as we have not initialised the client pool.
	 */
	@Test
	public void testVirtualClientManager_getTotalResponsesReceived() {
		VirtualClientManager clientManager = new VirtualClientManager(1, 0, 0, 0, 0, new InetSocketAddress("localhost", 8004));
		assertEquals(0, clientManager.getTotalResponsesReceived());
	}
	
	/**
	 * Test that the total responses received is what we expect when we initialise a fixed number
	 * of virtual clients with fixed sending frequencies and request limits.
	 * @throws IOException
	 */
/*	@Test
	public void testVirtualClientManager_getTotalResponsesReceivedAfterInitialisation() throws IOException {
		ServerSocketChannel mockServerSocketChannel = null;
		mockServerSocketChannel = ServerSocketChannel.open();
		mockServerSocketChannel.socket().bind(new InetSocketAddress(8000));
		VirtualClientManager clientManager = new VirtualClientManager(2, 50, 50, 5, 5);
		clientManager.initialiseClientPool();
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		assertEquals(10, clientManager.getTotalResponsesReceived());
		mockServerSocketChannel.close();
	}*/
	
	/**
	 * Utility method for mocking a ServerSocketChannel, to be used as either
	 * the name service or load balancer.
	 */
	private ServerSocketChannel getMockServerSocketChannel(int port) {
		ServerSocketChannel mockServerSocketChannel = null;
		try {
			mockServerSocketChannel = ServerSocketChannel.open();
			mockServerSocketChannel.socket().bind(new InetSocketAddress(port));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return mockServerSocketChannel;
	}
	
	/**
	 * Method for mocking the behaviour of the name service and load balancer so
	 * we can test the {@link RunnableClientProcess} which requires
	 * communication with both.
	 * @throws IOException 
	 */
	private void mockNameServiceAndLoadBalancers(int nameServicePort, int loadBalancerPort, int serverPort) throws IOException {
		ServerSocketChannel mockNameServiceSocketChannel = getMockServerSocketChannel(nameServicePort);
		Selector nameServiceAcceptSelector = Selector.open();
		mockNameServiceSocketChannel.configureBlocking(false);
		mockNameServiceSocketChannel.register(nameServiceAcceptSelector, SelectionKey.OP_ACCEPT);
		if (nameServiceAcceptSelector.select(1000) == 0) {
			throw new SocketTimeoutException();
		}
		SocketChannel acceptedNameServiceSocketChannel = mockNameServiceSocketChannel.accept();
		ByteBuffer buffer = ByteBuffer.allocate(17);

		Selector nameServiceReadSelector = Selector.open();
		acceptedNameServiceSocketChannel.configureBlocking(false);
		acceptedNameServiceSocketChannel.register(nameServiceReadSelector, SelectionKey.OP_READ);
		if (nameServiceReadSelector.select(1000) == 0) {
			throw new SocketTimeoutException();
		}

		// Send the client an address for the mocked load balancer
		buffer.clear();
		CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
		buffer.put((byte) MessageType.HOST_ADDR_RESPONSE.getValue());
		buffer.putInt(loadBalancerPort);
		buffer.put(encoder.encode(CharBuffer.wrap("localhost")));
		buffer.flip();
		while (buffer.hasRemaining()) {
			acceptedNameServiceSocketChannel.write(buffer);
		}
		nameServiceAcceptSelector.close();
		nameServiceReadSelector.close();
		mockNameServiceSocketChannel.close();
		
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		ServerSocketChannel mockLoadBalancerSocketChannel = getMockServerSocketChannel(loadBalancerPort);
		mockLoadBalancerSocketChannel.configureBlocking(false);
		Selector loadBalancerAcceptSelector = Selector.open();
		mockLoadBalancerSocketChannel.register(loadBalancerAcceptSelector, SelectionKey.OP_ACCEPT);
		if (loadBalancerAcceptSelector.select(1000) == 0) {
			throw new SocketTimeoutException();
		}
		SocketChannel acceptedMockLoadBalancerSocketChannel = mockLoadBalancerSocketChannel.accept();
		ByteBuffer loadBalancerBuffer = ByteBuffer.allocate(28);

		acceptedMockLoadBalancerSocketChannel.configureBlocking(false);
		Selector loadBalancerReadSelector = Selector.open();
		acceptedMockLoadBalancerSocketChannel.register(loadBalancerReadSelector, SelectionKey.OP_READ);
		if (loadBalancerReadSelector.select(1000) == 0) {
			throw new SocketTimeoutException();
		}
		
		loadBalancerBuffer.clear();
		loadBalancerBuffer.put((byte) MessageType.SERVER_TOKEN.getValue());
		loadBalancerBuffer.putLong(System.currentTimeMillis() / 1000 + 50);
		loadBalancerBuffer.putInt(serverPort);
		loadBalancerBuffer.put(encoder.encode(CharBuffer.wrap("localhost")));
		loadBalancerBuffer.flip();
		while (loadBalancerBuffer.hasRemaining()) {
			acceptedMockLoadBalancerSocketChannel.write(loadBalancerBuffer);
		}

		loadBalancerAcceptSelector.close();
		loadBalancerReadSelector.close();
		mockLoadBalancerSocketChannel.close();
	}
}
