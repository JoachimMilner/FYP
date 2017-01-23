package faultModule;

import java.util.Set;

import commsModel.RemoteLoadBalancer;
import loadBalancer.ActiveLoadBalancer;

/**
 * @author Joachim
 *         <p>
 *         Runnable class to be used by an {@link ActiveLoadBalancer} to
 *         periodically broadcast an <code>ALIVE_CONFIRM</code> message to all
 *         other (passive) load balancer nodes.
 *         </p>
 *
 */
public class HeartbeatBroadcaster implements Runnable {
	
	/**
	 * A set containing all other (passive) load balancer nodes that exist
	 * in the system. 
	 */
	private Set<RemoteLoadBalancer> remoteLoadBalancers;
	
	/**
	 * The interval, in seconds, that this HeartbeatBroadcaster will send <code>ALIVE_CONFIRM</code>
	 * messages at.
	 */
	private int heartbeatIntervalSecs;

	/**
	 * Creates a new HeartbeatBroadcaster instance that, when started in a
	 * thread, will periodically send <code>ALIVE_CONFIRM</code> messages to all
	 * remote load balancers in the <code>remoteLoadBalancers</code> set, at the
	 * specified <code>hearbeatIntervalSecs</code>.
	 */
	public HeartbeatBroadcaster(Set<RemoteLoadBalancer> remoteLoadBalancers, int heartbeatIntervalSecs) {
		if (remoteLoadBalancers == null || remoteLoadBalancers.isEmpty())
			throw new IllegalArgumentException("Remote load balancer set cannot be null or empty.");
		if (heartbeatIntervalSecs < 1)
			throw new IllegalArgumentException("Heartbeat interval must be at least 1 second.");
		
		this.remoteLoadBalancers = remoteLoadBalancers;
		this.heartbeatIntervalSecs = heartbeatIntervalSecs;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

}
