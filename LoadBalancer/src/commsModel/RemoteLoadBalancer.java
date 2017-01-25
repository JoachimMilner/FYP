package commsModel;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * @author Joachim
 *         <p>
 *         This class represents a remote load balancer node, modelling
 *         communication between primary and backup processes.
 *         </p>
 *
 */
public class RemoteLoadBalancer extends AbstractRemote {

	/**
	 * The active/passive state of the remote load balancer process that this
	 * object represents. That is, whether the remote is the primary load
	 * balancer or a backup.
	 */
	private LoadBalancerState state;

	/**
	 * The election ranking of this remote load balancer process, coordinated by
	 * the {@link ElectionManager} of each load balancer instance. If this is
	 * the active load balancer, the value is set to 0, otherwise it is a
	 * non-zero value.
	 */
	private int electionOrdinality;

	/**
	 * The SocketChannel that is currently held for the connection to this
	 * RemoteLoadBalancer. Used to make communication with this node more
	 * convenient.
	 */
	private SocketChannel socketChannel;

	/**
	 * Creates a new RemoteLoadBalancer object instance that hold relevant
	 * properties and provides an abstraction to the specified remote process.
	 * 
	 * @param loadBalancerAddress
	 *            the remote address of the process that this object represents.
	 * @throws IllegalArgumentException
	 *             if the {@link InetSocketAddress} passed in is null.
	 */
	public RemoteLoadBalancer(InetSocketAddress loadBalancerAddress) {
		if (loadBalancerAddress == null)
			throw new IllegalArgumentException("Remote load balancer address cannot be null.");

		this.address = loadBalancerAddress;
	}

	/**
	 * @return the active/passive state of the remote load balancer process that
	 *         this object represents.
	 */
	public LoadBalancerState getState() {
		return state;
	}

	/**
	 * @param state
	 *            the current state of the remote load balancer process that
	 *            this object represents.
	 */
	public void setState(LoadBalancerState state) {
		this.state = state;
	}

	/**
	 * @return the election ordinality of the remote load balancer process that
	 *         this object represents.
	 */
	public int getElectionOrdinality() {
		return electionOrdinality;
	}

	/**
	 * @param electionOrdinality
	 *            the current election ordinality of the remote load balancer
	 *            process that this object represents.
	 */
	public void setElectionOrdinality(int electionOrdinality) {
		this.electionOrdinality = electionOrdinality;
	}

	/**
	 * @return the current SocketChannel that is (or should be) connected to
	 *         this remote node.
	 */
	public SocketChannel getSocketChannel() {
		return socketChannel;
	}

	/**
	 * @param socketChannel
	 *            the SocketChannel that will be used to send/receive message
	 *            to/from this remote node.
	 */
	public void setSocketChannel(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}
}
