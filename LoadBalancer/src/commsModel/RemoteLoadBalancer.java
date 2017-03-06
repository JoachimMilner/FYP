package commsModel;

import java.net.InetSocketAddress;

import connectionUtils.ConnectNIO;

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
	 * Boolean value indicating whether this node has been elected as the backup
	 * to take over in the case of primary failure. Set to false by default, set
	 * to true after an election has been coordinated.
	 */
	private boolean isElectedBackup = false;

	/**
	 * The currently stored value for this node's candidacy. If this is a
	 * passive remote load balancer, the value stored will be its last known
	 * average server latency value. If this is an active node, the value will
	 * contain last octet of the node's IP address to be used to resolve a
	 * multiple-active conflict in the system.
	 */
	private Double candidacyValue;

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
	 * @return true if this remote load balancer has been elected as the node to
	 *         take over in the case of primary failure, otherwise false.
	 */
	public boolean isElectedBackup() {
		return isElectedBackup;
	}

	/**
	 * @param isElectedBackup
	 *            whether this node is the elected backup.
	 */
	public void setIsElectedBackup(boolean isElectedBackup) {
		this.isElectedBackup = isElectedBackup;
	}

	/**
	 * @return the most up to date candidacy value for this node (can be null).
	 */
	public Double getCandidacyValue() {
		return candidacyValue;
	}

	/**
	 * @param candidacyValue
	 *            the new candidacy value for this node (can be null).
	 */
	public void setCandidacyValue(Double candidacyValue) {
		this.candidacyValue = candidacyValue;
	}

	/**
	 * Attempts to connect to the remote load balancer. Does nothing if already
	 * connected.
	 */
	public void connect() {
		if (socketChannel == null || !socketChannel.isConnected()) {
			socketChannel = ConnectNIO.getNonBlockingSocketChannel(address);
		}
	}
}
