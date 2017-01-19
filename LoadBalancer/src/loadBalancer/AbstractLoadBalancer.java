package loadBalancer;

import java.net.InetSocketAddress;
import java.util.Set;

import commsModel.RemoteLoadBalancer;
import commsModel.Server;

/**
 * @author Joachim
 *         <p>
 *         Abstract base class for active and passive load balancer processes.
 *         Contains shared properties for both subclasses.
 *         </p>
 *
 */
public abstract class AbstractLoadBalancer implements Runnable {

	/**
	 * The port that this load balancer will listen for incoming TCP connections on. 
	 */
	protected int acceptPort;

	
	/**
	 * A set of all other load balancer nodes in the system.
	 */
	protected Set<RemoteLoadBalancer> remoteLoadBalancers;

	
	/**
	 * A set of the back end servers in the system.
	 */
	protected Set<Server> servers;

	
	/**
	 * The address of the name resolution service. 
	 */
	protected InetSocketAddress nameServiceAddress;
}
