package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import logging.ComponentLogger;
import logging.LogMessageType;

/**
 * @author Joachim
 *         <p>
 *         This class spawns and handles a specified number of
 *         {@link RunnableClientProcess} instances.
 *         </p>
 */
public class VirtualClientManager {

	/**
	 * The maximum number of virtual clients that this VirtualClientManager will
	 * maintain at any time.
	 */
	private AtomicInteger maxClients = new AtomicInteger();

	/**
	 * The number of virtual clients that this VirtualClientManager has started.
	 */
	private AtomicInteger numberOfLiveClients = new AtomicInteger(0);

	/**
	 * The minimum message sending frequency to be allocated to a client in
	 * milliseconds.
	 */
	private AtomicInteger minSendFrequencyMs = new AtomicInteger();

	/**
	 * The maximum message sending frequency to be allocated to a client in
	 * milliseconds.
	 */
	private AtomicInteger maxSendFrequencyMs = new AtomicInteger();

	/**
	 * The minimum number of requests that a virtual client created by this
	 * VirtualClientManagerwill send.
	 */
	private AtomicInteger minClientRequests = new AtomicInteger();

	/**
	 * The maximum number of requests that a virtual client created by this
	 * VirtualClientManagerwill send.
	 */
	private AtomicInteger maxClientRequests = new AtomicInteger();

	/**
	 * The number of requests sent by all virtual clients.
	 */
	private AtomicInteger totalRequestsSent = new AtomicInteger(0);

	/**
	 * The number of server responses received by all virtual clients.
	 */
	private AtomicInteger totalResponsesReceived = new AtomicInteger(0);

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

		this.maxClients.set(maxClients);
		this.minSendFrequencyMs.set(minSendFrequencyMs);
		this.maxSendFrequencyMs.set(maxSendFrequencyMs);
		this.minClientRequests.set(minClientRequests);
		this.maxClientRequests.set(maxClientRequests);
		this.nameServiceAddress = nameServiceAddress;
	}

	/**
	 * @return the number of {@link RunnableClientProcess} instances that this
	 *         object contains.
	 */
	public int getNumberOfClients() {
		return maxClients.get();
	}

	/**
	 * @return the number of {@link RunnableClientProcess} threads that this
	 *         object has started.
	 */
	public int getNumberOfLiveClients() {
		return numberOfLiveClients.get();
	}

	/**
	 * @return the total number of requests that have been sent by all
	 *         {@link RunnableClientProcess} threads.
	 */
	public int getTotalRequestsSent() {
		return totalRequestsSent.get();
	}

	/**
	 * Each {@link RunnableClientProcess} calls this method when they send a
	 * server request.
	 */
	public void incrementTotalRequestsSent() {
		totalRequestsSent.incrementAndGet();
	}

	/**
	 * @return the total number of responses that have been received on the
	 *         <code>SocketChannel</code>.
	 */
	public int getTotalResponsesReceived() {
		return totalResponsesReceived.get();

	}

	/**
	 * Each {@link RunnableClientProcess} calls this method when they receive a
	 * message from the server.
	 */
	public void incrementTotalResponsesReceived() {
		totalResponsesReceived.incrementAndGet();
	}

	/**
	 * Each {@link RunnableClientProcess} calls this method when they have sent
	 * the total number of messages assigned to them. Decrements the number of
	 * live clients so we can monitor the thread pool size.
	 */
	public void notifyThreadFinished() {
		numberOfLiveClients.decrementAndGet();
	}

	/**
	 * Creates a number of thread specified by <code>numberOfClients</code> and
	 * starts them using an {@link ExecutorService}. This method can only be
	 * called once after the VirtualClientManager has been created.
	 */
	public void initialiseClientPool() {
		System.out.println("Initialising Virtual Client Pool...");

		clientThreadExecutor = Executors.newCachedThreadPool();
		for (int i = 0; i < maxClients.get(); i++) {
			createNewClientThread();
		}
		startClientMonitor();
	}

	/**
	 * Starts a new thread that will monitor the size of the virtual client pool
	 * at a rate of half of the minimum message sending frequency. If the size
	 * of the pool falls below the max, a new virtual client will be created
	 * iteratively, up to the maximum.
	 * 
	 */
	private void startClientMonitor() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (!clientMonitorThreadStopped) {
					if (numberOfLiveClients.get() < maxClients.get()) {
						createNewClientThread();
					}
					//System.out.println(numberOfLiveClients.get());
					try {
						Thread.sleep(minSendFrequencyMs.get() / 2);
					} catch (InterruptedException e) {
						if (clientMonitorThreadStopped) {
							break;
						}
						startClientMonitor();
					}
					/*
					 * for (int i = 0; i < 20; i++) { System.out.println(" "); }
					 * System.out.println("Total Live Virtual Clients: " +
					 * numberOfLiveClients);
					 * System.out.println("Total Messages Sent: " +
					 * totalRequestsSent);
					 * System.out.println("Total Messages Received: " +
					 * totalResponsesReceived);
					 */
				}
			}
		}).start();
	}

	/**
	 * Generates random message sending frequency and message count values using
	 * the ranges provided, then instantiates and starts a new
	 * {@link RunnableClientProcess}.
	 */
	private void createNewClientThread() {
		int messageSendFrequencyMs = ThreadLocalRandom.current().nextInt(minSendFrequencyMs.get(), maxSendFrequencyMs.get() + 1);
		int totalRequestsToSend = ThreadLocalRandom.current().nextInt(minClientRequests.get(), maxClientRequests.get() + 1);
		RunnableClientProcess newClient = new RunnableClientProcess(nameServiceAddress, this, messageSendFrequencyMs,
				totalRequestsToSend);
		clientThreadExecutor.execute(newClient);
		numberOfLiveClients.incrementAndGet();
	}

	/**
	 * Stops the thread that is running this VirtualClientManager.
	 */
	public void stop() {
		clientMonitorThreadStopped = true;
	}

	/**
	 * This method allows the NodeMonitor to remotely modify the virtual client
	 * settings.
	 * 
	 * @param nodeMonitorSocketChannel
	 *            the SocketChannel that is connected to the NodeMonitor, this
	 *            should be retrieved from the {@link ComponentLogger} after
	 *            this ClientVirtualizer process has been registered.
	 */
	public void listenForConfigurationUpdates(SocketChannel nodeMonitorSocketChannel) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (!clientMonitorThreadStopped) {
					ByteBuffer buffer = ByteBuffer.allocate(21);
					try {
						while (nodeMonitorSocketChannel.read(buffer) > 0) {
							buffer.flip();
							LogMessageType messageType = LogMessageType.values()[buffer.get()];
							if (messageType.equals(LogMessageType.CLIENT_UPDATE_SETTINGS)) {
								maxClients.set(buffer.getInt());
								minSendFrequencyMs.set(buffer.getInt());
								maxSendFrequencyMs.set(buffer.getInt());
								minClientRequests.set(buffer.getInt());
								maxClientRequests.set(buffer.getInt());
								System.out.println(
										"Received client configuration update from NodeMonitor. New client settings:");
								System.out.println("Max Clients: " + maxClients + " Min Send Frequency: "
										+ minSendFrequencyMs + "ms Max Send Frequeny: " + maxSendFrequencyMs
										+ "ms Min Client Requests: " + minClientRequests + " Max Client Requests: "
										+ maxClientRequests);
							}
						}
					} catch (IOException e) {
						// e.printStackTrace();
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
			}

		}).start();
	}
}
