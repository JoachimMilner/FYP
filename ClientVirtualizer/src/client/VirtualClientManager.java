package client;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import connectionUtils.IConnectableComponent;

/**
 * @author Joachim
 *         <p>
 *         This class spawns and handles a specified number of
 *         {@link RunnableClientProcess} instances.
 *         </p>
 */
public class VirtualClientManager implements IConnectableComponent {

	/**
	 * The maximum number of virtual clients that this VirtualClientManager will
	 * maintain at any time.
	 */
	private int maxClients;
	

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
	 * The minimum number of requests that a virtual client created by this
	 * VirtualClientManagerwill send.
	 */
	private int minClientRequests;

	
	/**
	 * The maximum number of requests that a virtual client created by this
	 * VirtualClientManagerwill send.
	 */
	private int maxClientRequests;

	
	/**
	 * The number of requests sent by all virtual clients.
	 */
	private int totalRequestsSent = 0;

	
	/**
	 * The number of server responses received by all virtual clients.
	 */
	private int totalResponsesReceived = 0;
	
	
	/**
	 * The static address for the name resolution service. 
	 */
	private InetSocketAddress nameServiceAddress;

	
	/**
	 * Flag used to stop this VirtualClientManager.
	 */
	private boolean clientMonitorThreadStopped = false;
	
	
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
	 * @param maxClients
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
	public VirtualClientManager(int maxClients, int minSendFrequencyMs, int maxSendFrequencyMs, int minClientRequests,
			int maxClientRequests, InetSocketAddress nameServiceAddress) {
		if (maxClients < 1)
			throw new IllegalArgumentException("numberOfClients must be at least 1.");
		if (minSendFrequencyMs > maxSendFrequencyMs)
			throw new IllegalArgumentException(
					"Minimum message sending frequency must be less than or equal to maximum frequency.");
		if (minClientRequests > maxClientRequests)
			throw new IllegalArgumentException("Minimum client requests must be less than or equal to the maximum.");
		if (nameServiceAddress == null)
			throw new IllegalArgumentException("Name service address cannot be null.");

		this.maxClients = maxClients;
		this.minSendFrequencyMs = minSendFrequencyMs;
		this.maxSendFrequencyMs = maxSendFrequencyMs;
		this.minClientRequests = minClientRequests;
		this.maxClientRequests = maxClientRequests;
		this.nameServiceAddress = nameServiceAddress;
	}

	
	/**
	 * @return the number of {@link RunnableClientProcess} instances that this
	 *         object contains.
	 */
	public int getNumberOfClients() {
		return maxClients;
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
	 * Each {@link RunnableClientProcess} calls this method when they send a
	 * server request. Synchronized for thread safety.
	 */
	public synchronized void incrementTotalRequestsSent() {
		totalRequestsSent++;
	}

	
	/**
	 * @return the total number of responses that have been received on the
	 *         <code>SocketChannel</code>.
	 */
	public int getTotalResponsesReceived() {
		return totalResponsesReceived;

	}

	
	/**
	 * Each {@link RunnableClientProcess} calls this method when they receive a
	 * message from the server. Synchronized for thread safety.
	 */
	public synchronized void incrementTotalResponsesReceived() {
		totalResponsesReceived++;
	}
	
	
	/**
	 * Each {@link RunnableClientProcess} calls this method when they have sent the total
	 * number of messages assigned to them. Decrements the number of live clients so we can
	 * monitor the thread pool size. Synchronized for thread safety.
	 */
	public synchronized void notifyThreadFinished() {
		numberOfLiveClients--;
	}

	
	/**
	 * Creates a number of thread specified by <code>numberOfClients</code> and
	 * starts them using an {@link ExecutorService}. This method can only be
	 * called once after the VirtualClientManager has been created.
	 */
	public void initialiseClientPool() {
		System.out.println("Initialising Virtual Client Pool...");

		clientThreadExecutor = Executors.newFixedThreadPool(maxClients);
		for (int i = 0; i < maxClients; i++) {
			createNewClientThread();	
		}
		startClientMonitor();
	}
	
	
	/**
	 * Starts a new thread that will monitor the size of the virtual client pool every 250ms.
	 * If the size of the pool falls below the max, a new virtual client will be created
	 * iteratively, up to the maximum.
	 * 
	 */
	private void startClientMonitor() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (!clientMonitorThreadStopped) {
					if (numberOfLiveClients < maxClients) {
						createNewClientThread();
					}
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						if (clientMonitorThreadStopped) {
							break;
						}
						startClientMonitor();
					}
/*					for (int i = 0; i < 20; i++) {
						System.out.println(" ");
					}
					System.out.println("Total Live Virtual Clients: " + numberOfLiveClients);
					System.out.println("Total Messages Sent: " + totalRequestsSent);
					System.out.println("Total Messages Received: " + totalResponsesReceived);*/
				}
			}
		}).start();
	}
	
	
	/**
	 * Generates random message sending frequency and message count values using the ranges provided, then
	 * instantiates and starts a new {@link RunnableClientProcess}.
	 */
	private void createNewClientThread() {
		int messageSendFrequencyMs = ThreadLocalRandom.current().nextInt(minSendFrequencyMs,
				maxSendFrequencyMs + 1);
		int totalRequestsToSend = ThreadLocalRandom.current().nextInt(minClientRequests,
				maxClientRequests + 1);
		RunnableClientProcess newClient = new RunnableClientProcess(nameServiceAddress, this,
				messageSendFrequencyMs, totalRequestsToSend);
		clientThreadExecutor.execute(newClient);
		numberOfLiveClients++;
	}
	
	
	/**
	 * Stops the thread that is running this VirtualClientManager.
	 */
	public void stop() {
		clientMonitorThreadStopped = true;
	}
}
