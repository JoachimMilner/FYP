package loadBalancer;

import java.net.InetSocketAddress;
import java.util.Set;

import commsModel.RemoteLoadBalancer;
import commsModel.Server;

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
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

	}

}
