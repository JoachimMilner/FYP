package connectionUtils;

/**
 * @author Joachim
 *         <p>
 * 		MessageType class used to handle all data sending/receiving between
 *         network nodes. Note that the MessageType enumerations are never used
 *         directly - they are converted to/from their numerical value on every
 *         send/received.
 *         </p>
 */
public enum MessageType {

	////////// CLIENT-SERVER MESSAGES //////////
	/**
	 * A request sent from a client process containing data to be processed by a
	 * server.
	 */
	CLIENT_REQUEST(0),

	/**
	 * A processed response from the server to be sent to a client process.
	 */
	SERVER_RESPONSE(1),
	

	////////// NAME SERVICE MESSAGES //////////
	/**
	 * A message notifying the name service that the sending node is the primary
	 * host (load balancer).
	 */
	HOST_ADDR_NOTIFY(2),

	/**
	 * A request to the name service for the current primary host's address.
	 */
	HOST_ADDR_REQUEST(3),
	
	/**
	 * A message from the name service containing the current primary host's address. 
	 */
	HOST_ADDR_RESPONSE(4),
	

	////////// SERVER LOAD MESSAGES //////////
	/**
	 * A request to the server for its current CPU load info.
	 */
	SERVER_CPU_REQUEST(5),

	/**
	 * A server response containing the given node's current CPU load.
	 */
	SERVER_CPU_NOTIFY(6),
	
	
	////////// NODE ALIVE MESSAGES //////////
	/**
	 * A request to a node to determine if it is alive/responsive.
	 */
	ALIVE_REQUEST(7),
	
	/**
	 * Response from a node to indicate that it is alive.
	 */
	ALIVE_CONFIRM(8),
	
	
	////////// CLIENT-LOAD BALANCER MESSAGES //////////
	/**
	 * A request to the primary load balancer for the connection details of an available server. 
	 */
	AVAILABLE_SERVER_REQUEST(9),
	
	/**
	 * A server token message containing the address of an available server to use. 
	 */
	SERVER_TOKEN(10);
	

	/**
	 * Numerical value attributed to each enum that will be set as the first
	 * byte in any TCP segment.
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
