package loadBalancer;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
	 * The process that handles incoming connection requests for this load balancer.
	 * Passed between the active and passive states during program operation.
	 */
	protected LoadBalancerConnectionHandler connectionHandler;

	/**
	 * A set of all other load balancer nodes in the system.
	 */
	protected Set<RemoteLoadBalancer> remoteLoadBalancers;

	/**
	 * A set of the back end servers in the system.
	 */
	protected Set<Server> servers;
	
	/**
	 * Flag used to stop the current running load balancer process. 
	 * An atomic boolean is used so the value can be accessed within 
	 * anonymous runnable classes at runtime. 
	 */
	protected final AtomicBoolean terminateThread = new AtomicBoolean(false);
	
	/**
	 * Indefinitely checks for messages from all other load balancer nodes 
	 * in the system.
	 */
	protected abstract void listenForLoadBalancerMessages();
}
