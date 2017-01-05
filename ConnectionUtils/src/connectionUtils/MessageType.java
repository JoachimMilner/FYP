package connectionUtils;

/**
 * @author Joachim</br> MessageType class used to handle all data sending/receing between network nodes.
 * Note that the MessageType enumerations are never used directly - they are converted to/from their
 * numerical value on every send/received. 
 */
public enum MessageType {
	
	/**
	 * A request sent from a client process containing data to be processed by a server.
	 */
	CLIENT_REQUEST(0), 
	
	/**
	 * A processed response from the server to be sent to a client process.
	 */
	SERVER_RESPONSE(1), 
	
	/**
	 * A message notifying the name service that the sending node is the primary host (load balancer).
	 */
	HOST_ADDR_NOTIFY(2), 
	
	/**
	 * A request to the name service for the current primary host's address.
	 */
	HOST_ADDR_REQUEST(3), 
	
	/**
	 * A request to the server for its current CPU load info.
	 */
	SERVER_CPU_REQUEST(4), 
	
	/**
	 * A server response containing the given node's current CPU load.
	 */
	SERVER_CPU_NOTIFY(5);
	
	
	/**
	 * Numerical value attributed to each enum that will be set as the first byte in any TCP segment.
	 */
	private int value;
	
	
	/**
	 * Enumerator constructor.
	 */
	private MessageType(int value) {
		this.value = value;
	}
	
	
	/**
	 * @return the associated numerical value of this enum
	 */
	public int getValue() {
		return this.value;
	}
}
