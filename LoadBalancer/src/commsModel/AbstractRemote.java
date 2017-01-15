package commsModel;

import java.net.InetSocketAddress;

/**
 * @author Joachim
 *         <p>
 *         Interface representing a remote process. Contains properties and
 *         methods that are common of all remotes that the load balancer will
 *         communicate with.
 *         </p>
 *
 */
public abstract class AbstractRemote {

	/**
	 * The address of this remote object.
	 */
	protected InetSocketAddress address;

	
	/**
	 * Boolean value indicating whether this remote process is responsive. Set
	 * to false on object instantiation and then updated by the concrete
	 * implementation of this class.
	 */
	protected boolean isAlive = false;
	
	
	/**
	 * @return the address of the remote process that this object represents.
	 */
	public InetSocketAddress getAddress() {
		return address;
	}
	

	/**
	 * @return true if this remote process is alive or false if it is down or unresponsive.
	 */
	public boolean isAlive() {
		return isAlive;
	}

}
