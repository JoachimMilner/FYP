package server;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import connectionUtils.MessageType;
import logging.ComponentLogger;
import logging.LogMessageType;

/**
 * @author Joachim Class used to processing received client requests. When a
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
	 * The total number of messages that this object has sent in response to
	 * client requests.
	 */
	private int responsesSent = 0;

	/**
	 * The ThreadPooledServer instance that manages this
	 * RunnableRequestProcessor. The RunnableRequestProcessor will update the
	 * total requests received and responses sent values via this object.
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
		boolean isClient = false;
		while (socketChannel.isConnected()) {
			try {
				ByteBuffer buffer = ByteBuffer.allocate(81);
				int bytesRead = socketChannel.read(buffer);

				if (bytesRead == -1) { // Something went wrong, close channel
										// and terminate
					socketChannel.close();
					break;
				} else {
					buffer.flip();
					MessageType messageType = MessageType.values()[buffer.get()];

					switch (messageType) {
					case CLIENT_REQUEST:
						if (!isClient) {
							System.out.println("Server received connection request.");
							isClient = true;
						}
						threadManager.incrementTotalRequestsReceived();
						long[] requestData = new long[10];
						for (int i = 0; i < requestData.length; i++) {
							try {
								requestData[i] = buffer.getLong();
							} catch (BufferUnderflowException e) {
								// Something went wrong, close channel and
								// terminate
								socketChannel.close();
								break;
							}
						}
						//System.out.println("Server Thread (ID:" + Thread.currentThread().getId()
							//	+ ") received client request: " + Arrays.toString(requestData));
						//long startTime = System.currentTimeMillis();

						long[] processedResponseValues = processSumOfPrimes(requestData);

						//long endTime = System.currentTimeMillis();

						//System.out.println("Server Thread (ID:" + Thread.currentThread().getId()
							//	+ ") finished processing client request in " + (endTime - startTime)
							//	+ "ms, sending response...");

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
						double cpuUsage = getSystemCPULoad();
						if (!Double.isNaN(cpuUsage)) {
							//cpuUsage = -1.00;
							ComponentLogger.getInstance().log(LogMessageType.SERVER_CPU_LOAD, new Double(cpuUsage));
							System.out.println(cpuUsage);
							buffer.clear();
							buffer.put((byte) MessageType.SERVER_CPU_NOTIFY.getValue());
							buffer.putDouble(cpuUsage);
							buffer.flip();
							while (buffer.hasRemaining()) {
								socketChannel.write(buffer);
							}
						}
						break;
					default:
						// Received a bad request
						throw new IOException("Bad MessageType received");
					}
				}
			} catch (IOException e) {
				// e.printStackTrace();

				break;
			}
		}
		if (isClient) {
			System.out.println("Client disconected.");
		}
	}

	/**
	 * @return The total number of responses that this RunnableRequestProcessor
	 *         has sent.
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

	/**
	 * Retrieves the {@link MBeanServer} object from the
	 * <code>threadManager</code> and attempts to get the current CPU load for
	 * this machine.
	 * </br>
	 * Method based on code from: {@link http://stackoverflow.com/a/21962037}
	 * 
	 * @return a double representing the CPU load of the machine with 2 decimal
	 *         point precision, or NaN if the value cannot be obtained.
	 */
	private double getSystemCPULoad() {
		try {
			ObjectName name    = ObjectName.getInstance("java.lang:type=OperatingSystem");
			AttributeList list = threadManager.getMBeanServer().getAttributes(name, new String[]{ "SystemCpuLoad" });
			if (list.isEmpty()) {
				return Double.NaN;
			}
			
			Attribute att = (Attribute)list.get(0);
	    	Double value  = (Double)att.getValue();
	    	
		    if (value == -1.0) {
		    	return Double.NaN;
		    }
		   
		    return ((int)(value * 10000) / 100.0);
		} catch (MalformedObjectNameException | NullPointerException | InstanceNotFoundException | ReflectionException e) {
			e.printStackTrace();
			return Double.NaN;
		}
	}
}
