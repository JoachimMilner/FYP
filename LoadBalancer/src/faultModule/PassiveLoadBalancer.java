package faultModule;

import java.net.InetSocketAddress;
import java.util.Set;

import commsModel.RemoteLoadBalancer;
import commsModel.Server;
import loadBalancer.AbstractLoadBalancer;
import logging.ComponentLogger;
import logging.LogMessageType;

/**
 * @author Joachim
 *         <p>
 *         The main class for running a passive load balancer. Implements the
 *         {@link Runnable} interface, and when started in a new threads, begins
 *         running backup responsibilities:
 *         <ul>
 *         <li>Monitors the heartbeat of the active load balancer and
 *         initialises the recovery protocol in the case that failure is
 *         detected.</li>
 *         <li>Periodically coordinates pre-election with other passive nodes
 *         and determines the suitability ranking for all members.</li>
 *         </ul>
 *         </p>
 *
 */
public class PassiveLoadBalancer extends AbstractLoadBalancer implements Runnable {

	/**
	 * The default timeout duration that this object will use when monitoring
	 * the active load balancer's heartbeat.
	 */
	private int defaultTimeoutSecs;

	/**
	 * Creates a new PassiveLoadBalancer object that acts as a backup load
	 * balancer process in the system.
	 * 
	 * @param acceptPort
	 *            the port on which to accept incoming connection requests
	 * @param remoteLoadBalancers
	 *            the set of remote load balancers in the system
	 * @param servers
	 *            the set of all backend servers in the system
	 * @param nameServiceAddress
	 *            the address of the name service
	 * @param defaultTimeoutSecs
	 *            the default time duration when monitoring the active load
	 *            balancer's heartbeat.
	 */
	public PassiveLoadBalancer(int acceptPort, Set<RemoteLoadBalancer> remoteLoadBalancers, Set<Server> servers,
			InetSocketAddress nameServiceAddress, int defaultTimeoutSecs) {
		if (remoteLoadBalancers == null || remoteLoadBalancers.isEmpty())
			throw new IllegalArgumentException("Remote load balancer set cannot be null or empty.");
		if (servers == null || servers.isEmpty())
			throw new IllegalArgumentException("Servers set cannot be null or empty.");
		if (nameServiceAddress == null)
			throw new IllegalArgumentException("Name service address cannot be null.");
		if (defaultTimeoutSecs < 1)
			throw new IllegalArgumentException("Default timeout value must be a positive, non-zero value.");

		this.acceptPort = acceptPort;
		this.remoteLoadBalancers = remoteLoadBalancers;
		this.servers = servers;
		this.nameServiceAddress = nameServiceAddress;
		this.defaultTimeoutSecs = defaultTimeoutSecs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run() Starts a new ElectionManager that will
	 * periodically coordinate elections with other passive nodes in the system
	 * and then begins monitoring the state of the active load balancer.
	 */
	@Override
	public void run() {
		System.out.println("Initialising passive load balancer service on port " + acceptPort + "...");
		ComponentLogger.getInstance().log(LogMessageType.LOAD_BALANCER_ENTERED_PASSIVE);
		

	}
	
	@Override
	protected void startLoadBalancerMessageListener(Thread loadBalancerThread) {
		
	}

}
