package loadBalancer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import commsModel.RemoteLoadBalancer;
import commsModel.Server;
import connectionUtils.ConnectNIO;
import connectionUtils.MessageType;

/**
 * @author Joachim
 *         <p>
 *         The main active load balancer class. This object implements the
 *         {@link Runnable} interface and when started in a new thread, begins
 *         handling incoming client requests and distributing server tokens
 *         (i.e. providing clients with a suitable server to connect to).
 *         Contains a {@link ServerManager} that is used to monitor the status
 *         of the live servers and retrieve a server's details when necessary.
 *         </p>
 *
 */
public class ActiveLoadBalancer extends AbstractLoadBalancer {

	/**
	 * Creates a new ActiveLoadBalancer object that acts as the primary load balancer process in the system.
	 * The <code>run</code> method prompts this object to start listening to requests and 
	 * @param acceptPort
	 * @param remoteLoadBalancers
	 * @param servers
	 * @param nameServiceAddress
	 */
	public ActiveLoadBalancer(int acceptPort, Set<RemoteLoadBalancer> remoteLoadBalancers, Set<Server> servers,
			InetSocketAddress nameServiceAddress) {
		if (remoteLoadBalancers == null || remoteLoadBalancers.isEmpty())
			throw new IllegalArgumentException("Remote load balancer set cannot be null or empty.");
		if (servers == null || servers.isEmpty())
			throw new IllegalArgumentException("Servers set cannot be null or empty.");
		if (nameServiceAddress == null)
			throw new IllegalArgumentException("Name service address cannot be null.");
		
		this.acceptPort = acceptPort;
		this.remoteLoadBalancers = remoteLoadBalancers;
		this.servers = servers;
		this.nameServiceAddress = nameServiceAddress;
	}

	/*
	 * (non-Javadoc)
	 * Called on <code>Thread.start()</code> in order to initialise a new cached thread pool that delegates
	 * incoming connections to a new {@RunnableRequestProcessor}.
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		System.out.println("Initialising active load balancer service on port " + acceptPort + "...");
		
		notifyNameService();
		
		ServerSocketChannel serverSocketChannel = ConnectNIO.getServerSocketChannel(acceptPort);
		ExecutorService threadPoolExecutor = Executors.newCachedThreadPool();
		ServerManager serverManager = new ServerManager(servers);
		Thread serverManagerThread = new Thread(serverManager);
		serverManagerThread.start();
		
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
				System.out.println("Received connection request.");
				threadPoolExecutor.execute(new RunnableActiveRequestProcessor(connectRequestSocket, this, serverManager));
			}
		}
		System.out.println("Active load balancer shutting down...");
		
		serverManagerThread.interrupt();
		try {
			serverSocketChannel.close();
		} catch (IOException e) {
		}
		threadPoolExecutor.shutdown();
	}
	
	/**
	 * Opens a {@link SocketChannel} and sends a <code>HOST_ADDR_NOTIFY</code> message
	 * to the address that is stored for the name service, alerting the service that this
	 * process is acting as the active load balancer. 
	 */
	private void notifyNameService() {
		System.out.println("Sending host address notification message to name service...");
		SocketChannel socketChannel = ConnectNIO.getBlockingSocketChannel(nameServiceAddress);
		ByteBuffer buffer = ByteBuffer.allocate(5);
		buffer.put((byte) MessageType.HOST_ADDR_NOTIFY.getValue());
		buffer.putInt(acceptPort);
		buffer.flip();
		try {
			while (buffer.hasRemaining()) {
				socketChannel.write(buffer);
			}
			System.out.println("Notified name service.");
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
