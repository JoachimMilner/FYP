package commsModel;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

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
	 * The SocketChannel that is currently held for the connection to this
	 * remote node. Used to make communication with this node more
	 * convenient.
	 */
	protected SocketChannel socketChannel;
	
	
	/**
	 * @return the address of the remote process that this object represents.
	 */
	public InetSocketAddress getAddress() {
		return address;
	}
	
	
	/**
	 * @param isAlive the alive state of this remote process.
	 */
	public void setIsAlive(boolean isAlive) {
		this.isAlive = isAlive;
	}
	

	/**
	 * @return true if this remote process is alive or false if it is down or unresponsive.
	 */
	public boolean isAlive() {
		return isAlive;
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


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractRemote other = (AbstractRemote) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		return true;
	}

}
