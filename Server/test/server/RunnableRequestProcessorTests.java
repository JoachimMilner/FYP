package server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import connectionUtils.MessageType;

/**
 * @author Joachim</br>
 * <p>Tests for the {@link RunnableRequestProcessor} class and its instance methods.</p>
 */
public class RunnableRequestProcessorTests {

	/**
	 * Mocked ServerSocketChannel to be used for testing {@link RunnableRequestProcessor} instances. 
	 */
	private ServerSocketChannel mockServerSocketChannel;
	
	/**
	 * Mocked SocketChannel to be used for creating {@link RunnableRequestProcessor} instances. 
	 */
	private SocketChannel mockClientSocketChannel;
	
	/**
	 * Socket Channel created to mock the connection accepted by the server.
	 */
	private SocketChannel acceptedSocketChannel;
	/**
	 * As the {@link RunnableRequestProcessor} requires a SocketChannel to be instantiated,
	 * we create a fake one here.
	 * @throws IOException 
	 */
	@Before
	public void setUp() throws IOException {
		mockClientSocketChannel = SocketChannel.open();
	}
	
	/**
	 * Closes the fake socket channel.
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
	 * Utility method for getting a {@link SocketChannel} that has been accepted by the mocked {@link ServerSocketChannel}. 
	 * This way, we can test the {@link RunnableRequestProcessor} in isolation without a {@link ThreadPooledServer} instance.
	 * @return a <code>SocketChannel</code> that has been accepted by the mock <code>ServerSocketChannel</code>.
	 * @throws IOException
	 */
	private void createAcceptedSocketChannel() throws IOException {
		mockServerSocketChannel = ServerSocketChannel.open();
		mockServerSocketChannel.socket().bind(new InetSocketAddress(8000));
		mockClientSocketChannel.connect(new InetSocketAddress("localhost", 8000));
		acceptedSocketChannel = mockServerSocketChannel.accept();
	}
	
	
	/**
	 * Test successful instantiation of a {@link RunnableRequestProcessor} instance.
	 * @throws IOException 
	 */
	@Test
	public void testCreateRunnableRequestProcessor_successful() throws IOException {
		createAcceptedSocketChannel();
		RunnableRequestProcessor requestProcessor = new RunnableRequestProcessor(acceptedSocketChannel, new ThreadPooledServer(1, 8000));
		assertNotNull(requestProcessor);
	}
	
	/**
	 * Test instantiation of a {@link RunnableRequestProcessor} with a null socket.
	 * Should throw an <code>IllegalArgumentException</code>.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateRunnableRequestProcessor_nullSocket() {
		new RunnableRequestProcessor(null, new ThreadPooledServer(1, 8000));
	}
	
	/**
	 * Test instantiation of a {@link RunnableRequestProcessor} with a socket that has not been connected.
	 * Should throw an <code>IllegalArgumentException</code>.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateRunnableRequestProcessor_disconnectedSocket() {
		new RunnableRequestProcessor(mockClientSocketChannel, new ThreadPooledServer(1, 8000));
	}
	
	/**
	 * Test instantiation of a {@link RunnableRequestProcessor} with a null ThreadPooledServer.
	 * Should throw an <code>IllegalArgumentException</code>.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateRunnableRequestProcessor_nullThreadManager() {
		new RunnableRequestProcessor(mockClientSocketChannel, null);
	}
	
	/**
	 * Test that the {@link RunnableRequestProcessor}'s run method successfully responds to a client request.
	 * @throws IOException 
	 */
	@Test
	public void testRunnableRequestProcessor_run() throws IOException {
		createAcceptedSocketChannel();
		new Thread(new RunnableRequestProcessor(acceptedSocketChannel, new ThreadPooledServer(1, 8000))).start();
		
		// Send a request on the client's socket
		ByteBuffer buffer = ByteBuffer.allocate(81);
		buffer.clear();
		buffer.put((byte)MessageType.CLIENT_REQUEST.getValue());
		for (int i = 0; i < 10; i++) {
			long random = (long) (10000 + Math.random() * 100000);
			buffer.putLong(random);
		}
		buffer.flip();
		while(buffer.hasRemaining()) {
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
	    assertEquals(81, bytesRead);
		buffer.flip();
		MessageType responseMessageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.SERVER_RESPONSE, responseMessageType);
		long[] responseValues = new long[10];
		for (int i = 0; i < responseValues.length; i++) {
			responseValues[i] = buffer.getLong();
			assertTrue(responseValues[i] > 0);
		}
	}
	
	/**
	 * Test that the {@link RunnableRequestProcessor}'s run method throws an IOException
	 * when the {@link MessageType} received is not a <code>CLIENT_REQUEST</code>. 
	 * @throws IOException
	 */
/*	@Test(expected=IOException.class)
	public void testRunnableRequestProcessor_badMessageType() {
		SocketChannel acceptedSocketChannel = null;
		try {
			acceptedSocketChannel = getAcceptedSocketChannel();
		} catch (IOException e) {
			e.printStackTrace();
		}
		new Thread(new RunnableRequestProcessor(acceptedSocketChannel)).start();
		
		// Send a request on the client's socket
		ByteBuffer buffer = ByteBuffer.allocate(81);
		buffer.clear();
		buffer.put((byte)MessageType.SERVER_RESPONSE.getValue());
		for (int i = 0; i < 10; i++) {
			long random = (long) (10000 + Math.random() * 100000);
			buffer.putLong(random);
		}
		buffer.flip();
		while(buffer.hasRemaining()) {
			try {
				mockClientSocketChannel.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}*/
	
	/**
	 * Test that the {@link RunnableRequestProcessor}'s run method throws a BufferUnderflowException
	 * when the request received is smaller than expected
	 * @throws IOException
	 */
/*	@Test(expected=BufferUnderflowException.class)
	public void testRunnableRequestProcessor_runBufferUnderflow() throws IOException {
		SocketChannel acceptedSocketChannel = getAcceptedSocketChannel();
		new Thread(new RunnableRequestProcessor(acceptedSocketChannel)).start();
		
		// Send a request on the client's socket
		ByteBuffer buffer = ByteBuffer.allocate(41);
		buffer.clear();
		buffer.put((byte)MessageType.CLIENT_REQUEST.getValue());
		for (int i = 0; i < 5; i++) {
			long random = (long) (10000 + Math.random() * 100000);
			buffer.putLong(random);
		}
		buffer.flip();
		while(buffer.hasRemaining()) {
			mockClientSocketChannel.write(buffer);
		}
		
		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}*/
	
	/**
	 * Test that the {@link RunnableRequestProcessor}'s run method throws an IOException when
	 * the client's socket is closed before the method can respond.
	 * @throws IOException
	 * @throws InterruptedException 
	 */
/*	@Test(expected=IOException.class)
	public void testRunnableRequestProcessor_runIOException() throws IOException, InterruptedException {
		SocketChannel acceptedSocketChannel = getAcceptedSocketChannel();
		AsyncTester requestProcessorExceptionTester = new AsyncTester(new RunnableRequestProcessor(acceptedSocketChannel));
		requestProcessorExceptionTester.start();
		
		// Send a request on the client's socket
		ByteBuffer buffer = ByteBuffer.allocate(81);
		buffer.clear();
		buffer.put((byte)MessageType.CLIENT_REQUEST.getValue());
		for (int i = 0; i < 10; i++) {
			long random = (long) (10000 + Math.random() * 100000);
			buffer.putLong(random);
		}
		buffer.flip();
		while(buffer.hasRemaining()) {
			mockClientSocketChannel.write(buffer);
		}
		mockClientSocketChannel.close();
		
		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		requestProcessorExceptionTester.test();
	}*/
	
	/**
	 * Test that the {@link RunnableRequestProcessor}'s <code>getResponsesSent</code> method functions correctly
	 * after starting a new instance thread.
	 * @throws IOException
	 */
	@Test
	public void testRunnableRequestProcessor_getResponsesSent() throws IOException {
		createAcceptedSocketChannel();
		RunnableRequestProcessor requestProcessor = new RunnableRequestProcessor(acceptedSocketChannel, new ThreadPooledServer(1, 8000));
		new Thread(requestProcessor).start();
		assertEquals(0, requestProcessor.getResponsesSent());
	}
	
	/**
	 * Test that the {@link RunnableRequestProcessor} returns a response after sending a single client request.
	 * @throws IOException
	 */
	@Test
	public void testRunnableRequestProcessor_getResponsesSentAfterOneRequest() throws IOException {
		createAcceptedSocketChannel();
		RunnableRequestProcessor requestProcessor = new RunnableRequestProcessor(acceptedSocketChannel, new ThreadPooledServer(1, 8000));
		new Thread(requestProcessor).start();
		
		// Send a request on the client's socket
		ByteBuffer buffer = ByteBuffer.allocate(81);
		buffer.clear();
		buffer.put((byte)MessageType.CLIENT_REQUEST.getValue());
		for (int i = 0; i < 10; i++) {
			long random = (long) (10000 + Math.random() * 100000);
			buffer.putLong(random);
		}
		buffer.flip();
		while(buffer.hasRemaining()) {
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
	    assertEquals(81, bytesRead);
	    assertEquals(1, requestProcessor.getResponsesSent());
	}
	
	/**
	 * Test that the {@link RunnableRequestProcessor} returns responses to a random number or requests.
	 * In theory this test should always pass.
	 * @throws IOException
	 */
	@Test
	public void testRunnableRequestProcessor_getResponsesSentRandomRequests() throws IOException {
		createAcceptedSocketChannel();
		RunnableRequestProcessor requestProcessor = new RunnableRequestProcessor(acceptedSocketChannel, new ThreadPooledServer(1, 8000));
		new Thread(requestProcessor).start();
		
		// Send a random number of requests on the client's socket
		int iterations = ThreadLocalRandom.current().nextInt(5, 25);
		for (int i = 0; i < iterations; i++) {
			ByteBuffer buffer = ByteBuffer.allocate(81);
			buffer.clear();
			buffer.put((byte)MessageType.CLIENT_REQUEST.getValue());
			for (int j = 0; j < 10; j++) {
				long random = (long) (10000 + Math.random() * 100000);
				buffer.putLong(random);
			}
			buffer.flip();
			while(buffer.hasRemaining()) {
				mockClientSocketChannel.write(buffer);
			}
			buffer.clear();
			// Set the socket to non blocking and instantiate a selector so we can set a read timeout
		    Selector selector = Selector.open();
		    mockClientSocketChannel.configureBlocking(false);
		    mockClientSocketChannel.register(selector, SelectionKey.OP_READ);
		    if (selector.select(1000) == 0) {
		    	throw new SocketTimeoutException();
		    }
		    mockClientSocketChannel.read(buffer);
		}
	    assertEquals(iterations, requestProcessor.getResponsesSent());
	}
	
	/**
	 * Test that the {@link RunnableRequestProcessor} returns the server's CPU load when sent a 
	 * <code>SERVER_CPU_REQUEST</code>.
	 * @throws IOException
	 */
	@Test
	public void testRunnableRequestProcessor_getServerCPULoad() throws IOException {
		createAcceptedSocketChannel();
		RunnableRequestProcessor requestProcessor = new RunnableRequestProcessor(acceptedSocketChannel, new ThreadPooledServer(1, 8000));
		new Thread(requestProcessor).start();
		
		// Send a request for the server's CPU load
		ByteBuffer buffer = ByteBuffer.allocate(9);
		buffer.clear();
		buffer.put((byte)MessageType.SERVER_CPU_REQUEST.getValue());
		buffer.flip();
		while(buffer.hasRemaining()) {
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
	    assertEquals(9, bytesRead);
		buffer.flip();
		MessageType responseMessageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.SERVER_CPU_LOAD_NOTIFY, responseMessageType);
		double serverLoad = buffer.getDouble();
		assertTrue(serverLoad > 0);
	}
}
