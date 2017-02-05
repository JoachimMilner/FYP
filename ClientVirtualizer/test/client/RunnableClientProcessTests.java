package client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
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
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import connectionUtils.MessageType;

/**
 * @author Joachim
 *         <p>
 *         Tests for the {@link RunnableClientProcess} class and its instance
 *         methods.
 *         </p>
 */
public class RunnableClientProcessTests {

	private ServerSocketChannel mockServerSocketChannel;

	/**
	 * Creates a fake <code>ServerSocketChannel</code> so we can pass a
	 * connected <code>SocketChannel</code> to new {@link RunnableClientProcess}
	 * instances.
	 * 
	 * @throws IOException
	 */
	@Before
	public void setUp() throws IOException {
		mockServerSocketChannel = ServerSocketChannel.open();
		mockServerSocketChannel.socket().bind(new InetSocketAddress(8000));
	}

	/**
	 * Closes the fake server channel.
	 * 
	 * @throws IOException
	 */
	@After
	public void cleanup() throws IOException {
		mockServerSocketChannel.close();
	}

	/**
	 * Tests creating an instance of the {@link RunnableClientProcess} class.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCreateRunnableClientProcess_successful() throws IOException {
		int sendFrequencyMs = (int) Math.random() * 5000;
		VirtualClientManager clientManager = new VirtualClientManager(1, 1, 1, 1, 1,
				new InetSocketAddress("localhost", 8000));
		RunnableClientProcess client = new RunnableClientProcess(new InetSocketAddress("localhost", 8000),
				clientManager, sendFrequencyMs, 1);
		assertNotNull(client);
	}

	/**
	 * Tests creating an instance of the {@link RunnableClientProcess} class
	 * with a null {@link InetSocketAddress} passed in.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateRunnableClientProcess_nullSocket() {
		VirtualClientManager clientManager = new VirtualClientManager(1, 1, 1, 1, 1,
				new InetSocketAddress("localhost", 8000));
		new RunnableClientProcess(null, clientManager, 1, 1);
	}

	/**
	 * Tests creating an instance of the {@link RunnableClientProcess} class
	 * with a null {@link VirtualClientManager} passed in.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateRunnableClientProcess_nullClientManager() {
		new RunnableClientProcess(new InetSocketAddress("localhost", 8000), null, 1, 1);
	}

	/**
	 * Tests creating an instance of the {@link RunnableClientProcess} class
	 * with a zero request limit. Should throw IllegalArgumentException as a
	 * client should only be started with a non-zero request limit
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateRunnableClientProcess_zeroRequestsSet() {
		int sendFrequencyMs = (int) Math.random() * 5000;
		VirtualClientManager clientManager = new VirtualClientManager(1, 1, 1, 1, 1,
				new InetSocketAddress("localhost", 8000));
		RunnableClientProcess client = new RunnableClientProcess(new InetSocketAddress("localhost", 8000),
				clientManager, sendFrequencyMs, 0);
		assertNotNull(client);
	}

	/**
	 * Tests the {@link RunnableClientProcess}'s <code>getRequestsSent</code>
	 * method.
	 * 
	 * @throws IOException
	 */
	/*
	 * @Test public void testRunnableClientProcess_getRequestsSent() throws
	 * IOException { int sendFrequencyMs = (int) Math.random() * 5000;
	 * VirtualClientManager clientManager = new VirtualClientManager(1, 1, 1);
	 * RunnableClientProcess client = new RunnableClientProcess(new
	 * InetSocketAddress("localhost", 8000), clientManager, sendFrequencyMs);
	 * int clientRequestsSent = client.getRequestsSent(); assertEquals(0,
	 * clientRequestsSent); }
	 */

	/**
	 * Test that the {@link RunnableClientProcess}'s
	 * <code>SocketChannel.connect</code> is accepted by the mock
	 * <code>ServerSocketChannel</code>.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testRunnableClientProcess_connectSocket() throws IOException {
		int sendFrequencyMs = (int) Math.random() * 5000;
		VirtualClientManager clientManager = new VirtualClientManager(1, 1, 1, 1, 1,
				new InetSocketAddress("localhost", 8000));
		Thread clientThread = new Thread(
				new RunnableClientProcess(new InetSocketAddress("localhost", 8000), clientManager, sendFrequencyMs, 1));
		clientThread.start();

		SocketChannel acceptedClientSocket = mockServerSocketChannel.accept();
		assertNotNull(acceptedClientSocket);
		clientThread.interrupt();
		acceptedClientSocket.close();
	}

	/**
	 * Test termination of a {@link RunnableClientProcess}'s instance. After
	 * starting a new {@link RunnableClientProcess} thread, interruption should
	 * subsequently kill the thread.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testRunnableClientProcess_terminate() throws IOException {
		VirtualClientManager clientManager = new VirtualClientManager(1, 1, 1, 1, 1,
				new InetSocketAddress("localhost", 8001));
		Thread clientThread = new Thread(
				new RunnableClientProcess(new InetSocketAddress("localhost", 8001), clientManager, 100, 10));
		clientThread.start();

		mockNameServiceAndLoadBalancers(8001, 8002);

		mockServerSocketChannel.accept();
		clientThread.interrupt();

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertFalse(clientThread.isAlive());
	}

	/**
	 * Test that a {@link RunnableClientProcess} terminates itself after it has
	 * sent the specified number of requests to the server.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testRunnableClientProcess_selfTerminate() throws IOException {
		VirtualClientManager clientManager = new VirtualClientManager(1, 1, 1, 1, 1,
				new InetSocketAddress("localhost", 8001));
		Thread clientThread = new Thread(
				new RunnableClientProcess(new InetSocketAddress("localhost", 8001), clientManager, 10, 5));
		clientThread.start();

		mockNameServiceAndLoadBalancers(8001, 8002);

		mockServerSocketChannel.accept();

		try {
			Thread.sleep(2100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertFalse(clientThread.isAlive());
	}

	/**
	 * Starts a {@link RunnableClientProcess} thread and attempts to close the
	 * server socket while it is still alive. The client should catch the
	 * <code>IOException</code> and gracefully terminate.
	 * 
	 * @throws IOException
	 */
	/*
	 * @Test public void testRunnableClientProcess_killServer() throws
	 * IOException { SocketChannel clientSocket = SocketChannel.open();
	 * clientSocket.configureBlocking(false); clientSocket.connect(new
	 * InetSocketAddress("localhost", 8000)); while
	 * (!clientSocket.finishConnect()) {} mockServerSocketChannel.accept();
	 * Thread clientThread = new Thread(new RunnableClientProcess(clientSocket,
	 * 100)); clientThread.start();
	 * 
	 * mockServerSocketChannel.close();
	 * 
	 * try { Thread.sleep(500); } catch (InterruptedException e) {
	 * e.printStackTrace(); }
	 * 
	 * assertFalse(clientThread.isAlive()); }
	 */

	/**
	 * Attempts to leave a {@link RunnableClientProcess} instance running for 1
	 * second. Termination after this duration should execute as normal.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testRunnableClientProcess_runFor1Second() throws IOException {
		VirtualClientManager clientManager = new VirtualClientManager(1, 1, 1, 1, 1,
				new InetSocketAddress("localhost", 8001));
		Thread clientThread = new Thread(
				new RunnableClientProcess(new InetSocketAddress("localhost", 8001), clientManager, 50, 100));
		clientThread.start();

		mockNameServiceAndLoadBalancers(8001, 8002);

		mockServerSocketChannel.accept();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		clientThread.interrupt();

		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertFalse(clientThread.isAlive());
	}

	/**
	 * Test starting a {@link RunnableClientProcess} thread - attempt to
	 * retrieve the request bytes on the <code>ServerSocketChannel</code>.
	 * Client should have sent a message ID and an array of 10 random long
	 * values.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testRunnableClientProcess_run() throws IOException {
		VirtualClientManager clientManager = new VirtualClientManager(1, 1, 1, 1, 1,
				new InetSocketAddress("localhost", 8001));
		Thread clientThread = new Thread(
				new RunnableClientProcess(new InetSocketAddress("localhost", 8001), clientManager, 100, 50));
		clientThread.start();

		mockNameServiceAndLoadBalancers(8001, 8002);

		SocketChannel acceptedClientSocket = mockServerSocketChannel.accept();

		ByteBuffer buffer = ByteBuffer.allocate(81);
		int bytesRead = acceptedClientSocket.read(buffer);
		assertEquals(81, bytesRead);

		buffer.flip();
		MessageType messageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.CLIENT_REQUEST, messageType);

		ArrayList<Long> clientRequest = new ArrayList<>();
		while (buffer.hasRemaining()) {
			clientRequest.add(buffer.getLong());
		}

		assertEquals(10, clientRequest.size());
		for (Long value : clientRequest) {
			assertTrue(value > 0);
		}
		clientThread.interrupt();
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		acceptedClientSocket.close();
	}

	/**
	 * Test that a new {@link RunnableClientProcess} sends a
	 * <code>HOST_ADDR_REQUEST</code> message to the name service asking for the
	 * address of the load balancer when it starts.
	 * 
	 * @throws IOException
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	@Test
	public void testRunnableClientProcess_requestHostNameResolution() throws IOException, NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
		ServerSocketChannel mockNameServiceSocketChannel = getMockServerSocketChannel(8001);

		VirtualClientManager clientManager = new VirtualClientManager(1, 1, 1, 1, 1,
				new InetSocketAddress("localhost", 8001));
		RunnableClientProcess clientProcess = new RunnableClientProcess(new InetSocketAddress("localhost", 8001),
				clientManager, 100, 50);
		Field loadBalancerAddressField = clientProcess.getClass().getDeclaredField("loadBalancerAddress");
		loadBalancerAddressField.setAccessible(true);
		loadBalancerAddressField.set(clientProcess, new InetSocketAddress("localhost", 8002));
		Thread clientThread = new Thread(clientProcess);
		clientThread.start();

		Selector acceptSelector = Selector.open();
		mockNameServiceSocketChannel.configureBlocking(false);
		mockNameServiceSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
		if (acceptSelector.select(1000) == 0) {
			throw new SocketTimeoutException();
		}
		SocketChannel acceptedNameServiceSocketChannel = mockNameServiceSocketChannel.accept();
		ByteBuffer buffer = ByteBuffer.allocate(1);

		Selector readSelector = Selector.open();
		acceptedNameServiceSocketChannel.configureBlocking(false);
		acceptedNameServiceSocketChannel.register(readSelector, SelectionKey.OP_READ);
		if (readSelector.select(1000) == 0) {
			throw new SocketTimeoutException();
		}
		int bytesRead = acceptedNameServiceSocketChannel.read(buffer);
		assertEquals(1, bytesRead);

		buffer.flip();
		MessageType messageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.HOST_ADDR_REQUEST, messageType);

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		clientThread.interrupt();
		acceptSelector.close();
		readSelector.close();
		mockNameServiceSocketChannel.close();
	}

	/**
	 * Test that a new {@link RunnableClientProcess} sends a
	 * <code>HOST_ADDR_REQUEST</code> message to the name service asking for the
	 * address of the load balancer when it starts. In this test we delay the
	 * response from the name service in order to check that the client
	 * periodically sends a request.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testRunnableClientProcess_requestHostNameResolutionDelayedReply() throws IOException {
		ServerSocketChannel mockNameServiceSocketChannel = getMockServerSocketChannel(8001);

		VirtualClientManager clientManager = new VirtualClientManager(1, 1, 1, 1, 1,
				new InetSocketAddress("localhost", 8001));
		Thread clientThread = new Thread(
				new RunnableClientProcess(new InetSocketAddress("localhost", 8001), clientManager, 100, 50));
		clientThread.start();

		Selector acceptSelector = Selector.open();
		mockNameServiceSocketChannel.configureBlocking(false);
		mockNameServiceSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
		if (acceptSelector.select(1000) == 0) {
			throw new SocketTimeoutException();
		}
		SocketChannel acceptedNameServiceSocketChannel = mockNameServiceSocketChannel.accept();
		ByteBuffer buffer = ByteBuffer.allocate(1);

		int bytesRead = acceptedNameServiceSocketChannel.read(buffer);
		assertEquals(1, bytesRead);

		buffer.flip();
		MessageType messageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.HOST_ADDR_REQUEST, messageType);

		buffer.clear();

		bytesRead = acceptedNameServiceSocketChannel.read(buffer);
		assertEquals(1, bytesRead);

		buffer.flip();
		messageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.HOST_ADDR_REQUEST, messageType);

		clientThread.interrupt();
		acceptSelector.close();
		mockNameServiceSocketChannel.close();
	}

	/**
	 * Test that a new {@link RunnableClientProcess} sends a
	 * <code>SERVER_TOKEN_REQUEST</code> message to the load balancer asking for
	 * a server token. Requires the name service to be mocked so it can send a
	 * load balancer address to the client.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testRunnableClientProcess_requestServerToken() throws IOException {
		ServerSocketChannel mockNameServiceSocketChannel = getMockServerSocketChannel(8001);

		VirtualClientManager clientManager = new VirtualClientManager(1, 1, 1, 1, 1,
				new InetSocketAddress("localhost", 8001));
		Thread clientThread = new Thread(
				new RunnableClientProcess(new InetSocketAddress("localhost", 8001), clientManager, 100, 50));
		clientThread.start();

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
		acceptedNameServiceSocketChannel.read(buffer);
		buffer.flip();
		MessageType messageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.HOST_ADDR_REQUEST, messageType);

		// Send the client an address for the mocked load balancer
		buffer.clear();
		CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
		buffer.put((byte) MessageType.HOST_ADDR_RESPONSE.getValue());
		buffer.putInt(8002);
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

		ServerSocketChannel mockLoadBalancerSocketChannel = getMockServerSocketChannel(8002);
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
		int bytesRead = acceptedMockLoadBalancerSocketChannel.read(loadBalancerBuffer);
		assertEquals(1, bytesRead);

		loadBalancerBuffer.flip();
		messageType = MessageType.values()[loadBalancerBuffer.get()];
		assertEquals(MessageType.AVAILABLE_SERVER_REQUEST, messageType);

		loadBalancerBuffer.clear();
		loadBalancerBuffer.put((byte) MessageType.SERVER_TOKEN.getValue());
		loadBalancerBuffer.putLong(System.currentTimeMillis() / 1000 + 50);
		loadBalancerBuffer.putInt(mockServerSocketChannel.socket().getLocalPort());
		loadBalancerBuffer.put(encoder.encode(CharBuffer.wrap("localhost")));
		loadBalancerBuffer.flip();
		while (loadBalancerBuffer.hasRemaining()) {
			acceptedMockLoadBalancerSocketChannel.write(loadBalancerBuffer);
		}

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		clientThread.interrupt();
		loadBalancerAcceptSelector.close();
		loadBalancerReadSelector.close();
		mockLoadBalancerSocketChannel.close();
	}

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
	 * 
	 * @throws IOException
	 */
	private void mockNameServiceAndLoadBalancers(int nameServicePort, int loadBalancerPort) throws IOException {
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
		loadBalancerBuffer.putInt(mockServerSocketChannel.socket().getLocalPort());
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
