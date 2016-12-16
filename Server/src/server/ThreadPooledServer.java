package server;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import connectionUtils.*;

/**
 * @author Joachim</br>
 *         This class is used to initialise and manage a thread pool of
 *         {@link RunnableRequestProcessor} instances. Exactly one
 *         ThreadPooledServer should be instantiated and started in a thread
 *         when the application starts.
 */
public class ThreadPooledServer implements ConnectableComponent, Runnable {

	/**
	 * The size of the thread pool to be created for handling received client
	 * requests.
	 */
	private int threadPoolSize;

	/**
	 * The port to listen for incoming client requests on.
	 */
	private int connectPort;

	/**
	 * The ServerSocketChannel that this ThreadPooledServer will use to accept
	 * incoming client connections.
	 */
	// private ServerSocketChannel serverSocketChannel;

	/**
	 * The number of requests that have been received been received by all
	 * server processing threads.
	 */
	private int totalRequestsReceived = 0;

	/**
	 * The number of responses that have been sent by all server processing
	 * threads.
	 * 
	 */
	private int totalResponsesSent = 0;

	/**
	 * Creates a new ThreadPooledServer instance that will create and start a
	 * pool of {@link RunnableRequestProcessor} threads once started. This
	 * object must be passed to the {@link Thread} constructor so that its
	 * <code>run</code> method can be called.
	 * 
	 * @param threadPoolSize
	 *            the size of the thread pool that this ThreadPooledServer will
	 *            create.
	 */
	public ThreadPooledServer(int threadPoolSize, int connectPort) {
		if (threadPoolSize < 1)
			throw new IllegalArgumentException("Thread pool size must be at least 1.");

		this.threadPoolSize = threadPoolSize;
		this.connectPort = connectPort;
	}

	/*
	 * (non-Javadoc) To be called on <code>Thread.start()</code> to create the
	 * thread pool and begin listening for incoming connection requests.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		System.out.println("Initialising Server Thread Pool...");
		ServerSocketChannel serverSocketChannel = ConnectNIO.getServerSocketChannel(connectPort);
		ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(threadPoolSize);
		while (!Thread.currentThread().isInterrupted()) {
			SocketChannel connectRequestSocket = null;
			try {
				connectRequestSocket = serverSocketChannel.accept();
			} catch (IOException e) {
				if (Thread.currentThread().isInterrupted()) {
					break;
				}
				e.printStackTrace();
			}
			if (connectRequestSocket != null) {
				System.out.println("Server received connection request.");
				threadPoolExecutor.execute(new RunnableRequestProcessor(connectRequestSocket, this));
			}
		}
		System.out.println("Server shutting down...");
		try {
			serverSocketChannel.close();
		} catch (IOException e) {
		}
		threadPoolExecutor.shutdown();
	}

	/**
	 * @return The total number of requests that have been received by all
	 *         server threads.
	 */
	public int getTotalRequestsReceived() {
		return totalRequestsReceived;
	}

	/**
	 * Each {@link RunnableRequestProcessor} instance calls this method whenever
	 * they receive a client request. Synchronized for thread safety.
	 */
	public synchronized void incrementTotalRequestsReceived() {
		totalRequestsReceived++;
	}

	/**
	 * @return The total number of responses that have been sent by all server
	 *         threads.
	 */
	public int getTotalResponsesSent() {
		return totalResponsesSent;
	}

	/**
	 * Each {@link RunnableRequestProcessor} instance calls this method whenever
	 * they send a response to a client. Synchronized for thread safety.
	 */
	public synchronized void incrementTotalResponsesSent() {
		totalResponsesSent++;
	}
}
