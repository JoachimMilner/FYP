package server;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.junit.Test;

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
		ThreadPooledServer server = new ThreadPooledServer(1);
		assertNotNull(server);
	}
	
	/**
	 * Tests creating an instance of the {@link ThreadPooledServer} class. 
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateThreadPooledServer_threadPoolTooSmall() {
		new ThreadPooledServer(0);
	}
	
	/**
	 * Tests that the {@link ThreadPooledServer}'s <code>run</code> method creates a <code>ServerSocketChannel</code> 
	 * by creating a mock client <code>SocketChannel</code> and attempting to connect. 
	 * @throws IOException 
	 */
	@Test
	public void testThreadPooledServer_socketCreation() throws IOException {
		new Thread(new ThreadPooledServer(1)).start();
		SocketChannel mockClient = SocketChannel.open();
		mockClient.connect(new InetSocketAddress("localhost", 8000));
		assertTrue(mockClient.isConnected());
	}
}
