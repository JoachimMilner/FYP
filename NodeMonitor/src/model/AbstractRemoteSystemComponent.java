package model;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * @author Joachim
 *         <p>
 * 		Abstract base class for remote system components.
 *         </p>
 *
 */
public abstract class AbstractRemoteSystemComponent {
	
	/**
	 * The unique ID of this remote system component.
	 */
	protected int componentID;
	
	/**
	 * The stored address (IP and port) for this remote system component.
	 */
	protected InetSocketAddress remoteAddress;
	
	/**
	 * The socket channel that is connected to this remote system component.
	 */
	protected SocketChannel socketChannel;

	/**
	 * @return the unique ID of this component.
	 */
	public int getComponentID() {
		return componentID;
	}

	/**
	 * @return the stored address for this component/
	 */
	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	/**
	 * @return the socket channel that is connected to this remote component.
	 */
	public SocketChannel getSocketChannel() {
		return socketChannel;
	}

	/**
	 * @param socketChannel the socket channel to use for this component.
	 */
	public void setSocketChannel(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

}
