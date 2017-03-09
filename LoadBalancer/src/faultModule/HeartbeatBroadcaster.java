package faultModule;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
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
	 * The interval, in seconds, that this HeartbeatBroadcaster will send
	 * <code>ALIVE_CONFIRM</code> messages at.
	 */
	private int heartbeatIntervalSecs;

	/**
	 * Creates a new HeartbeatBroadcaster instance that, when started in a
	 * thread, will periodically send <code>ALIVE_CONFIRM</code> messages to all
	 * remote load balancers in the <code>remoteLoadBalancers</code> set, at the
	 * specified <code>hearbeatIntervalSecs</code>.
	 */
	public HeartbeatBroadcaster(Set<RemoteLoadBalancer> remoteLoadBalancers, int heartbeatIntervalSecs) {
		if (remoteLoadBalancers == null || remoteLoadBalancers.isEmpty())
			throw new IllegalArgumentException("Remote load balancer set cannot be null or empty.");
		if (heartbeatIntervalSecs < 1)
			throw new IllegalArgumentException("Heartbeat interval must be at least 1 second.");

		this.remoteLoadBalancers = remoteLoadBalancers;
		this.heartbeatIntervalSecs = heartbeatIntervalSecs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run() Begins sending ALIVE_CONFIRM messages at
	 * the specified heartbeat interval rate to all other load balancer nodes in
	 * the system. Also listens for ALIVE_REQUEST messages and responds with an
	 * ALIVE_CONFIRM if one is received.
	 */
	@Override
	public void run() {

		while (!Thread.currentThread().isInterrupted()) {

			for (final RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
				if (remoteLoadBalancer.isConnected()) {
					sendHeartbeat(remoteLoadBalancer.getSocketChannel());
				}
			}
			try {
				Thread.sleep(heartbeatIntervalSecs * 1000);
			} catch (Exception e) {

			}
		}
	}

	/**
	 * Sends an <code>ALIVE_CONFIRM</code> message (i.e. a heartbeat) to the
	 * specified {@link SocketChannel}.
	 * 
	 * @param socketChannel
	 *            the SocketChannel on which to send the heartbeat message
	 */
	private void sendHeartbeat(SocketChannel socketChannel) {
		ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.put((byte) MessageType.ALIVE_CONFIRM.getValue());
		buffer.flip();
		try {
			while (buffer.hasRemaining()) {
				socketChannel.write(buffer);
			}
		} catch (IOException e) {

		}

	}
}
