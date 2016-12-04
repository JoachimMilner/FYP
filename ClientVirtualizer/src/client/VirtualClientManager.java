package client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import connectionUtils.ConnectableComponent;
import connectionUtils.MessageType;

/**
 * @author Joachim
 * <p>This class spawns and handles a specified number of {@link RunnableClientProcess} instances.</p>
 */
public class VirtualClientManager implements ConnectableComponent {
	
	/**
	 * The number of virtual clients that this VirtualClientManager will attempt to start.
	 */
	private int numberOfClients;
	
	/**
	 * The number of virtual clients that this VirtualClientManager has started.
	 */
	private int numberOfLiveClients = 0;
	
	/**
	 * The minimum message sending frequency to be allocated to a client in milliseconds.
	 */
	private int minSendFrequencyMs;
	
	/**
	 * The maximum message sending frequency to be allocated to a client in milliseconds.
	 */
	private int maxSendFrequencyMs;
	
	/**
	 * The number of server responses received on the <code>SocketChannel</code>. i.e. the total from all virtual clients
	 */
	private int responsesReceived = 0;
	
	/**
	 * Selector used to monitor the read-ready state of the clientSocket.
	 */
	private SelectionKey clientSocketSelectionKey;
	
	/**
	 * Channel for sending client requests. The SocketChannel is passed to each virtual client and its connected state is polled periodically
	 * by this VirtualClientManager.
	 */
	private SocketChannel clientSocketChannel;
	
	/**
	 * A list of the <code>RunnableClientProcess</code> instances that this VirtualClientManager has instantiated.
	 */
	private ArrayList<RunnableClientProcess> clients = new ArrayList<>();;
	
	/**
	 * ExecutorService for starting the pool of client threads.
	 */
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
	 * @return the total number of requests that have been sent by all {@link RunnableClientProcess} threads.
	 */
	public int getTotalRequestsSent() {
		int requestsSent = 0;
		for (RunnableClientProcess client : clients) {
			requestsSent += client.getRequestsSent();
		}
		return requestsSent;
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
		clientSocketChannel = getNonBlockingSocketChannel("localhost", 8000);
		clientThreadExecutor = Executors.newFixedThreadPool(numberOfClients);
		for (int i = 0; i < numberOfClients; i++) {
			int messageSendFrequencyMs = ThreadLocalRandom.current().nextInt(minSendFrequencyMs, maxSendFrequencyMs + 1);
			RunnableClientProcess newClient = new RunnableClientProcess(clientSocketChannel, messageSendFrequencyMs);
			clients.add(newClient);
			clientThreadExecutor.execute(newClient);
			numberOfLiveClients++;
		}
		startListening();
		clientThreadExecutor.shutdown();
	}

	
	/**
	 * Starts a new thread that will periodically check the <code>clientSocketChannel</code> for server replies.
	 * Uses a {@link SelectionKey} to determine when the channel is ready to be read.
	 */
	private void startListening() {
		try {
			Selector clientSocketSelector = Selector.open();
			clientSocketSelectionKey = clientSocketChannel.register(clientSocketSelector, SelectionKey.OP_READ);
		} catch (IOException e) {
			e.printStackTrace();
		}
		new Thread(new Runnable() {
		    public void run() {
		    	ByteBuffer buffer = ByteBuffer.allocate(81);
				while(!Thread.currentThread().isInterrupted()) {
					try {
						System.out.println(clientSocketChannel.read(buffer));
						if (clientSocketSelectionKey.isReadable()) {
							System.out.println("test");
							clientSocketChannel.read(buffer);
							buffer.flip();
							MessageType messageType = MessageType.values()[buffer.get()];
							if (messageType.equals(MessageType.SERVER_RESPONSE)) {
								responsesReceived++;
								buffer.clear();
							}
						}
						Thread.sleep(5);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
		    }
		}).start();
	}
}
