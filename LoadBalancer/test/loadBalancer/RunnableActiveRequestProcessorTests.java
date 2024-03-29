package loadBalancer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import commsModel.Server;
import connectionUtils.MessageType;
import testUtils.TestUtils;

/**
 * @author Joachim
 *         <p>
 *         Tests for the {@link RunnableClientRequestProcessor} class and its
 *         instance methods.
 *         </p>
 *
 */
public class RunnableActiveRequestProcessorTests {

	/**
	 * Mocked ServerSocketChannel to be used for testing
	 * {@link RunnableClientRequestProcessor} instances.
	 */
	private ServerSocketChannel mockServerSocketChannel;

	/**
	 * Mocked SocketChannel to be used for creating
	 * {@link RunnableClientRequestProcessor} instances.
	 */
	private SocketChannel mockClientSocketChannel;

	/**
	 * Socket Channel created to mock the connection accepted by the server.
	 */
	private SocketChannel acceptedSocketChannel;

	/**
	 * As the {@link RunnableClientRequestProcessor} requires a SocketChannel to
	 * be instantiated, we create a fake one here.
	 * 
	 * @throws IOException
	 */
	@Before
	public void setUp() throws IOException {
		mockClientSocketChannel = SocketChannel.open();
	}

	/**
	 * Closes the fake socket channel.
	 * 
	 * @throws IOException
	 */
	@After
	public void cleanup() throws IOException {
		mockClientSocketChannel.close();
		if (mockServerSocketChannel != null) {
			mockServerSocketChannel.close();
		}
		if (acceptedSocketChannel != null) {
			acceptedSocketChannel.close();
		}
	}

	/**
	 * Utility method for getting a {@link SocketChannel} that has been accepted
	 * by the mocked {@link ServerSocketChannel}. This way, we can test the
	 * {@link RunnableClientRequestProcessor} in isolation without an
	 * {@link AbstractLoadBalancer} instance.
	 * 
	 * @return a <code>SocketChannel</code> that has been accepted by the mock
	 *         <code>ServerSocketChannel</code>.
	 * @throws IOException
	 */
	private void createAcceptedSocketChannel() throws IOException {
		mockServerSocketChannel = ServerSocketChannel.open();
		mockServerSocketChannel.socket().bind(new InetSocketAddress(8000));
		mockClientSocketChannel.connect(new InetSocketAddress("localhost", 8000));
		acceptedSocketChannel = mockServerSocketChannel.accept();
	}

	/**
	 * Tests successful creation of a new {@link RunnableClientRequestProcessor}
	 * instance.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCreateActiveRequestProcessor_successful() throws IOException {
		createAcceptedSocketChannel();
		ServerManager serverManager = new ServerManager(TestUtils.getServerSet(1));
		RunnableClientRequestProcessor activeRequestProcessor = new RunnableClientRequestProcessor(
				acceptedSocketChannel, serverManager);
		assertNotNull(activeRequestProcessor);
	}

	/**
	 * Test instantiation of a {@link RunnableClientRequestProcessor} with a
	 * null socket. Should throw an <code>IllegalArgumentException</code>.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateActiveRequestProcessor_nullSocket() {
		ServerManager serverManager = new ServerManager(TestUtils.getServerSet(1));
		new RunnableClientRequestProcessor(null, serverManager);
	}

	/**
	 * Test instantiation of a {@link RunnableClientRequestProcessor} with a
	 * socket that has not been connected. Should throw an
	 * <code>IllegalArgumentException</code>.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateActiveRequestProcessor_disconnectedSocket() {
		ServerManager serverManager = new ServerManager(TestUtils.getServerSet(1));
		new RunnableClientRequestProcessor(mockClientSocketChannel, serverManager);
	}

	/**
	 * Test instantiation of a {@link RunnableClientRequestProcessor} with a
	 * null {@link ServerManager}. Should throw an
	 * <code>IllegalArgumentException</code>.
	 * 
	 * @throws IOException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateActiveRequestProcessor_nullServerManager() throws IOException {
		createAcceptedSocketChannel();
		new RunnableClientRequestProcessor(acceptedSocketChannel, null);
	}

	/**
	 * Tests that the <code>socketChannel</code> property of the
	 * {@link RunnableClientRequestProcessor} is set correctly in the class
	 * constructor. Use reflection to check the value after the constructor is
	 * called.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testActiveRequestProcessorConstructor_socketChannelIsSet() throws NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException, IOException {
		createAcceptedSocketChannel();
		ServerManager serverManager = new ServerManager(TestUtils.getServerSet(1));
		RunnableClientRequestProcessor activeRequestProcessor = new RunnableClientRequestProcessor(
				acceptedSocketChannel, serverManager);

		Field socketChannelField = activeRequestProcessor.getClass().getSuperclass().getDeclaredField("socketChannel");
		socketChannelField.setAccessible(true);
		assertEquals(acceptedSocketChannel, socketChannelField.get(activeRequestProcessor));
	}

	/**
	 * Tests that the <code>serverManager</code> property of the
	 * {@link RunnableClientRequestProcessor} is set correctly in the class
	 * constructor. Use reflection to check the value after the constructor is
	 * called.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testActiveRequestProcessorConstructor_serverManagerIsSet() throws NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException, IOException {
		createAcceptedSocketChannel();
		ServerManager serverManager = new ServerManager(TestUtils.getServerSet(1));
		RunnableClientRequestProcessor activeRequestProcessor = new RunnableClientRequestProcessor(
				acceptedSocketChannel, serverManager);

		Field serverManagerField = activeRequestProcessor.getClass().getDeclaredField("serverManager");
		serverManagerField.setAccessible(true);
		assertEquals(serverManager, serverManagerField.get(activeRequestProcessor));
	}

	/**
	 * Test that the {@link RunnableClientRequestProcessor} correctly processes
	 * and responds to a mock client requesting the details of an available
	 * server.
	 * 
	 * @throws IOException
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	@Test
	public void testActiveRequestProcessor_processClientRequest() throws IOException, NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
		createAcceptedSocketChannel();
		Set<Server> servers = TestUtils.getServerSet(1);
		ServerManager serverManager = new ServerManager(servers);
		RunnableClientRequestProcessor activeRequestProcessor = new RunnableClientRequestProcessor(
				acceptedSocketChannel, serverManager);

		// Set CPU load
		Field serverCPULoadField = servers.iterator().next().getClass().getDeclaredField("cpuLoad");
		serverCPULoadField.setAccessible(true);
		serverCPULoadField.set(servers.iterator().next(), 50.00);
		// Set address
		Field serverAddressField = servers.iterator().next().getClass().getSuperclass().getDeclaredField("address");
		serverAddressField.setAccessible(true);
		serverAddressField.set(servers.iterator().next(), new InetSocketAddress("127.0.0.2", 8080));
		// Set isAlive
		Field serverIsAliveField = servers.iterator().next().getClass().getSuperclass().getDeclaredField("isAlive");
		serverIsAliveField.setAccessible(true);
		serverIsAliveField.set(servers.iterator().next(), true);
		// Set default token expiry
		int defaultTokenExpiration = 50;
		Server.setDefaultTokenExpiration(defaultTokenExpiration);

		Thread requestProcessorThread = new Thread(activeRequestProcessor);
		requestProcessorThread.start();

		ByteBuffer buffer = ByteBuffer.allocate(28);
		buffer.clear();
		buffer.put((byte) MessageType.AVAILABLE_SERVER_REQUEST.getValue());
		buffer.flip();
		while (buffer.hasRemaining()) {
			mockClientSocketChannel.write(buffer);
		}

		buffer.clear();
		Selector selector = Selector.open();
		mockClientSocketChannel.configureBlocking(false);
		mockClientSocketChannel.register(selector, SelectionKey.OP_READ);
		if (selector.select(1000) == 0) {
			throw new SocketTimeoutException();
		}
		long expectedTokenExpiryTime = System.currentTimeMillis() / 1000 + defaultTokenExpiration;
		int bytesRead = mockClientSocketChannel.read(buffer);
		assertEquals(22, bytesRead);

		buffer.flip();
		MessageType responseMessageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.SERVER_TOKEN, responseMessageType);

		long serverTokenExpiryTime = buffer.getLong();
		assertEquals(expectedTokenExpiryTime, serverTokenExpiryTime, 1);

		int serverPort = buffer.getInt();
		assertEquals(8080, serverPort);

		CharBuffer charBuffer = Charset.forName("UTF-8").decode(buffer);
		String hostAddress = charBuffer.toString();
		assertEquals("127.0.0.2", hostAddress);

		selector.close();
		requestProcessorThread.interrupt();
	}

	/**
	 * Test that the {@link RunnableClientRequestProcessor} responds to an
	 * <code>ALIVE_REQUEST</code> message. This is to confirm that the active
	 * load balancer can verify its status in the case that the connection from
	 * the {@link HeartbeatBroadcaster} drops but the process is still running.
	 * @throws IOException 
	 */
	@Test
	public void testActiveRequestProcessor_respondToAliveRequest() throws IOException {
		createAcceptedSocketChannel();
		ServerManager serverManager = new ServerManager(TestUtils.getServerSet(1));
		RunnableClientRequestProcessor activeRequestProcessor = new RunnableClientRequestProcessor(
				acceptedSocketChannel, serverManager);
		
		Thread requestProcessorThread = new Thread(activeRequestProcessor);
		requestProcessorThread.start();
		
		ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.clear();
		buffer.put((byte) MessageType.ALIVE_REQUEST.getValue());
		buffer.flip();
		while (buffer.hasRemaining()) {
			mockClientSocketChannel.write(buffer);
		}

		buffer.clear();
		Selector selector = Selector.open();
		mockClientSocketChannel.configureBlocking(false);
		mockClientSocketChannel.register(selector, SelectionKey.OP_READ);
		if (selector.select(1000) == 0) {
			throw new SocketTimeoutException();
		}
		
		int bytesRead = mockClientSocketChannel.read(buffer);
		assertEquals(1, bytesRead);
		
		buffer.flip();
		
		MessageType messageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.ACTIVE_ALIVE_CONFIRM, messageType);
		
		selector.close();
		requestProcessorThread.interrupt();
	}
}
