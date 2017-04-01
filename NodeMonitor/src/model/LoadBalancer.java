package model;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import logging.LogMessageType;

/**
 * @author Joachim
 * <p>Object used to represent LoadBalancer nodes in the system. </p>
 *
 */
public class LoadBalancer extends AbstractRemoteSystemComponent {
	
	/**
	 * The current state of this remote load balancer.
	 */
	private LoadBalancerState state;
	
	/**
	 * Constructs a new LoadBalancer instance representing a
	 * LoadBalancer in the system, with the specified unique ID and
	 * address.
	 * 
	 * @param componentID
	 *            the unique ID of this LoadBalancer
	 * @param remoteAddress
	 *            the remote address of this LoadBalancer
	 */
	public LoadBalancer(int componentID, InetSocketAddress remoteAddress) {
		this.componentID = componentID;
		this.remoteAddress = remoteAddress;
	}
	
	
	/**
	 * @return The current state of this remote load balancer.
	 */
	public LoadBalancerState getState() {
		return state;
	}


	/**
	 * @param state The current state of this remote load balancer.
	 */
	public void setState(LoadBalancerState state) {
		this.state = state;
	}
	
	/**
	 * Used to synchronously start multiple active force-started load balancers from the node monitor.
	 */
	public void sendActiveReleaseMessage() {
		ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.put((byte) LogMessageType.ACTIVE_RELEASE_NOTIFY.getValue());
		buffer.flip();
		try {
			while (buffer.hasRemaining()) {
				socketChannel.write(buffer);
			}	
		} catch (IOException e) {
			
		}
	}
		


	/**
	 * @author Joachim
	 *         <p>
	 * 		Enum used to represent the state of a load balance node.
	 *         </p>
	 *
	 */
	public enum LoadBalancerState {

		ACTIVE, PASSIVE

	}
}
