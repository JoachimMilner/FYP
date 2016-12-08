package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import connectionUtils.ConnectNIO;
import connectionUtils.MessageType;

/**
 * @author Joachim</br>
 *         <p>
 *         This class represents a single virtualized client process and can
 *         only be instantiated by using <code>initialiseClientPool</code> on a
 *         {@link VirtualClientManager} object.
 *         </p>
 */
public class RunnableClientProcess implements Runnable {

	/**
	 * The address that the {@link VirtualClientManager} has provided to request
	 * a TCP connection on.
	 */
	private InetSocketAddress serverAddress;
	
	/**
	 * The VirtualClientManager instance that this object will use to update the total
	 * requests sent and responses received.
	 */
	private VirtualClientManager clientManager;

	/**
	 * The socket that this virtual client will use to send and receive
	 * messages.
	 */
	private SocketChannel socketChannel;

	/**
	 * The frequency that this client will send requests at in milliseconds.
	 */
	private int sendFrequencyMs;


	/**
	 * Creates a RunnableClientProcess which will connect to the supplied
	 * <code>serverAddress</code>. When <code>run</code> is called, this object
	 * will create a non-blocking socket to send requests on at the specified
	 * <code>sendFrequencyMs</code>.
	 * 
	 * @param serverAddress
	 *            the remote address that this virtual client will connect to.
	 * @param clientManager
	 *            the {@link VirtualClientManager} that created this
	 *            RunnableClientProcess.
	 * @param sendFrequencyMs
	 *            the frequency at which the client will send requests in
	 *            milliseconds.
	 * @throws IllegalArgumentException
	 *             if the <code>SocketChannel</code> passed in is null or has
	 *             not been connected.
	 */
	public RunnableClientProcess(InetSocketAddress serverAddress, VirtualClientManager clientManager,
			int sendFrequencyMs) {
		if (serverAddress == null) {
			throw new IllegalArgumentException("serverAddress cannot be null.");
		}

		if (clientManager == null) {
			throw new IllegalArgumentException("clientManager cannot be null.");
		}

		
		this.serverAddress = serverAddress;
		this.clientManager = clientManager;
		this.sendFrequencyMs = sendFrequencyMs;
	}

	/*
	 * (non-Javadoc) Called on RunnableClientProcess thread creation - transmits
	 * requests to the server at the specified frequency.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		socketChannel = ConnectNIO.getNonBlockingSocketChannel(serverAddress);
		while (!Thread.currentThread().isInterrupted()) {
			sendRequest();
			checkForMessages();
			try {
				Thread.sleep(sendFrequencyMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		try {
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * Generates 10 random long values and sends to the server on the provided
	 * <code>SocketChannel</code>.
	 * 
	 * @throws IOException
	 */
	private void sendRequest() {
		ByteBuffer buffer = ByteBuffer.allocate(81);
		//buffer.clear();
		buffer.put((byte) MessageType.CLIENT_REQUEST.getValue());
		for (int i = 0; i < 10; i++) {
			long random = (long) (10000 + Math.random() * 100000);
			buffer.putLong(random);
		}
		buffer.flip();
		while (buffer.hasRemaining()) {
			try {
				socketChannel.write(buffer);
			} catch (IOException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		clientManager.incrementTotalRequestsSent();
		//System.out.println("Virtual Client (ID:" + Thread.currentThread().getId() + ") sent request to server.");
	}
	
	
	/**
	 * Checks this virtual client's {@link SocketChannel} for messages and increments the
	 * {@link VirtualClientManager}'s <code>totalResponsesReceived</code> value if a valid
	 * message is received.
	 */
	private void checkForMessages() {
		boolean messageReceived = false;
		ByteBuffer buffer = ByteBuffer.allocate(81);
		try {
			while (socketChannel.read(buffer) > 0) {
				buffer.flip();
				MessageType messageType = MessageType.values()[buffer.get()];
				if (messageType.equals(MessageType.SERVER_RESPONSE)) {
					int i = 0;
					for (; i < 10; i++) {
						try {
							buffer.getLong();
						} catch(BufferUnderflowException e) {
							// Bad/Incomplete message received
							e.printStackTrace();
							break;
						}
					}
					if (i == 10) {
						messageReceived = true;
					}
				}
			}
		} catch (IOException e) {
			//e.printStackTrace();
		}
		if (messageReceived) {
			clientManager.incrementTotalResponsesReceived();
		}
	} 
}
