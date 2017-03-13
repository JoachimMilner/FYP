package logging;

/**
 * @author Joachim
 *         <p>
 *         Enumeration class used for all system logging at the Node Monitor.
 *         <p>
 *
 */
public enum LogMessageType {

	////////// NODE REGISTRATION MESSAGES //////////
	/**
	 * Message from the ClientVirtualizer requesting to register. Message should
	 * also contain the default client configuration settings.
	 */
	CLIENT_REGISTER(0),

	/**
	 * Message from the NameService requesting to register.
	 */
	NAME_SERVICE_REGISTER(1),

	/**
	 * Message from a Server requesting to register.
	 */
	SERVER_REGISTER(2),

	/**
	 * Message from s LoadBalancer requesting to register.
	 */
	LOAD_BALANCER_REGISTER(3),

	/**
	 * A message response from the node monitor with a unique ID attached for
	 * this component.
	 */
	REGISTRATION_CONFIRM(4),

	////////// LOGGING MESSAGES //////////
	/**
	 * Message containing the total number of sent and received client messages.
	 */
	CLIENT_MESSAGE_COUNT(5),

	/**
	 * A message from the NodeMonitor to the client containing updated client
	 * options (i.e. number of clients, message sending frequency etc.)
	 */
	CLIENT_UPDATE_SETTINGS(6),

	/**
	 * Message indicating that a client is unable to connect to the load
	 * balancer after re-requesting name resolution from the name service. </br>
	 * Not currently used.
	 */
	CLIENT_CANNOT_CONNECT_TO_SERVICE(7), 

	/**
	 * Message indicating that the load balancer service has been recovered from
	 * the client's perspective. </br>
	 * Not currently used.
	 */
	CLIENT_RECONNECTED_TO_SERVICE(8), 

	/**
	 * Message indicating that a node has registered its address with the name
	 * service, also containing the address of the remote node.
	 */
	NAME_SERVICE_ADDR_REGISTERED(9),

	/**
	 * A CPU load reading from a server, containing a double representing the
	 * load as a percentage.
	 */
	SERVER_CPU_LOAD(10),

	/**
	 * Message indicating that the sending load balancer process has entered the
	 * active state.
	 */
	LOAD_BALANCER_ENTERED_ACTIVE(11),

	/**
	 * Message indicating that the sending load balancer process has entered
	 * the passive state.
	 */
	LOAD_BALANCER_ENTERED_PASSIVE(12),

	/**
	 * Notification that the sending node has detected failure of the active
	 * load balancer.
	 */
	LOAD_BALANCER_ACTIVE_FAILURE_DETECTED(13),
	
	/**
	 * Notification that the sending node has detected failure of the backup 
	 * load balancer.
	 */
	LOAD_BALANCER_BACKUP_FAILURE_DETECTED(14),
	
	/**
	 * Notification that a previous failure detection of the active load balancer
	 * has been dismissed by another passive node.
	 */
	LOAD_BALANCER_FAILURE_DETECTION_DISMISSED(15),
	
	/**
	 * Notification that the sending node has confirmed failure of the active
	 * load balancer.
	 */
	LOAD_BALANCER_FAILURE_CONFIRMED(16),

	/**
	 * Sent after an election has been performed to indicate that the sending node
	 * is now the elected backup. 
	 */
	LOAD_BALANCER_ELECTED_AS_BACKUP(17),
	
	/**
	 * Message indicating that the current passive backup has initiated a re-election.
	 */
	LOAD_BALANCER_PROMPTED_RE_ELECTION(18),

	/**
	 * Message indicating that the sending node has detected multiple active
	 * load balancers in the system.
	 */
	LOAD_BALANCER_MULTIPLE_ACTIVES_DETECTED(19),
	
	/**
	 * Message indicating that the sending node has detected the absence of an active
	 * load balancer and subsequently promoted itself. 
	 */
	LOAD_BALANCER_NO_ACTIVE_DETECTED(21);
	
	/**
	 * Message indicating that the sending node has detected the absence of a backup
	 * load balancer and subsequently initiated a pre-election.
	 */
	//LOAD_BALANCER_NO_BACKUP_DETECTED(21);

	/**
	 * Numerical value attributed to each enum that will be set as the first
	 * byte in any TCP segment.
	 */
	private int value;

	/**
	 * Enumerator constructor.
	 */
	private LogMessageType(int value) {
		this.value = value;
	}

	/**
	 * @return the associated numerical value of this enum
	 */
	public int getValue() {
		return this.value;
	}

}
