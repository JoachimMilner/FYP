package client;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import connectionUtils.ConnectableComponent;

/**
 * @author Joachim
 * <p>This class spawns and handles a specified number of {@link RunnableClientProcess} instances.</p>
 */
public class VirtualClientManager implements ConnectableComponent {
	
	private int numberOfClients;
	
	private int numberOfLiveClients = 0;
	
	private int minSendFrequencyMs;
	
	private int maxSendFrequencyMs;
	
	private int responsesReceived = 0;
	
	private ArrayList<RunnableClientProcess> clients;
	
	private ExecutorService clientThreadExecutor;
	
	
	/**
	 * Creates a VirtualClientManager with encapsulated functionality for initialising and handling 
	 * a collection of {@link RunnableClientProcess} instances. Each client will be initialised with
	 * a random request sending frequency within the range of <code>minSendFrequencyMs</code> and <code>maxSendFrequencyMs</code>.
	 * @param numberOfClients the number of clients to be spawned.
	 * @param minSendFrequencyMs the minimum message sending frequency to be allocated to a client in milliseconds.
	 * @param maxSendFrequencyMs the maximum message sending frequency to be allocated to a client in milliseconds.
	 * @throws IllegalArgumentException if the constructor is called with numberOfClients set to less than 1.
	 */
	public VirtualClientManager(int numberOfClients, int minSendFrequencyMs, int maxSendFrequencyMs) {
		if (numberOfClients < 1) throw new IllegalArgumentException("numberOfClients must be at least 1.");
		if (minSendFrequencyMs > maxSendFrequencyMs) throw new IllegalArgumentException("Minimum message sending frequency must be less than or equal to maximum frequency.");
		
		this.numberOfClients = numberOfClients;
		this.minSendFrequencyMs = minSendFrequencyMs;
		this.maxSendFrequencyMs = maxSendFrequencyMs;
	}


	/**
	 * @return the number of {@link RunnableClientProcess} instances that this object contains.
	 */
	public int getNumberOfClients() {
		return numberOfClients;
	}	
	
	
	/**
	 * @return the number of {@link RunnableClientProcess} threads that this object has started.
	 */
	public int getNumberOfLiveClients() {
		return numberOfLiveClients;
	}	
	
	
	/**
	 * @return the total number of responses that have been received on the <code>SocketChannel</code>.
	 */
	public int getTotalResponsesReceived() {
		return responsesReceived;
	}
	
	
	/**
	 * Creates a number of thread specified by <code>numberOfClients</code> and starts them using an {@link ExecutorService}.
	 * This method can only be called once after the VirtualClientManager has been created.
	 */
	public void initialiseClientPool() {
		SocketChannel clientSocket = getNonBlockingSocketChannel("localhost", 8000);
		clientThreadExecutor = Executors.newFixedThreadPool(numberOfClients);
		clients = new ArrayList<>();
		for (int i = 0; i < numberOfClients; i++) {
			int messageSendFrequencyMs = ThreadLocalRandom.current().nextInt(minSendFrequencyMs, maxSendFrequencyMs + 1);
			RunnableClientProcess newClient = new RunnableClientProcess(clientSocket, messageSendFrequencyMs);
			clients.add(newClient);
			clientThreadExecutor.execute(newClient);
			numberOfLiveClients++;
		}
		clientThreadExecutor.shutdown();
	}

}
