package faultModule;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import commsModel.LoadBalancerState;
import commsModel.RemoteLoadBalancer;
import connectionUtils.MessageType;
import loadBalancer.AbstractLoadBalancer;

/**
 * @author Joachim
 *         <p>
 *         Runnable class to be used by an {@link AbstractLoadBalancer} to
 *         periodically broadcast a heart message to all other (passive) load
 *         balancer nodes.
 *         </p>
 *
 */
public class HeartbeatBroadcaster implements Runnable {

	/**
	 * A set containing all other load balancer nodes that exist in the system.
	 */
	private Set<RemoteLoadBalancer> remoteLoadBalancers;

	/**
	 * The interval, in milliseconds, that this HeartbeatBroadcaster will send
	 * heartbeat messages at.
	 */
	private int heartbeatIntervalMillis;

	/**
	 * The state of the load balancer that is initialising this
	 * HeartbeatBroadcaster that subsequently determines whether the broadcast
	 * message is <code>ACTIVE_ALIVE_CONFIRM</code> or
	 * <code>BACKUP_ALIVE_CONFIRM</code>.
	 */
	private LoadBalancerState broadcastState;

	/**
	 * Flag used to terminate this HeartbeatBroadcaster thread.
	 */
	private boolean isTerminated = false;
	
	/**
	 * Creates a new HeartbeatBroadcaster instance that, when started in a
	 * thread, will periodically send heartbeat messages to all remote load
	 * balancers in the <code>remoteLoadBalancers</code> set, at the specified
	 * <code>hearbeatIntervalMillis</code>.
	 */
	public HeartbeatBroadcaster(Set<RemoteLoadBalancer> remoteLoadBalancers, int heartbeatIntervalMillis,
			LoadBalancerState broadcastState) {
		if (remoteLoadBalancers == null || remoteLoadBalancers.isEmpty())
			throw new IllegalArgumentException("Remote load balancer set cannot be null or empty.");
		if (heartbeatIntervalMillis < 1)
			throw new IllegalArgumentException("Heartbeat interval must be at least 1 millisecond.");

		this.remoteLoadBalancers = remoteLoadBalancers;
		this.heartbeatIntervalMillis = heartbeatIntervalMillis;
		this.broadcastState = broadcastState;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run() Begins sending ACTIVE_ALIVE_CONFIRM or
	 * BACKUP_ALIVE_CONFIRM messages at the specified heartbeat interval rate to
	 * all other load balancer nodes in the system.
	 */
	@Override
	public void run() {
		MessageType broadcastMessage = broadcastState.equals(LoadBalancerState.ACTIVE)
				? MessageType.ACTIVE_ALIVE_CONFIRM : MessageType.BACKUP_ALIVE_CONFIRM;

		while (!isTerminated) {
			for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
				if (remoteLoadBalancer.isConnected()
						&& remoteLoadBalancer.getState().equals(LoadBalancerState.PASSIVE)) {
					sendHeartbeat(remoteLoadBalancer, broadcastMessage);
				}
			}
			try {
				Thread.sleep(heartbeatIntervalMillis);
			} catch (Exception e) {

			}
		}
	}
	
	public void cancel() {
		isTerminated = true;
	}

	/**
	 * Sends an the specified heartbeat message to the specified
	 * {@link RemoteLoadBalancer}.
	 * 
	 * @param remoteLoadBalancer
	 *            the RemoteLoadbalancer to send the heartbeat message to
	 * @param broadcastMessage
	 *            the message type to send - active or passive.
	 */
	private void sendHeartbeat(RemoteLoadBalancer remoteLoadBalancer, MessageType broadcastMessage) {
		ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.put((byte) broadcastMessage.getValue());
		buffer.flip();
		try {
			while (buffer.hasRemaining()) {
				remoteLoadBalancer.getSocketChannel().write(buffer);
			}
		} catch (IOException e) {
			if (e.getMessage() != null && e.getMessage().equals("An existing connection was forcibly closed by the remote host")) {
				try {
					remoteLoadBalancer.getSocketChannel().close();
				} catch (IOException e1) {
				}
			}
		}
	}
}
