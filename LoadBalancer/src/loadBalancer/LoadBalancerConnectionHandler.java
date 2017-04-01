package loadBalancer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import commsModel.LoadBalancerState;
import commsModel.RemoteLoadBalancer;
import connectionUtils.ConnectNIO;

public class LoadBalancerConnectionHandler implements Runnable {
	
	/**
	 * The port that this load balancer process will listen for and accept
	 * incoming connection requests.
	 */
	private int acceptPort; 
	
	/**
	 * The state of this load balancer - used to determine whether
	 * this connection handler will check for client connection requests
	 * or only remote load balancer connection requests. Always initially
	 * passive.
	 */
	private LoadBalancerState state = LoadBalancerState.PASSIVE;
	
	/**
	 * The set representing other load balancers in the system.
	 */
	private Set<RemoteLoadBalancer> remoteLoadBalancers;
	
	/**
	 * The ServerManager used when this load balancer is in the active state.
	 */
	private ServerManager serverManager;
	
	public LoadBalancerConnectionHandler(int acceptPort, Set<RemoteLoadBalancer> remoteLoadBalancers) {
		if (remoteLoadBalancers == null || remoteLoadBalancers.isEmpty())
			throw new IllegalArgumentException("Remote load balancer set cannot be null or empty.");
		
		this.acceptPort = acceptPort;
		this.remoteLoadBalancers = remoteLoadBalancers;
	}
	
	@Override
	public void run() {
		System.out.println("Accepting connections on port: " + acceptPort);
		ServerSocketChannel serverSocketChannel = ConnectNIO.getServerSocketChannel(acceptPort);
		int connectionPrecedence = 0;
		try {
			connectionPrecedence = Integer.parseInt(InetAddress.getLocalHost().getHostAddress().split("\\.")[3]);
		} catch (NumberFormatException | UnknownHostException e1) {
		}
		
		// Initially attempt to form a connection to all load balancers
		for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
			remoteLoadBalancer.connect(0);
		}
		
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
				System.out.println("Received connection request.");
				boolean isLoadBalancerNode = false;
				String connectingIP = connectRequestSocket.socket().getInetAddress().getHostAddress();
				for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
					if (remoteLoadBalancer.getAddress().getAddress().getHostAddress().equals(connectingIP)) {
						isLoadBalancerNode = true;
						int remotePrecedence = 0;
						try {
							remotePrecedence = Integer.parseInt(connectingIP.split("\\.")[3]);
						} catch (NumberFormatException e) {
						}
						if (!remoteLoadBalancer.isConnected() || remotePrecedence > connectionPrecedence) {
							try {
								connectRequestSocket.configureBlocking(false);
							} catch (IOException e) {							
							}
							remoteLoadBalancer.setSocketChannel(connectRequestSocket);
						}
						break;
					}
				}
				if (state.equals(LoadBalancerState.ACTIVE) && !isLoadBalancerNode) {
					threadPoolExecutor
							.execute(new RunnableClientRequestProcessor(connectRequestSocket, serverManager));
				}
			}
		}
		
		try {
			serverSocketChannel.close();
		} catch (IOException e) {
		}
		threadPoolExecutor.shutdown();
	}
	
	/**
	 * @return the port that this LoadBalancerConnectionHandler is accepting connection
	 * requests on
	 */
	public int getAcceptPort() {
		return acceptPort;
	}
	
	/**
	 * Sets this LoadBalancerConnectionHandler in the active state, prompting it to handle
	 * client requests. 
	 * @param serverManager the ServerManager that this LoadBalancerConnectionHandler will 
	 * use to handle server token requests
	 */
	public void setActive(ServerManager serverManager) {
		state = LoadBalancerState.ACTIVE;
		this.serverManager = serverManager;
	}
	
	/**
	 * Sets this LoadBalancerConnectionHandler in the passive state, prompting it to only
	 * handle incoming connection requests from other load balancer nodes. 
	 */
	public void setPassive() {
		state = LoadBalancerState.PASSIVE;
	}
}
