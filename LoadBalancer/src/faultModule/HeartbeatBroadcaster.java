package faultModule;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Set;

import commsModel.RemoteLoadBalancer;
import connectionUtils.ConnectNIO;
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
				if (remoteLoadBalancer.getSocketChannel() == null) {
					remoteLoadBalancer
							.setSocketChannel(ConnectNIO.getNonBlockingSocketChannel(remoteLoadBalancer.getAddress()));
				}
				if (remoteLoadBalancer.getSocketChannel().isConnected()) {
					sendHeartbeat(remoteLoadBalancer.getSocketChannel());
				}
			}

			try {
				Thread checkForMessageThread = new Thread(new Runnable() {

					@Override
					public void run() {
						while (!Thread.currentThread().isInterrupted()) {
							for (final RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
								if (remoteLoadBalancer.getSocketChannel() != null) {
									checkForMessages(remoteLoadBalancer.getSocketChannel());
								}
							}
							try {
								Thread.sleep(heartbeatIntervalSecs * 100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}

				});
				checkForMessageThread.start();
				Thread.sleep(heartbeatIntervalSecs * 1000);
				checkForMessageThread.interrupt();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
			try {
				remoteLoadBalancer.getSocketChannel().close();
			} catch (IOException e) {
				e.printStackTrace();
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
		while (buffer.hasRemaining()) {
			try {
				socketChannel.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Listens for messages on the specified {@link SocketChannel}, and responds
	 * to an <code>ALIVE_REQUEST</code> message with a
	 * <code>ALIVE_CONFIRM</code> message. Since this class uses not blocking
	 * sockets to communicate with other nodes, this method will return straight
	 * away if no messages are found in the buffer. Therefore, this method
	 * should be called periodically from this class's <code>run</code> method.
	 * 
	 * @param socketChannel
	 *            the SocketChannel on which to check for messages.
	 */
	private void checkForMessages(SocketChannel socketChannel) {
		ByteBuffer buffer = ByteBuffer.allocate(1);
		try {
			while (socketChannel.read(buffer) > 0) {
				buffer.flip();
				MessageType messageType = MessageType.values()[buffer.get()];
				if (messageType.equals(MessageType.ALIVE_REQUEST)) {
					buffer.clear();
					buffer.put((byte) MessageType.ALIVE_CONFIRM.getValue());
					buffer.flip();
					while (buffer.hasRemaining()) {
						socketChannel.write(buffer);
					}
				}
			}
		} catch (IOException e) {
			// e.printStackTrace();
		}
	}
}
