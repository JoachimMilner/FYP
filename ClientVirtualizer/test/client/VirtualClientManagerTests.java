package client;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

import org.junit.Test;

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
	 */
	@Test
	public void testVirtualClientManager_getNumberOfLiveClientsAfterInitialisation() {
		ServerSocketChannel mockServerSocketChannel;
		try {
			mockServerSocketChannel = ServerSocketChannel.open();
			mockServerSocketChannel.socket().bind(new InetSocketAddress(8000));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		VirtualClientManager clientManager = new VirtualClientManager(2, 0, 0);
		clientManager.initialiseClientPool();
		assertEquals(2, clientManager.getNumberOfLiveClients());
	}
	
	/**
	 * Tests the {@link VirtualClientManager}'s <code>getTotalRequestsSent</code> method.
	 */
/*	@Test
	public void testVirtualClientManager_getTotalRequestsSent() {
		VirtualClientManager clientManager = new VirtualClientManager(1, 0, 0);
		assertEquals(0, clientManager.getTotalRequestsSent());
	}*/
	
	/**
	 * Tests the {@link VirtualClientManager}'s <code>getTotalResponsesReceived</code> method. Will
	 * return 0 in this instance as we have not initialised the client pool.
	 */
	@Test
	public void testVirtualClientManager_getTotalResponsesReceived() {
		VirtualClientManager clientManager = new VirtualClientManager(1, 0, 0);
		assertEquals(0, clientManager.getTotalResponsesReceived());
	}
}
