package client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import connectionUtils.MessageType;

/**
 * @author Joachim
 * <p>Tests for the {@link RunnableClientProcess} class and its instance methods.</p>
 */
public class RunnableClientProcessTests {
	
	private static ServerSocketChannel mockServerSocketChannel;
	
	/**
	 * Creates a fake <code>ServerSocketChannel</code> so we can pass a connected <code>SocketChannel</code> to new {@link RunnableClientProcess} instances.
	 * @throws IOException 
	 */
	@Before
	public void setUp() throws IOException {
		mockServerSocketChannel = ServerSocketChannel.open();
		mockServerSocketChannel.socket().bind(new InetSocketAddress(8000));
	}
	
	/**
	 * Close the fake server channel once all tests have completed
	 * @throws IOException
	 */
	@After
	public void cleanup() throws IOException {
		mockServerSocketChannel.close();
	}
	
	/**
	 * Tests creating an instance of the {@link RunnableClientProcess} class. 
	 * @throws IOException 
	 */
	@Test
	public void testCreateRunnableClientProcess_successful() throws IOException {
		SocketChannel clientSocket = SocketChannel.open();
		clientSocket.connect(new InetSocketAddress("localhost", 8000));
		int sendFrequencyMs = (int) Math.random() * 5000;
		RunnableClientProcess client = new RunnableClientProcess(clientSocket, sendFrequencyMs);
		assertNotNull(client);
		clientSocket.close();
	}
	
	/**
	 * Tests creating an instance of the {@link RunnableClientProcess} class with a null {@link SocketChannel} passed in.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateRunnableClientProcess_nullSocket() {
		new RunnableClientProcess(null, 1);
	}
	
	/**
	 * Tests creating an instance of the {@link RunnableClientProcess} class with a disconnected {@link SocketChannel} passed in.
	 * @throws IOException 
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateRunnableClientProcess_disconnectedSocket() throws IOException {
		SocketChannel clientSocket = SocketChannel.open();
		new RunnableClientProcess(clientSocket, 1);
	}
	
	/**
	 * Tests the {@link RunnableClientProcess}'s <code>getRequestsSent</code> method.
	 * @throws IOException 
	 */
	@Test
	public void testRunnableClientProcess_getRequestsSent() throws IOException {
		SocketChannel clientSocket = SocketChannel.open();
		clientSocket.connect(new InetSocketAddress("localhost", 8000));
		int sendFrequencyMs = (int) Math.random() * 5000;
		RunnableClientProcess client = new RunnableClientProcess(clientSocket, sendFrequencyMs);
		int clientRequestsSent = client.getRequestsSent();
		assertEquals(0, clientRequestsSent);
		clientSocket.close();
	}
	
	/**
	 * Test that the {@link RunnableClientProcess}'s <code>SocketChannel.connect</code> is accepted by the mock <code>ServerSocketChannel</code>.
	 * @throws IOException
	 */
	@Test
	public void testRunnableClientProcess_connectSocket() throws IOException {
		SocketChannel clientSocket = SocketChannel.open();
		clientSocket.connect(new InetSocketAddress("localhost", 8000));
		SocketChannel acceptedClientSocket = mockServerSocketChannel.accept();
		int sendFrequencyMs = (int) Math.random() * 5000;
		new RunnableClientProcess(clientSocket, sendFrequencyMs);
		
		assertNotNull(acceptedClientSocket);
		clientSocket.close();
		acceptedClientSocket.close();
	}
	
	/**
	 * Test termination of a {@link RunnableClientProcess}'s instance. After starting a new {@link RunnableClientProcess} thread,
	 * interruption should subsequently kill the thread.
	 * @throws IOException
	 */
	@Test
	public void testRunnableClientProcess_terminate() throws IOException {
		SocketChannel clientSocket = SocketChannel.open();
		clientSocket.configureBlocking(false);
		clientSocket.connect(new InetSocketAddress("localhost", 8000));
		while (!clientSocket.finishConnect()) {}
		mockServerSocketChannel.accept();
		Thread clientThread = new Thread(new RunnableClientProcess(clientSocket, 100));
		clientThread.start();
		clientThread.interrupt();

		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertFalse(clientThread.isAlive());
	}
	
	/**
	 * Starts a {@link RunnableClientProcess} thread and attempts to close the server socket while it is still alive.
	 * The client should catch the <code>IOException</code> and gracefully terminate.
	 * @throws IOException
	 */
/*	@Test
	public void testRunnableClientProcess_killServer() throws IOException {
		SocketChannel clientSocket = SocketChannel.open();
		clientSocket.configureBlocking(false);
		clientSocket.connect(new InetSocketAddress("localhost", 8000));
		while (!clientSocket.finishConnect()) {}
		mockServerSocketChannel.accept();
		Thread clientThread = new Thread(new RunnableClientProcess(clientSocket, 100));
		clientThread.start();
		
		mockServerSocketChannel.close();

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertFalse(clientThread.isAlive());
	}*/
	
	/**
	 * Attempts to leave a {@link RunnableClientProcess} instance running for 2 seconds. Termination after this
	 * duration should execute as normal.
	 * @throws IOException
	 */
	@Test
	public void testRunnableClientProcess_runFor1Second() throws IOException {
		SocketChannel clientSocket = SocketChannel.open();
		clientSocket.configureBlocking(false);
		clientSocket.connect(new InetSocketAddress("localhost", 8000));
		while (!clientSocket.finishConnect()) {}
		mockServerSocketChannel.accept();
		Thread clientThread = new Thread(new RunnableClientProcess(clientSocket, 50));
		clientThread.start();
		
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
	 * Test starting a {@link RunnableClientProcess} thread - attempt to retrieve the request bytes on the <code>ServerSocketChannel</code>.
	 * Client should have sent a message ID and an array of 10 random long values.
	 * @throws IOException
	 */
	@Test
	public void testRunnableClientProcess_run() throws IOException {
		SocketChannel clientSocket = SocketChannel.open();
		clientSocket.configureBlocking(false);
		clientSocket.connect(new InetSocketAddress("localhost", 8000));
		while (!clientSocket.finishConnect()) {}
		SocketChannel acceptedClientSocket = mockServerSocketChannel.accept();
		Thread clientThread = new Thread(new RunnableClientProcess(clientSocket, 100));
		clientThread.start();
		
		ByteBuffer buffer = ByteBuffer.allocate(81);
		int bytesRead = acceptedClientSocket.read(buffer);
		assertEquals(81, bytesRead);
		
		buffer.flip();
		MessageType messageType = MessageType.values()[buffer.get()];
		assertEquals(MessageType.CLIENT_REQUEST, messageType);
		
		ArrayList<Long> clientRequest = new ArrayList<>();
		while(buffer.hasRemaining()) {
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
		clientSocket.close();
		acceptedClientSocket.close();
	}
}
