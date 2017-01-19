package nameService;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import connectionUtils.ConnectNIO;

/**
 * @author Joachim
 *         <p>
 *         This class represents a mocked DNS service. When run in a new thread,
 *         it will act as a thread pooled server that accepts 2 kinds of
 *         request. Either an address notification from a load balancer,
 *         indicating that the remote process has become the primary server for
 *         clients to connect to, or a request from a client for the current
 *         primary's address.
 *         </p>
 *
 */
public class AddressResolutionService {

	/**
	 * The port that this AddressResolutionService will listen for incoming
	 * connection requests on.
	 */
	private int acceptPort;

	
	/**
	 * The remote address that will be updated by the active load balancer and
	 * requested by clients.
	 */
	private String hostAddress;

	
	/**
	 * The remote port that will be updated by the active load balancer and
	 * requested by clients.
	 */
	private int hostPort;

	
	/**
	 * Creates a new AddressResolutionService with the specified port to accept
	 * connections on.
	 * 
	 * @param acceptPort
	 */
	public AddressResolutionService(int acceptPort) {
		this.acceptPort = acceptPort;
	}

	
	/**
	 * @return the current remote address that is held for the load balancer.
	 */
	public String getHostAddress() {
		return hostAddress;
	}

	
	/**
	 * @param hostAddress
	 *            - updated remote address to be set as the primary load
	 *            balancer.
	 */
	public synchronized void setHostAddress(String hostAddress) {
		this.hostAddress = hostAddress;
	}

	
	/**
	 * @return the current remote port that is held for the load balancer.
	 */
	public int getHostPort() {
		return hostPort;
	}

	
	/**
	 * @param hostPort
	 *            - updated remote port to be set as the primary load balancer
	 */
	public void setHostPort(int hostPort) {
		this.hostPort = hostPort;
	}

	
	/**
	 * Starts listening for incoming connection requests and delegates each to a
	 * {@link RunnableRequestProcessor}. Expects to receive either a
	 * <code>HOST_ADDR_NOTIFY</code> or a <code>HOST_ADDR_REQUEST</code>.
	 */
	public void startService() {
		System.out.println("Initialising Address Resolution Service...");
		ServerSocketChannel serverSocketChannel = ConnectNIO.getServerSocketChannel(acceptPort);
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
		System.out.println("Address Resolution Service shutting down...");
		try {
			serverSocketChannel.close();
		} catch (IOException e) {
		}
		threadPoolExecutor.shutdown();
	}
}
