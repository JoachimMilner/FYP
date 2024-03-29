package server;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;

import connectionUtils.*;

/**
 * @author Joachim
 *         This class is used to initialise and manage a thread pool of
 *         {@link RunnableRequestProcessor} instances. Exactly one
 *         ThreadPooledServer should be instantiated and started in a thread
 *         when the application starts.
 */
public class ThreadPooledServer implements Runnable {

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
	private AtomicInteger totalRequestsReceived = new AtomicInteger(0);

	/**
	 * The number of responses that have been sent by all server processing
	 * threads.
	 * 
	 */
	private AtomicInteger totalResponsesSent = new AtomicInteger(0);
	
	
	/**
	 * The MBeanServer that {@link RunnableRequestProcessor}s can access
	 * in order to retrieve the machine's CPU load. 
	 */
	private MBeanServer mBeanServer;

	
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
	public ThreadPooledServer(int connectPort) {
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
		System.out.println("Initialising Server Thread Pool on Port " + connectPort + "...");
		// Initialise MBeanServer for getting CPU load. 
		mBeanServer = ManagementFactory.getPlatformMBeanServer();

		ServerSocketChannel serverSocketChannel = ConnectNIO.getServerSocketChannel(connectPort);
		ExecutorService threadPoolExecutor = Executors.newCachedThreadPool();
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
		return totalRequestsReceived.get();
	}

	
	/**
	 * Each {@link RunnableRequestProcessor} instance calls this method whenever
	 * they receive a client request.
	 */
	public void incrementTotalRequestsReceived() {
		totalRequestsReceived.incrementAndGet();
	}

	
	/**
	 * @return The total number of responses that have been sent by all server
	 *         threads.
	 */
	public int getTotalResponsesSent() {
		return totalResponsesSent.get();
	}

	
	/**
	 * Each {@link RunnableRequestProcessor} instance calls this method whenever
	 * they send a response to a client.
	 */
	public void incrementTotalResponsesSent() {
		totalResponsesSent.incrementAndGet();
	}
	
	/**
	 * @return the initialised MBeanServer used to access system properties.
	 */
	public MBeanServer getMBeanServer() {
		return mBeanServer;
	}
}
