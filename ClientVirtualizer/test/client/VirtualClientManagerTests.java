package client;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

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
		VirtualClientManager clientManager = new VirtualClientManager(1, 0, 0);
		assertNotNull(clientManager);
	}
	
	/**
	 * Tests creating an instance of the {@link VirtualClientManager} class with
	 * less than one client passed. Should throw IllegalArgumentException
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateVirtualClientManager_lessThanOneClient() {
		new VirtualClientManager(0, 0, 0);
	}
	
	/**
	 * Tests creating an instance of the {@link VirtualClientManager} class with
	 * less than one client passed. Should throw IllegalArgumentException
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateVirtualClientManager_wrongFrequencyParams() {
		new VirtualClientManager(1, 1, 0);
	}
	
	/**
	 * Tests the {@link VirtualClientManager}'s <code>getNumberOfClients</code> method.
	 */
	@Test
	public void testVirtualClientManager_getNumberOfClients() {
		VirtualClientManager clientManager = new VirtualClientManager(1, 0, 0);
		assertEquals(1, clientManager.getNumberOfClients());
	}
	
	/**
	 * Tests the {@link VirtualClientManager}'s <code>getNumberOfLiveClients</code> method. Should return 0
	 * as we have not initialised the client pool.
	 */
	@Test
	public void testVirtualClientManager_getNumberOfLiveClients() {
		VirtualClientManager clientManager = new VirtualClientManager(1, 0, 0);
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
		VirtualClientManager clientManager = new VirtualClientManager(2, 0, 0);
		clientManager.initialiseClientPool();
		assertEquals(2, clientManager.getNumberOfLiveClients());
		mockServerSocketChannel.close();
	}
	
	/**
	 * Tests the {@link VirtualClientManager}'s <code>getTotalRequestsSent</code> method before client pool initialisation.
	 */
	@Test
	public void testVirtualClientManager_getTotalRequestsSentNoInitialisation() {
		VirtualClientManager clientManager = new VirtualClientManager(1, 0, 0);
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
		VirtualClientManager clientManager = new VirtualClientManager(3, 50, 150);
		clientManager.initialiseClientPool();
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		assertTrue(clientManager.getTotalRequestsSent() > 0);
		mockServerSocketChannel.close();
	}
	
	/**
	 * Tests the {@link VirtualClientManager}'s <code>getTotalResponsesReceived</code> method. Will
	 * return 0 in this instance as we have not initialised the client pool.
	 */
	@Test
	public void testVirtualClientManager_getTotalResponsesReceived() {
		VirtualClientManager clientManager = new VirtualClientManager(1, 0, 0);
		assertEquals(0, clientManager.getTotalResponsesReceived());
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
		VirtualClientManager clientManager = new VirtualClientManager(1, 0, 0);

		clientManager.initialiseClientPool();
		
		SocketChannel clientSocket = mockServerSocketChannel.accept();
		
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
		    clientSocket.write(buffer);
		}
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		assertEquals(1, clientManager.getTotalResponsesReceived());
		mockServerSocketChannel.close();
	}
}
