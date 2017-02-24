package model;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import logging.LogMessageType;

/**
 * @author Joachim
 *         <p>
 *         Object used to represent the ClientVirtualizer node in the system.
 *         </p>
 *
 */
public class ClientVirtualizer extends AbstractRemoteSystemComponent {

	/**
	 * The total number of requests that have been sent by all clients in the
	 * ClientVirtualizer.
	 */
	private int totalRequestsSent = 0;

	/**
	 * The total number of responses received by the ClientVirtualizer.
	 */
	private int totalResponsesReceived = 0;

	/**
	 * The total number of connection attempts to the load balancer that have
	 * failed.
	 */
	private int totalClientConnectFailures = 0;

	/**
	 * Constructs a new ClientVirtualizer instance representing the
	 * ClientVirtualizer in the system, with the specified unique ID and
	 * address.
	 * 
	 * @param componentID
	 *            the unique ID of this ClientVirtualizer
	 * @param remoteAddress
	 *            the remote address of this ClientVirtualizer
	 */
	public ClientVirtualizer(int componentID, InetSocketAddress remoteAddress) {
		this.componentID = componentID;
		this.remoteAddress = remoteAddress;
	}

	/**
	 * @return the total number of requests sent by all clients
	 */
	public int getTotalRequestsSent() {
		return totalRequestsSent;
	}

	/**
	 * @param totalRequestsSent
	 *            the total number of requests sent by all clients
	 */
	public void setTotalRequestsSent(int totalRequestsSent) {
		this.totalRequestsSent = totalRequestsSent;
	}

	/**
	 * @return the total number of responses received by all clients
	 */
	public int getTotalResponsesReceived() {
		return totalResponsesReceived;
	}

	/**
	 * @param totalResponsesReceived
	 *            the total number of responses received by all clients
	 */
	public void setTotalResponsesReceived(int totalResponsesReceived) {
		this.totalResponsesReceived = totalResponsesReceived;
	}

	/**
	 * @return the total number of connection attempts to the load balancer that
	 *         have failed
	 */
	public int getTotalClientConnectFailures() {
		return totalClientConnectFailures;
	}

	/**
	 * @param totalClientConnectFailures
	 *            the total number of connection attempts to the load balancer
	 *            that have failed
	 */
	public void setTotalClientConnectFailures(int totalClientConnectFailures) {
		this.totalClientConnectFailures = totalClientConnectFailures;
	}

	/**
	 * Attempts to send a configuration setting update to the ClientVirtualizer
	 * represented by this object.
	 * 
	 * @param maxClients
	 * @param minSendFrequency
	 * @param maxSendFrequency
	 * @param minClientRequests
	 * @param maxClientRequests
	 * @return true if send is successful, false otherwise.
	 */
	public boolean sendClientConfigurationUpdate(int maxClients, int minSendFrequency, int maxSendFrequency,
			int minClientRequests, int maxClientRequests) {
		ByteBuffer buffer = ByteBuffer.allocate(21);
		buffer.put((byte) LogMessageType.CLIENT_UPDATE_SETTINGS.getValue());
		buffer.putInt(maxClients);
		buffer.putInt(minSendFrequency);
		buffer.putInt(maxSendFrequency);
		buffer.putInt(minClientRequests);
		buffer.putInt(maxClientRequests);
		buffer.flip();

		while (buffer.hasRemaining()) {
			try {
				socketChannel.write(buffer);
			} catch (IOException e) {
				return false;
			}
		}
		return true;
	}
}
