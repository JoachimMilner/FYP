package commsModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

import connectionUtils.MessageType;
import loadBalancer.ServerManager;

/**
 * @author Joachim
 * <p>Tests for the {@link Server} class and its instance methods.</p>
 */
public class ServerTests {

	/**
	 * Test that a new {@link Server} object is created successfully when passed valid parameters.
	 */
	@Test
	public void testServer_createNewServerSuccessful() {
		Server server = new Server(new InetSocketAddress("localhost", 8000));
		assertNotNull(server);
	}
	
	/**
	 * Test that the {@link Server} constructor throws an {@link IllegalArgumentException}
	 * when passed a null address.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testServer_createNewServerNullAddress() {
		new Server(null);
	}
	
	/**
	 * Test the {@link Server} object's <code>getAddress</code> method. Should return the 
	 * address that is passed to the constructor.
	 */
	@Test
	public void testServer_getAddress() {
		InetSocketAddress serverAddress = new InetSocketAddress("localhost", 8000);
		Server server = new Server(serverAddress);
		assertEquals(serverAddress, server.getAddress());
		assertEquals(serverAddress.getHostName(), server.getAddress().getHostName());
		assertEquals(serverAddress.getPort(), server.getAddress().getPort());
	}
	
	/**
	 * Test the {@link Server} object's <code>isAlive</code> method. Should return false
	 * as soon as the object has been instantiated.
	 */
	@Test
	public void testServer_isAliveAfterInstantiation() {
		Server server = new Server(new InetSocketAddress("localhost", 8000));
		assertFalse(server.isAlive());
	}
	
	/**
	 * Test the {@link Server} object's <code>isAlive</code> method. <code>isAlive</code> 
	 * should return true after this </code>updateServerState</code> is called and the remote
	 * (mocked) server is responsive.
	 * @throws IOException 
	 */
	@Test
	public void testServer_isAliveTrue() throws IOException {
		Server server = new Server(new InetSocketAddress("localhost", 8000));
		
		ServerSocketChannel mockServerSocketChannel = ServerSocketChannel.open();
		mockServerSocketChannel.socket().bind(new InetSocketAddress(8000));
		mockServerSocketChannel.configureBlocking(false);
		Selector acceptSelector = Selector.open();
		mockServerSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				server.updateServerState();
			}
			
		}).start();
		
		// Check that the mocked server receives the alive request message
		SocketChannel acceptedSocketChannel = null;
		if (acceptSelector.select(1000) == 0) {
			throw new SocketTimeoutException();
		}
		try {
			acceptedSocketChannel = mockServerSocketChannel.accept();
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertNotNull(acceptedSocketChannel);
		ByteBuffer buffer = ByteBuffer.allocate(9);
	    Selector readSelector = Selector.open();
	    acceptedSocketChannel.configureBlocking(false);
	    acceptedSocketChannel.register(readSelector, SelectionKey.OP_READ);
	    if (readSelector.select(1000) == 0) {
	    	throw new SocketTimeoutException();
	    }
	    int bytesRead = acceptedSocketChannel.read(buffer);
	    assertEquals(1, bytesRead);
		buffer.flip();
		MessageType responseMessageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.SERVER_CPU_REQUEST, responseMessageType);
		
		// Return a random double representing the CPU load from the mocked server and check that the Server object updates it
		buffer.clear();
		buffer.put((byte) MessageType.SERVER_CPU_NOTIFY.getValue());
		double cpuUsage = ThreadLocalRandom.current().nextDouble(0.1, 99.9);
		buffer.putDouble(cpuUsage);
		buffer.flip();
		while (buffer.hasRemaining()) {
			acceptedSocketChannel.write(buffer);
		}
		try {
			Thread.sleep(25);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertTrue(server.isAlive());
		acceptSelector.close();
		readSelector.close();
		mockServerSocketChannel.close();
	}
	
	/**
	 * Test the {@link Server} object's <code>isAlive</code> method. <code>isAlive</code> 
	 * should return false after this <code>updateServerState</code> is called and the remote
	 * (mocked) server is unresponsive. We use reflection here to first set the value to true, 
	 * this is to ensure that the test doesn't incorrectly pass because the value is false by 
	 * default.
	 * @throws IOException 
	 */
	@Test
	public void testServer_isAliveAfterServerUnresponsive() throws IOException {
		Server server = new Server(new InetSocketAddress("localhost", 8000));
		try {
			Field isAliveField = Server.class.getSuperclass().getDeclaredField("isAlive");
			isAliveField.setAccessible(true);
			isAliveField.set(server, new Boolean(true));
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		assertTrue(server.isAlive());
		
		server.updateServerState();
		
		assertFalse(server.isAlive());
	}
	
	/**
	 * Test the {@link Server} object's <code>getCPULoad</code> method. Should return -1
	 * when the object has been instantiated but the cpu load has not been updated.
	 */
	@Test
	public void testServer_getCPULoad() {
		Server server = new Server(new InetSocketAddress("localhost", 8000));
		assertEquals(-1, server.getCPULoad(), 0);
	}
	
	/**
	 * Test that the {@link Server} object's <code>updateCPULoad</code> method works correctly.
	 * Calling the method in a new thread (as it would be done in the {@link ServerManager}) should
	 * request the CPU load of the remote server that the object represents. Here we mock the actual 
	 * server and check that the CPU load value is updated. 
	 * @throws IOException
	 */
	@Test
	public void testServer_updateCPULoad() throws IOException {
		Server server = new Server(new InetSocketAddress("localhost", 8000));
		
		ServerSocketChannel mockServerSocketChannel = ServerSocketChannel.open();
		mockServerSocketChannel.socket().bind(new InetSocketAddress(8000));
 		mockServerSocketChannel.configureBlocking(false);
		Selector acceptSelector = Selector.open();
		mockServerSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				server.updateServerState();
			}
			
		}).start();
		
		// Check that the mocked server receives the correct CPU load request message
		SocketChannel acceptedSocketChannel = null;
		if (acceptSelector.select(1000) == 0) {
			throw new SocketTimeoutException();
		}
		try {
			acceptedSocketChannel = mockServerSocketChannel.accept();
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertNotNull(acceptedSocketChannel);
		ByteBuffer buffer = ByteBuffer.allocate(9);
	    Selector readSelector = Selector.open();
	    acceptedSocketChannel.configureBlocking(false);
	    acceptedSocketChannel.register(readSelector, SelectionKey.OP_READ);
	    if (readSelector.select(1000) == 0) {
	    	throw new SocketTimeoutException();
	    }
	    int bytesRead = acceptedSocketChannel.read(buffer);
	    assertEquals(1, bytesRead);
		buffer.flip();
		MessageType responseMessageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.SERVER_CPU_REQUEST, responseMessageType);
		
		// Return a random double representing the CPU load from the mocked server and check that the Server object updates it
		buffer.clear();
		buffer.put((byte) MessageType.SERVER_CPU_NOTIFY.getValue());
		double cpuUsage = ThreadLocalRandom.current().nextDouble(0.1, 99.9);
		buffer.putDouble(cpuUsage);
		buffer.flip();
		while (buffer.hasRemaining()) {
			acceptedSocketChannel.write(buffer);
		}
		try {
			Thread.sleep(25);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(cpuUsage, server.getCPULoad(), 0);
		acceptSelector.close();
		readSelector.close();
		mockServerSocketChannel.close();
	}
}
