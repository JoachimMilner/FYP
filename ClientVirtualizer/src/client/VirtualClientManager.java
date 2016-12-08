package client;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import connectionUtils.ConnectableComponent;

/**
 * @author Joachim</br>
 *         <p>
 * 		This class spawns and handles a specified number of
 *         {@link RunnableClientProcess} instances.
 *         </p>
 */
public class VirtualClientManager implements ConnectableComponent {

	/**
	 * The number of virtual clients that this VirtualClientManager will attempt
	 * to start.
	 */
	private int numberOfClients;

	/**
	 * The number of virtual clients that this VirtualClientManager has started.
	 */
	private int numberOfLiveClients = 0;

	/**
	 * The minimum message sending frequency to be allocated to a client in
	 * milliseconds.
	 */
	private int minSendFrequencyMs;

	/**
	 * The maximum message sending frequency to be allocated to a client in
	 * milliseconds.
	 */
	private int maxSendFrequencyMs;

	/**
	 * The number of requests sent by all virtual clients.
	 */
	private int totalRequestsSent = 0;
	
	/**
	 * The number of server responses received by all virtual clients.
	 */
	private int totalResponsesReceived = 0;

	/**
	 * Selector used to monitor the read-ready state of the clientSocket.
	 */
	//private SelectionKey clientSocketSelectionKey;

	/**
	 * A list of the <code>RunnableClientProcess</code> instances that this
	 * VirtualClientManager has instantiated.
	 */
	private ArrayList<RunnableClientProcess> clients = new ArrayList<>();;

	/**
	 * ExecutorService for starting the pool of client threads.
	 */
	private ExecutorService clientThreadExecutor;

	
	/**
	 * Creates a VirtualClientManager with encapsulated functionality for
	 * initialising and handling a collection of {@link RunnableClientProcess}
	 * instances. Each client will be initialised with a random request sending
	 * frequency within the range of <code>minSendFrequencyMs</code> and
	 * <code>maxSendFrequencyMs</code>.
	 * 
	 * @param numberOfClients
	 *            the number of clients to be spawned.
	 * @param minSendFrequencyMs
	 *            the minimum message sending frequency to be allocated to a
	 *            client in milliseconds.
	 * @param maxSendFrequencyMs
	 *            the maximum message sending frequency to be allocated to a
	 *            client in milliseconds.
	 * @throws IllegalArgumentException
	 *             if the constructor is called with numberOfClients set to less
	 *             than 1.
	 */
	public VirtualClientManager(int numberOfClients, int minSendFrequencyMs, int maxSendFrequencyMs) {
		if (numberOfClients < 1)
			throw new IllegalArgumentException("numberOfClients must be at least 1.");
		if (minSendFrequencyMs > maxSendFrequencyMs)
			throw new IllegalArgumentException(
					"Minimum message sending frequency must be less than or equal to maximum frequency.");

		this.numberOfClients = numberOfClients;
		this.minSendFrequencyMs = minSendFrequencyMs;
		this.maxSendFrequencyMs = maxSendFrequencyMs;
	}

	
	/**
	 * @return the number of {@link RunnableClientProcess} instances that this
	 *         object contains.
	 */
	public int getNumberOfClients() {
		return numberOfClients;
	}

	
	/**
	 * @return the number of {@link RunnableClientProcess} threads that this
	 *         object has started.
	 */
	public int getNumberOfLiveClients() {
		return numberOfLiveClients;
	}
	

	/**
	 * @return the total number of requests that have been sent by all
	 *         {@link RunnableClientProcess} threads.
	 */
	public int getTotalRequestsSent() {
		return totalRequestsSent;
	}
	
	
	/**
	 * Each {@link RunnableClientProcess} calls this method when they send a server request.
	 * Synchronized for thread safety.
	 */
	public synchronized void incrementTotalRequestsSent() {
		totalRequestsSent++;
		
		//System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
		for (int i = 0; i < 20; i++) {
			System.out.println(" ");
		}
		System.out.print("Total Messages Sent: " + totalRequestsSent + " Received: " + totalResponsesReceived);
	}
	

	/**
	 * @return the total number of responses that have been received on the
	 *         <code>SocketChannel</code>.
	 */
	public int getTotalResponsesReceived() {
		return totalResponsesReceived;

	}
	
	
	/**
	 * Each {@link RunnableClientProcess} calls this method when they receive a message from the server.
	 * Synchronized for thread safety.
	 */
	public synchronized void incrementTotalResponsesReceived() {
		totalResponsesReceived++;
		
		//System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
		for (int i = 0; i < 20; i++) {
			System.out.println(" ");
		}
		System.out.print("Total Messages Sent: " + totalRequestsSent + " Received: " + totalResponsesReceived);
	}

	
	/**
	 * Creates a number of thread specified by <code>numberOfClients</code> and
	 * starts them using an {@link ExecutorService}. This method can only be
	 * called once after the VirtualClientManager has been created.
	 */
	public void initialiseClientPool() {
		System.out.println("Initialising Virtual Client Pool...");
		
		clientThreadExecutor = Executors.newFixedThreadPool(numberOfClients);
		for (int i = 0; i < numberOfClients; i++) {
			int messageSendFrequencyMs = ThreadLocalRandom.current().nextInt(minSendFrequencyMs,
					maxSendFrequencyMs + 1);
			RunnableClientProcess newClient = new RunnableClientProcess(new InetSocketAddress("localhost", 8000), this, messageSendFrequencyMs);
			clients.add(newClient);
			clientThreadExecutor.execute(newClient);
			numberOfLiveClients++;
		}
		clientThreadExecutor.shutdown();
	}
}
