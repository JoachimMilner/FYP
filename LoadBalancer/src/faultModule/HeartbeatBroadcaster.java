package faultModule;

import java.io.IOException;
import java.nio.BufferUnderflowException;
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

	@Override
	public void run() {

		while (!Thread.currentThread().isInterrupted()) {

			for (final RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
				if (remoteLoadBalancer.getSocketChannel() == null) {
					remoteLoadBalancer.setSocketChannel(ConnectNIO.getNonBlockingSocketChannel(remoteLoadBalancer.getAddress()));
				}
				if(remoteLoadBalancer.getSocketChannel().isConnected()) {
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
								Thread.sleep(heartbeatIntervalSecs * 100 );
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
