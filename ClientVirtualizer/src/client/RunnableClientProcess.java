package client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import connectionUtils.MessageType;

/**
 * @author Joachim
 * <p>This class represents a single virtualized client process and can only be instantiated
 * by using <code>initialiseClientPool</code> on a {@link VirtualClientManager} object.</p>
 */
public class RunnableClientProcess implements Runnable {
	
	/**
	 * The socket that the {@link VirtualClientManager} has provided to send requests on.
	 */
	private SocketChannel clientSocket;
	
	/**
	 * The frequency that this client will send requests at in milliseconds.
	 */
	private int sendFrequencyMs;
	
	/**
	 * The total number of requests that this client has sent since t=0.
	 */
	private int requestsSent = 0;
	
	
	/**
	 * Creates a RunnableClientProcess with the VirtualClientManager's <code>SocketChannel</code>.
	 * When <code>run</code> is called, this object will send requests on the socket at the specified
	 * <code>sendFrequencyMs</code>.
	 * @param clientID a unique integer to identify this client.
	 * @param clientSocket the socket channel that the client will write to.
	 * @param sendFrequencyMs the frequency at which the client will send requests in milliseconds.
	 * @throws IllegalArgumentException if the <code>SocketChannel</code> passed in is null or has not been connected.
	 */
	public RunnableClientProcess(SocketChannel clientSocket, int sendFrequencyMs) {
		if (clientSocket == null || !clientSocket.isConnected()) throw new IllegalArgumentException("Null or disconnected SocketChannel.");
		
		this.clientSocket = clientSocket;
		this.sendFrequencyMs = sendFrequencyMs;
	}
	

	/* (non-Javadoc)
	 * Called on RunnableClientProcess thread creation - transmits requests to the server at the specified frequency.
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while(!Thread.currentThread().isInterrupted()) {
			sendRequest();
			try {
				Thread.sleep(sendFrequencyMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
	

	/**
	 * @return the number of requests that this virtual client has sent to the server.
	 */
	public int getRequestsSent() {
		return requestsSent;
	}
	

	/**
	 * Generates 10 random long values and sends to the server on the provided <code>SocketChannel</code>.
	 * @throws IOException 
	 */
	private void sendRequest() {
		ByteBuffer buffer = ByteBuffer.allocate(81);
		buffer.clear();
		buffer.put((byte)MessageType.CLIENT_REQUEST.getValue());
		for (int i = 0; i < 10; i++) {
			long random = (long) (10000 + Math.random() * 100000);
			buffer.putLong(random);
		}
		buffer.flip();
		while(buffer.hasRemaining()) {
			try {
				clientSocket.write(buffer);
			} catch (IOException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		requestsSent++;
	}
}
