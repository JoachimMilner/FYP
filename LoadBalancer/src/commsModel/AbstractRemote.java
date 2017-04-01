package commsModel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import connectionUtils.ConnectNIO;

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
	
	/**
	 * Attempts to connect to the remote node with a given timeout. 
	 * Does nothing if already connected.
	 * @return true if this object's SocketChannel is now connected, otherwise false.
	 */
	public boolean connect(int timeoutMillis) {
		if (socketChannel == null || !socketChannel.isConnected()) {
			SocketChannel newSocketChannel = ConnectNIO.getNonBlockingSocketChannel(address, timeoutMillis);
			if (newSocketChannel != null && newSocketChannel.isConnected()) {
				socketChannel = newSocketChannel;
				return true;
			} else if (newSocketChannel != null) {
				try {
					newSocketChannel.close();
				} catch (IOException e) {}
			}
			return false;
		}
		return true;
	}
	
	/**
	 * @return Convenience method for checking if this remote node is currently connected.
	 */
	public boolean isConnected() {
		return socketChannel != null && socketChannel.isConnected();
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
