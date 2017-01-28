package commsModel;

/**
 * @author Joachim
 *         <p>
 * 		Enum used to represent the state of a load balance node. Primarily
 *         used in establishing the state of a newly started process.
 *         </p>
 *
 */
public enum LoadBalancerState {

	ACTIVE, PASSIVE, ELECTION_IN_PROGRESS

}
