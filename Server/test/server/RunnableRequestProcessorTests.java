package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joachim
 * <p>Tests for the {@link RunnableRequestProcessor} class and its instance methods.</p>
 */
public class RunnableRequestProcessorTests {

	
	/**
	 * Mocked SocketChannel to be used for creating {@link RunnableRequestProcessor} instances. 
	 */
	private SocketChannel mockClientSocketChannel;
	
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
	}
	
	/**
	 * Utility method for getting a {@link SocketChannel} that has been accepted by the mocked {@link ServerSocketChannel}. 
	 * This way, we can test the {@link RunnableRequestProcessor} in isolation without a {@link ThreadPooledServer} instance.
	 * @return a <code>SocketChannel</code> that has been accepted by the mock <code>ServerSocketChannel</code>.
	 * @throws IOException
	 */
	private SocketChannel getAcceptedSocketChannel() throws IOException {
		ServerSocketChannel mockServerSocketChannel = ServerSocketChannel.open();
		mockServerSocketChannel.socket().bind(new InetSocketAddress(8000));
		mockClientSocketChannel.connect(new InetSocketAddress("localhost", 8000));
		SocketChannel acceptedSocketChannel = mockServerSocketChannel.accept();
		return acceptedSocketChannel;
	}
	
	
	/**
	 * Test successful instantiation of a {@link RunnableRequestProcessor} instance.
	 * @throws IOException 
	 */
	@Test
	public void testCreateRunnableRequestProcessor_successful() throws IOException {
		SocketChannel acceptedSocketChannel = getAcceptedSocketChannel();
		RunnableRequestProcessor requestProcessor = new RunnableRequestProcessor(acceptedSocketChannel);
	}
	
	/**
	 * Test instantiation of a {@link RunnableRequestProcessor} with a null socket.
	 * Should throw an <code>IllegalArgumentException</code>.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateRunnableRequestProcessor_nullSocket() {
		RunnableRequestProcessor requestProcessor = new RunnableRequestProcessor(null);
	}
	
	/**
	 * Test instantiation of a {@link RunnableRequestProcessor} with a socket that has not been connected.
	 * Should throw an <code>IllegalArgumentException</code>.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateRunnableRequestProcessor_disconnectedSocket() {
		RunnableRequestProcessor requestProcessor = new RunnableRequestProcessor(mockClientSocketChannel);
	}
}
