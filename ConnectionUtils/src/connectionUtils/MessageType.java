package connectionUtils;

/**
 * @author Joachim
 *         <p>
 * 		MessageType class used to handle all data sending/receiving between
 *         network nodes. Note that the MessageType enumerations are never used
 *         directly - they are converted to/from their numerical value on every
 *         send/receive.
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
	SERVER_TOKEN(10),
	
	
	////////// ACTIVE-PASSIVE COORDINATION MESSAGES //////////
	/**
	 * A request to the recipient node for it's current load balancer state (active/passive).
	 */
	STATE_REQUEST(11),
	
	/**
	 * A message notifying that the sending node is the active load balancer.
	 */
	ACTIVE_NOTIFY(12),
	
	/**
	 * A message notifying that the sending node is a passive load balancer, to be
	 * accompanied by it's current election ordinality. 
	 */
	PASSIVE_NOTIFY(13),
	
	/**
	 * A message notifying the recipient that an election is currently in progress,
	 * indicating that the recipient node should wait for the election to finish.
	 */
	ELECTION_IN_PROGRESS_NOTIFY(14),
	
	/**
	 * A message notifying that the sending node has detected multiple active load balancers
	 * in the network, and prompts an emergency election.
	 */
	MULTIPLE_ACTIVES_WARNING(15),
	
	/**
	 * An election prompt either initialising an election or transporting the sending node's
	 * election candidacy message.
	 */
	ELECTION_MESSAGE(16),
	
	/**
	 * A message to all other nodes that the sending node has detected a failure.
	 */
	ACTIVE_HAS_FAILED(17),
	
	/**
	 * A message in response to an <code>ACTIVE_HAS_FAILED</code> message indicating 
	 * that the sending node has detected that the active is still alive. 
	 */
	ACTIVE_IS_ALIVE(18),
	
	/**
	 * Used for emergency elections. <br/>
	 * In the case of there being multiple actives present
	 * in the system, this election will be used to carry the sending node's IP address (last octet)
	 * to use as it's candidacy suitability. <br/>
	 * In the case that both the active and the passive backup have failed, the remaining passive 
	 * nodes will send their last stored average server latency value immediately.
	 */
	EMERGENCY_ELECTION_MESSAGE(19);
	

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
