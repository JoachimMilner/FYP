package connectionUtils;

/**
 * @author Joachim
 *         <p>
 *         MessageType class used to handle all data sending/receiving between
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
	 * A message from the name service containing the current primary host's
	 * address.
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

	////////// CLIENT-LOAD BALANCER MESSAGES //////////
	/**
	 * A request to the primary load balancer for the connection details of an
	 * available server.
	 */
	AVAILABLE_SERVER_REQUEST(7),

	/**
	 * A server token message containing the address of an available server to
	 * use.
	 */
	SERVER_TOKEN(8),

	////////// ACTIVE-PASSIVE COORDINATION MESSAGES //////////
	/**
	 * A request to a node to determine if it is alive/responsive.
	 */
	ALIVE_REQUEST(9),

	/**
	 * Response from the active load balancer to indicate that it is alive (used
	 * as heartbeat).
	 */
	ACTIVE_ALIVE_CONFIRM(10),

	/**
	 * Response from the elected backup to indicate that it is alive (used as
	 * heartbeat).
	 */
	BACKUP_ALIVE_CONFIRM(11),

	/**
	 * A message notifying that the sending node is the active load balancer.
	 * When a node transitions to the active state, it broadcasts this message.
	 * Similarly if an active receives a heartbeat from another active node it
	 * broadcasts this message to say "I am the only active" prompting all other
	 * active nodes to move to the passive state. Any passive node that receives
	 * this message marks the sending node as the active.
	 */
	ACTIVE_DECLARATION(12),

	/**
	 * A message notifying that the sending node is a passive load balancer, to
	 * be accompanied by it's current election ordinality.
	 */
	PASSIVE_NOTIFY(13),

	/**
	 * An election prompt either initialising an election or transporting the
	 * sending node's election candidacy message.
	 */
	ELECTION_MESSAGE(14);

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
