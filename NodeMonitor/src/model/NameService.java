package model;

import java.net.InetSocketAddress;

/**
 * @author Joachim
 *         <p>
 * 		Object used to represent the NameService node in the system.
 *         </p>
 *
 */
public class NameService extends AbstractRemoteSystemComponent {

	/**
	 * The port that is currently held in the NameService for the active load balancer.
	 */
	private int currentHostPort;
	
	/**
	 * The IP address that is currently held in the NameService for the active load balancer. 
	 */
	private String currentHostIP;
	/**
	 * Constructs a new NameService instance representing the
	 * NameService in the system, with the specified unique ID and
	 * address.
	 * 
	 * @param componentID
	 *            the unique ID of this NameService
	 * @param remoteAddress
	 *            the remote address of this NameService
	 */
	public NameService(int componentID, InetSocketAddress remoteAddress) {
		this.componentID = componentID;
		this.remoteAddress = remoteAddress;
	}
	
	/**
	 * @return The port that is currently held in the NameService for the active load balancer.
	 */
	public int getCurrentHostPort() {
		return currentHostPort;
	}
	
	/**
	 * @return The IP address that is currently held in the NameService for the active load balancer. 
	 */
	public String getCurrentHostIP() {
		return currentHostIP;
	}
	
	/**
	 * Sets the port and IP that is currently held by the NameService for the active load balancer.
	 * @param hostPort the new port for the host
	 * @param hostIP the new IP for the host
	 */
	public void setCurrentHostAddress(int hostPort, String hostIP) {
		this.currentHostPort = hostPort;
		this.currentHostIP = hostIP;
	}
}
