package faultModule;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import commsModel.RemoteLoadBalancer;
import connectionUtils.MessageType;
import loadBalancer.ActiveLoadBalancer;

/**
 * @author Joachim
 *         <p>
 *         Runnable class to be used by an {@link ActiveLoadBalancer} to
 *         periodically broadcast an <code>ALIVE_CONFIRM</code> message to all
 *         other (passive) load balancer nodes, as well as responding to
 *         <code>ALIVE_REQUEST</code> messages.
 *         </p>
 *
 */
public class HeartbeatBroadcaster implements Runnable {

	/**
	 * A set containing all other (passive) load balancer nodes that exist in
	 * the system.
	 */
	private Set<RemoteLoadBalancer> remoteLoadBalancers;

	/**
	 * The interval, in milliseconds, that this HeartbeatBroadcaster will send
	 * <code>ALIVE_CONFIRM</code> messages at.
	 */
	private int heartbeatIntervalMillis;

	/**
	 * Creates a new HeartbeatBroadcaster instance that, when started in a
	 * thread, will periodically send <code>ALIVE_CONFIRM</code> messages to all
	 * remote load balancers in the <code>remoteLoadBalancers</code> set, at the
	 * specified <code>hearbeatIntervalMillis</code>.
	 */
	public HeartbeatBroadcaster(Set<RemoteLoadBalancer> remoteLoadBalancers, int heartbeatIntervalMillis) {
		if (remoteLoadBalancers == null || remoteLoadBalancers.isEmpty())
			throw new IllegalArgumentException("Remote load balancer set cannot be null or empty.");
		if (heartbeatIntervalMillis < 1)
			throw new IllegalArgumentException("Heartbeat interval must be at least 1 millisecond.");

		this.remoteLoadBalancers = remoteLoadBalancers;
		this.heartbeatIntervalMillis = heartbeatIntervalMillis;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run() Begins sending ALIVE_CONFIRM messages at
	 * the specified heartbeat interval rate to all other load balancer nodes in
	 * the system.
	 */
	@Override
	public void run() {

		while (!Thread.currentThread().isInterrupted()) {

			for (final RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
				if (remoteLoadBalancer.isConnected()) {
					sendHeartbeat(remoteLoadBalancer);
				}
			}
			try {
				Thread.sleep(heartbeatIntervalMillis);
			} catch (Exception e) {

			}
		}
	}

	/**
	 * Sends an <code>ALIVE_CONFIRM</code> message (i.e. a heartbeat) to the
	 * specified {@link RemoteLoadBalancer}.
	 * 
	 * @param remoteLoadBalancer
	 *            the RemoteLoadbalancer to send the heartbeat message to
	 */
	private void sendHeartbeat(RemoteLoadBalancer remoteLoadBalancer) {
		ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.put((byte) MessageType.ACTIVE_ALIVE_CONFIRM.getValue());
		buffer.flip();
		try {
			while (buffer.hasRemaining()) {
				remoteLoadBalancer.getSocketChannel().write(buffer);
			}
		} catch (IOException e) {
			if (e != null & e.getMessage().equals("An existing connection was forcibly closed by the remote host")) {
				remoteLoadBalancer.setSocketChannel(null);
			}
		}

	}
}
