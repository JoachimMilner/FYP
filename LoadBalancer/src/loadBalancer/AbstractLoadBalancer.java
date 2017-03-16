package loadBalancer;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import commsModel.RemoteLoadBalancer;
import commsModel.Server;

/**
 * @author Joachim
 *         <p>
 *         Abstract base class for active and passive load balancer processes.
 *         Contains shared properties for both subclasses.
 *         </p>
 *
 */
public abstract class AbstractLoadBalancer implements Runnable {

	/**
	 * The process that handles incoming connection requests for this load
	 * balancer. Passed between the active and passive states during program
	 * operation.
	 */
	protected LoadBalancerConnectionHandler connectionHandler;

	/**
	 * A set of all other load balancer nodes in the system.
	 */
	protected Set<RemoteLoadBalancer> remoteLoadBalancers;

	/**
	 * A set of the back end servers in the system.
	 */
	protected Set<Server> servers;

	/**
	 * Flag used to stop the current running load balancer process. An atomic
	 * boolean is used so the value can be accessed within anonymous runnable
	 * classes at runtime.
	 */
	protected final AtomicBoolean terminateThread = new AtomicBoolean(false);

	/**
	 * Flag indicating whether this load balancer is in a resolution state. That
	 * is, the node has been alerted that the system is in a multiple-active
	 * state and is attempting to resolve this with the other actives. Settings
	 * this flag indicates the following:
	 * <ol>
	 * <li>During the resolution period any other multiple-active messages are
	 * ignored.</li>
	 * <li>If this node is demoted to the passive state resulting from the
	 * emergency election, it will keep the flag set for a short period and
	 * ignore any multiple-active detections.</li>
	 * </ol>
	 */
	protected static boolean inResolutionState = false;

	/**
	 * Indefinitely checks for messages from all other load balancer nodes in
	 * the system.
	 */
	protected abstract void listenForLoadBalancerMessages();

	/**
	 * This method is used to determine the initial state of this load balancer
	 * node. The algorithm used is as follows: </br>
	 * <ul>
	 * <li>Attempt to message all other load balancer nodes and request their
	 * state.</li>
	 * <li>If any node responds with election_in_progress, wait for election to
	 * finish and re-request states.</li>
	 * <li>If any node responds as active, set flag on that node and enter
	 * passive state.</li>
	 * <li>If no responses, elevate self to active state.</li>
	 * <li>If all respond passive, start election.</li>
	 * </ul>
	 * 
	 * @param remoteLoadBalancers
	 *            the set of all other load balancers in the system.
	 * @return ACTIVE if this process should initialise in as an
	 *         {@link ActiveLoadBalancer}, or PASSIVE if it should initialise as
	 *         a {@link PassiveLoadBalancer}.
	 */
	/*
	 * public static LoadBalancerState coordinateState(Set<RemoteLoadBalancer>
	 * remoteLoadBalancers, int connectTimeoutSecs) { LoadBalancerState
	 * determinedState = null; int numberOfRemotes = remoteLoadBalancers.size();
	 * Thread[] connectionThreads = new Thread[numberOfRemotes];
	 * 
	 * while (determinedState == null) { Iterator<RemoteLoadBalancer> iterator =
	 * remoteLoadBalancers.iterator(); for (int i = 0; i < numberOfRemotes; i++)
	 * { final RemoteLoadBalancer remoteLoadBalancer = iterator.next();
	 * 
	 * connectionThreads[i] = new Thread(new Runnable() {
	 * 
	 * @Override public void run() { requestState(remoteLoadBalancer,
	 * connectTimeoutSecs); }
	 * 
	 * }); connectionThreads[i].start(); }
	 * 
	 * for (int i = 0; i < numberOfRemotes; i++) { try {
	 * connectionThreads[i].join(); } catch (InterruptedException e) {
	 * e.printStackTrace(); } }
	 * 
	 * int activeCount = 0; int passiveCount = 0; boolean electionInProgress =
	 * false; for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers)
	 * { LoadBalancerState state = remoteLoadBalancer.getState(); if (state !=
	 * null) { switch (state) { case ACTIVE: activeCount++; break; case PASSIVE:
	 * passiveCount++; break; case ELECTION_IN_PROGRESS: electionInProgress =
	 * true; break; } } } int totalResponses = activeCount + passiveCount;
	 * System.out.println("actives:" + activeCount);
	 * System.out.println("passives:" + passiveCount); if (electionInProgress) {
	 * try { Thread.sleep(2000); } catch (InterruptedException e) {
	 * e.printStackTrace(); } } else if (activeCount == 1) { determinedState =
	 * LoadBalancerState.PASSIVE; } else if (activeCount > 1) { // Force
	 * emergency election } else if (totalResponses == 0) { determinedState =
	 * LoadBalancerState.ACTIVE; }
	 * 
	 * } return determinedState; }
	 */

	/**
	 * Attempts to retrieve the state of the specified
	 * {@link RemoteLoadBalancer} and update the relevant fields in the object.
	 * 
	 * @param remoteLoadBalancer
	 *            the object representing the remote load balancer node to be
	 *            messaged
	 * @param connectTimeoutSecs
	 *            the duration in seconds to wait for a response
	 */
	/*
	 * private static void requestState(RemoteLoadBalancer remoteLoadBalancer,
	 * int connectTimeoutSecs) { try { if
	 * (!remoteLoadBalancer.connect(connectTimeoutSecs * 1000)) { return; }
	 * SocketChannel socketChannel = remoteLoadBalancer.getSocketChannel();
	 * ByteBuffer buffer = ByteBuffer.allocate(5); buffer.put((byte)
	 * MessageType.STATE_REQUEST.getValue()); buffer.flip(); while
	 * (buffer.hasRemaining()) { socketChannel.write(buffer); }
	 * 
	 * Selector readSelector = Selector.open();
	 * socketChannel.register(readSelector, SelectionKey.OP_READ); if
	 * (readSelector.select(connectTimeoutSecs * 1000) != 0) { buffer.clear();
	 * socketChannel.read(buffer); buffer.flip(); MessageType messageType =
	 * MessageType.values()[buffer.get()];
	 * System.out.println(messageType.name()); switch (messageType) { //
	 * ALIVE_CONFIRM is also a valid state notification as only an active node
	 * is capable // of sending this message. case ACTIVE_ALIVE_CONFIRM: case
	 * ACTIVE_NOTIFY: remoteLoadBalancer.setState(LoadBalancerState.ACTIVE);
	 * break; case PASSIVE_NOTIFY:
	 * remoteLoadBalancer.setState(LoadBalancerState.PASSIVE); boolean
	 * isElectedBackup = buffer.get() != 0;
	 * remoteLoadBalancer.setIsElectedBackup(isElectedBackup); break; case
	 * ELECTION_IN_PROGRESS_NOTIFY:
	 * remoteLoadBalancer.setState(LoadBalancerState.ELECTION_IN_PROGRESS);
	 * break; default: // bad message type received break; } }
	 * readSelector.close(); } catch (IOException e) { // e.printStackTrace(); }
	 * }
	 */
}
