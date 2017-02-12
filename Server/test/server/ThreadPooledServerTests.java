package server;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.junit.Test;

import connectionUtils.MessageType;

/**
 * @author Joachim
 * <p>Tests for the {@link ThreadPooledServer} class and its instance methods.</p>
 */
public class ThreadPooledServerTests {

	/**
	 * Tests creating an instance of the {@link ThreadPooledServer} class. 
	 */
	@Test
	public void testCreateThreadPooledServer_successful() {
		ThreadPooledServer server = new ThreadPooledServer(8000);
		assertNotNull(server);
	}
	
	/**
	 * Tests that the {@link ThreadPooledServer}'s <code>run</code> method creates a <code>ServerSocketChannel</code> 
	 * by creating a mock client <code>SocketChannel</code> and attempting to connect. 
	 * @throws IOException 
	 */
	@Test
	public void testThreadPooledServer_socketCreation() throws IOException {
		Thread serverThread = new Thread(new ThreadPooledServer(8000));
		serverThread.start();
		SocketChannel mockClient = SocketChannel.open();
		mockClient.connect(new InetSocketAddress("localhost", 8000));
		assertTrue(mockClient.isConnected());
		serverThread.interrupt();
		mockClient.close();
	}
	
	/**
	 * Tests execution of the thread pool. Connecting a mock client should increase the active thread count
	 * by one, as well as returning a correct response to a request.
	 * @throws IOException
	 */
	@Test
	public void testThreadPooledServer_createNewThreads() throws IOException {
		Set<Thread> threadSetDefault = Thread.getAllStackTraces().keySet();
		Thread serverThread = new Thread(new ThreadPooledServer(8000));
		serverThread.start();
		Set<Thread> threadSetServerInit = Thread.getAllStackTraces().keySet();
		assertEquals(threadSetDefault.size() + 1, threadSetServerInit.size());
		
		SocketChannel mockClient = SocketChannel.open();
		mockClient.connect(new InetSocketAddress("localhost", 8000));
		
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Set<Thread> threadSetPoolIncremented = Thread.getAllStackTraces().keySet();
		assertEquals(threadSetServerInit.size() + 1, threadSetPoolIncremented.size());
		serverThread.interrupt();
		mockClient.close();
	}
	
	/**
	 * Here we test that the server correctly responds to a request sent by the <code>mockClient</code>.
	 * @throws IOException
	 */
	@Test
	public void testThreadPooledServer_getClientResponse() throws IOException {
		Thread serverThread = new Thread(new ThreadPooledServer(8000));
		serverThread.start();
		SocketChannel mockClient = SocketChannel.open();
		mockClient.connect(new InetSocketAddress("localhost", 8000));
		ByteBuffer buffer = ByteBuffer.allocate(81);
		buffer.clear();
		buffer.put((byte) MessageType.CLIENT_REQUEST.getValue());
		for (int i = 0; i < 10; i++) {
			long random = (long) (10000 + Math.random() * 100000);
			buffer.putLong(random);
		}
		buffer.flip();
		while (buffer.hasRemaining()) {
			try {
				mockClient.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	    buffer.clear();
		// Set the socket to non blocking and instantiate a selector so we can set a read timeout
	    Selector selector = Selector.open();
	    mockClient.configureBlocking(false);
	    mockClient.register(selector, SelectionKey.OP_READ);
	    if (selector.select(1000) == 0) {
	    	throw new SocketTimeoutException();
	    }
		int bytesRead = mockClient.read(buffer);
		assertEquals(81, bytesRead);
		buffer.flip();
		MessageType responseMessageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.SERVER_RESPONSE, responseMessageType);
		long[] responseValues = new long[10];
		for (int i = 0; i < responseValues.length; i++) {
			responseValues[i] = buffer.getLong();
			assertTrue(responseValues[i] > 0);
		}
		serverThread.interrupt();
		selector.close();
		mockClient.close();
	}
	
	/**
	 * @throws IOException
	 */
/*	@Test
	public void testThreadPooledServer_getTotalRequestsReceived() throws IOException {
		
	}*/
	
	
	/**
	 * Basic test to check that the {@link ThreadPooledServer}'s updateTotalRequestsReceived method works correctly
	 */
	@Test
	public void testThreadPooledServer_updateTotalRequestsReceived() {
		ThreadPooledServer threadPooledServer = new ThreadPooledServer(8000);
		assertEquals(0, threadPooledServer.getTotalRequestsReceived());
		threadPooledServer.incrementTotalRequestsReceived();
		assertEquals(1, threadPooledServer.getTotalRequestsReceived());
	}
	
	/**
	 * Check that the {@link ThreadPooledServer}'s updateTotalRequestsReceived method works correctly
	 * with a request sent from a mock client
	 * @throws IOException 
	 */
	@Test
	public void testThreadPooledServer_updateTotalRequestsWithMockClient() throws IOException {
		ThreadPooledServer threadPooledServer = new ThreadPooledServer(8000);
		Thread serverThread = new Thread(threadPooledServer);
		serverThread.start();
		assertEquals(0, threadPooledServer.getTotalRequestsReceived());
		SocketChannel mockClient = SocketChannel.open();
		mockClient.connect(new InetSocketAddress("localhost", 8000));
		ByteBuffer buffer = ByteBuffer.allocate(81);
		buffer.clear();
		buffer.put((byte) MessageType.CLIENT_REQUEST.getValue());
		for (int i = 0; i < 10; i++) {
			long random = (long) (10000 + Math.random() * 100000);
			buffer.putLong(random);
		}
		buffer.flip();
		while (buffer.hasRemaining()) {
			try {
				mockClient.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		buffer.clear();
		// Set the socket to non blocking and instantiate a selector so we can set a read timeout
	    Selector selector = Selector.open();
	    mockClient.configureBlocking(false);
	    mockClient.register(selector, SelectionKey.OP_READ);
	    if (selector.select(1000) == 0) {
	    	throw new SocketTimeoutException();
	    }
		int bytesRead = mockClient.read(buffer);
		assertEquals(81, bytesRead);
		assertEquals(1, threadPooledServer.getTotalRequestsReceived());
		serverThread.interrupt();
		selector.close();
		mockClient.close();
	}
	
	/**
	 * Basic test to check that the {@link ThreadPooledServer}'s updateTotalResponsesSent method works correctly
	 */
	@Test
	public void testThreadPooledServer_updateTotalResponsesSent() {
		ThreadPooledServer threadPooledServer = new ThreadPooledServer(8000);
		assertEquals(0, threadPooledServer.getTotalResponsesSent());
		threadPooledServer.incrementTotalResponsesSent();
		assertEquals(1, threadPooledServer.getTotalResponsesSent());
	}

	/**
	 * Check that the {@link ThreadPooledServer}'s updateTotalRequestsReceived method works correctly
	 * with a request sent from a mock client
	 * @throws IOException 
	 */
	@Test
	public void testThreadPooledServer_updateTotalResponsesWithMockClient() throws IOException {
		ThreadPooledServer threadPooledServer = new ThreadPooledServer(8000);
		Thread serverThread = new Thread(threadPooledServer);
		serverThread.start();
		assertEquals(0, threadPooledServer.getTotalResponsesSent());
		SocketChannel mockClient = SocketChannel.open();
		mockClient.connect(new InetSocketAddress("localhost", 8000));
		ByteBuffer buffer = ByteBuffer.allocate(81);
		buffer.clear();
		buffer.put((byte) MessageType.CLIENT_REQUEST.getValue());
		for (int i = 0; i < 10; i++) {
			long random = (long) (10000 + Math.random() * 100000);
			buffer.putLong(random);
		}
		buffer.flip();
		while (buffer.hasRemaining()) {
			try {
				mockClient.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		assertEquals(1, threadPooledServer.getTotalResponsesSent());
		serverThread.interrupt();
		mockClient.close();
	}
	
	/**
	 * Tests that the {@link ThreadPooledServer} initialises its mBeanServer 
	 * property when started. This is to allow processor threads to retrieve 
	 * the machine's CPU load when needed. 
	 */
	@Test
	public void testThreadPooledServer_initMBeanServer() {
		ThreadPooledServer threadPooledServer = new ThreadPooledServer(8000);
		Thread serverThread = new Thread(threadPooledServer);
		serverThread.start();
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			
		}
		
		assertNotNull(threadPooledServer.getMBeanServer());
		
		serverThread.interrupt();
	}
}
