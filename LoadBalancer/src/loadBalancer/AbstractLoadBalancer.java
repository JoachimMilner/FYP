package loadBalancer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import commsModel.LoadBalancerState;
import commsModel.RemoteLoadBalancer;
import commsModel.Server;
import connectionUtils.ConnectNIO;
import connectionUtils.MessageType;

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
	 * The port that this load balancer will listen for incoming TCP connections
	 * on.
	 */
	protected int acceptPort;

	/**
	 * A set of all other load balancer nodes in the system.
	 */
	protected Set<RemoteLoadBalancer> remoteLoadBalancers;

	/**
	 * A set of the back end servers in the system.
	 */
	protected Set<Server> servers;

	/**
	 * The address of the name resolution service.
	 */
	protected InetSocketAddress nameServiceAddress;

	/**
	 * This method is used to determine the initial state of this load balancer
	 * node. The algorithm used is as follows: </br>
	 * <ul>
	 * <li>Attempt to message all other load balancer nodes and request their
	 * state.</li>
	 * <li>If any node responds with election_in_progress, wait for election
	 * to finish and re-request states.</li>
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
	public static LoadBalancerState coordinateState(Set<RemoteLoadBalancer> remoteLoadBalancers,
			int connectTimeoutSecs) {
		LoadBalancerState determinedState = null;
		int numberOfRemotes = remoteLoadBalancers.size();
		Thread[] connectionThreads = new Thread[numberOfRemotes];

		while (determinedState == null || determinedState.equals(LoadBalancerState.ELECTION_IN_PROGRESS)) {
			Iterator<RemoteLoadBalancer> iterator = remoteLoadBalancers.iterator();
			for (int i = 0; i < numberOfRemotes; i++) {
				final RemoteLoadBalancer remoteLoadBalancer = iterator.next();

				connectionThreads[i] = new Thread(new Runnable() {

					@Override
					public void run() {
						sendStateRequest(remoteLoadBalancer, connectTimeoutSecs);
					}

				});
				connectionThreads[i].start();
			}

			for (int i = 0; i < numberOfRemotes; i++) {
				try {
					connectionThreads[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			int activeCount = 0;
			int passiveCount = 0;
			for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
				LoadBalancerState state = remoteLoadBalancer.getState();
				if (state != null) {
					switch (state) {
					case ACTIVE:
						activeCount++;
						break;
					case PASSIVE:
						passiveCount++;
						break;
					case ELECTION_IN_PROGRESS:
						determinedState = LoadBalancerState.ELECTION_IN_PROGRESS;
						break;
					}
				}
			}
			int totalResponses = activeCount + passiveCount;

			if (determinedState != null && determinedState.equals(LoadBalancerState.ELECTION_IN_PROGRESS)) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else if (activeCount == 1) {
				determinedState = LoadBalancerState.PASSIVE;
			} else if (totalResponses == 0) {
				determinedState = LoadBalancerState.ACTIVE;
			}

		}
		return determinedState;
	}

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
	private static void sendStateRequest(RemoteLoadBalancer remoteLoadBalancer, int connectTimeoutSecs) {
		try {
			SocketChannel socketChannel = remoteLoadBalancer.getSocketChannel();
			if (socketChannel == null || !socketChannel.isConnected()) {
				socketChannel = ConnectNIO.getNonBlockingSocketChannel(remoteLoadBalancer.getAddress());
			} 
			ByteBuffer buffer = ByteBuffer.allocate(5);
			buffer.put((byte) MessageType.STATE_REQUEST.getValue());
			buffer.flip();
			while (buffer.hasRemaining()) {
				socketChannel.write(buffer);
			}

			Selector readSelector = Selector.open();
			socketChannel.register(readSelector, SelectionKey.OP_READ);
			if (readSelector.select(connectTimeoutSecs * 1000) == 0) {
				// This remote is either down, unresponsive, or has not yet been
				// started.
				remoteLoadBalancer.setIsAlive(false);
			} else {
				buffer.clear();
				socketChannel.read(buffer);
				buffer.flip();
				MessageType messageType = MessageType.values()[buffer.get()];
				switch (messageType) {
				case ACTIVE_NOTIFY:
					remoteLoadBalancer.setState(LoadBalancerState.ACTIVE);
					break;
				case PASSIVE_NOTIFY:
					remoteLoadBalancer.setState(LoadBalancerState.PASSIVE);
					int electionOrdinality = buffer.getInt();
					remoteLoadBalancer.setElectionOrdinality(electionOrdinality);
					break;
				case ELECTION_IN_PROGRESS_NOTIFY:
					remoteLoadBalancer.setState(LoadBalancerState.ELECTION_IN_PROGRESS);
					break;
				default:
					// bad message type received
					break;
				}
			}
			readSelector.close();
		} catch (IOException e) {
			// e.printStackTrace();
		}
	}
}
