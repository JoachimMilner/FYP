package server;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import connectionUtils.MessageType;

/**
 * @author Joachim</br> Class used to processing received client requests. When a
 *         request is received at the <code>ServerSocketChannel</code> in the
 *         {@link ThreadPooledServer}, it is delegated to
 *         RunnableRequestProcessor which will handle the request processing.
 */
public class RunnableRequestProcessor implements Runnable {

	/**
	 * The accepted client's SocketChannel.
	 */
	private SocketChannel socketChannel;
	
	/**
	 * The total number of messages that this object has sent in response to client requests.
	 */
	private int responsesSent = 0;
		
	/**
	 * The ThreadPooledServer instance that manages this RunnableRequestProcessor. The RunnableRequestProcessor
	 * will update the total requests received and responses sent values via this object.
	 */
	private ThreadPooledServer threadManager;

	
	/**
	 * Constructor for creating a new RunnableRequestProcessor. Accepts a
	 * {@link SocketChannel}, reads the request bytes from it, processes a
	 * response and sends it. Whenever a message is sent or received, this
	 * instance will update the total value in the <code>threadManager</code>.
	 * 
	 * @param socketChannel
	 */
	public RunnableRequestProcessor(SocketChannel socketChannel, ThreadPooledServer threadManager) {
		if (socketChannel == null || !socketChannel.isConnected())
			throw new IllegalArgumentException("Null or disconnected SocketChannel.");
		if (threadManager == null)
			throw new IllegalArgumentException("ThreadPooledServer instance cannot be null.");
		
		this.socketChannel = socketChannel;
		this.threadManager = threadManager;
	}
	

	/*
	 * (non-Javadoc) To be called on <code>Thread.start()</code> to listen for
	 * and process incoming client requests on the provided
	 * <code>SocketChannel</code>
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while (socketChannel.isConnected()) {
			try {
				ByteBuffer buffer = ByteBuffer.allocate(81);
				int bytesRead = socketChannel.read(buffer);

				if (bytesRead == -1) { // Something went wrong, close channel and terminate
					socketChannel.close();
					break;
				} else {
					buffer.flip();
					MessageType messageType = MessageType.values()[buffer.get()];

					switch (messageType) {
					case CLIENT_REQUEST:
						threadManager.incrementTotalRequestsReceived();
						long[] requestData = new long[10];
						for (int i = 0; i < requestData.length; i++) {
							try {
								requestData[i] = buffer.getLong();
							} catch (BufferUnderflowException e) {
								// Something went wrong, close channel and terminate
								socketChannel.close();
								break;
							}
						}
						System.out.println("Server Thread (ID:" + Thread.currentThread().getId() + ") received client request: " + Arrays.toString(requestData));
						long startTime = System.currentTimeMillis();
						
						long[] processedResponseValues = processSumOfPrimes(requestData);
						
						long endTime = System.currentTimeMillis();
		
						System.out.println("Server Thread (ID:" + Thread.currentThread().getId() + ") finished processing client request in " + (endTime - startTime) + "ms, sending response..." );

						buffer.clear();
						buffer.put((byte) MessageType.SERVER_RESPONSE.getValue());
						for (int i = 0; i < processedResponseValues.length; i++) {
							buffer.putLong(processedResponseValues[i]);
						}

						buffer.flip();
						while (buffer.hasRemaining()) {
							socketChannel.write(buffer);

						}
						threadManager.incrementTotalResponsesSent();
						responsesSent++;
						break;
					case SERVER_CPU_REQUEST:
						// Get the local machine's CPU usage here. We will mock this for now.
						buffer.clear();
						buffer.put((byte) MessageType.SERVER_CPU_NOTIFY.getValue());
						double cpuUsage = ThreadLocalRandom.current().nextDouble(0.1, 99.9);
						buffer.putDouble(cpuUsage);
						buffer.flip();
						while (buffer.hasRemaining()) {
							socketChannel.write(buffer);
						}
						break;
					default:
						// Received a bad request
						throw new IOException("Bad MessageType received");
					}
				}
			} catch (IOException e) {
				//e.printStackTrace();
				
				break;
			}
		}
		System.out.println("Client disconected.");
	}
	

	/**
	 * @return The total number of responses that this RunnableRequestProcessor has sent.
	 */
	public int getResponsesSent() {
		return responsesSent;
	}
	

	/**
	 * Private method for processing a client's request. Finds the total sum of
	 * all prime numbers less than each long value and stores it at the
	 * equivalent index in the returned array.To be used in conjunction with
	 * <code>isAPrime()</code>.
	 * 
	 * @param inputData
	 *            the long values to be processed, as an indexed array.
	 * @return the processed values as an array of longs.
	 */
	private long[] processSumOfPrimes(long[] inputData) {
		long[] summatedPrimeValues = new long[inputData.length];
		for (int i = 0; i < inputData.length; i++) {
			long sum = 0;
			for (long j = 2; j < inputData[i]; j++) {
				if (isAPrime(j)) {
					sum += j;
				}
			}
			summatedPrimeValues[i] = sum;
		}
		return summatedPrimeValues;
	}
	

	/**
	 * Determines whether a number is a prime number.
	 * 
	 * @param value
	 *            the input to check.
	 * @return true if the value is a prime number, otherwise false.
	 */
	private boolean isAPrime(long value) {
		for (long i = 2; i <= Math.sqrt(value); i++) {
			if (value % i == 0) {
				return false;
			}
		}
		return true;
	}
}
