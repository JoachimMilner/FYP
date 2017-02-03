package loadBalancer;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.junit.Test;

import commsModel.LoadBalancerState;
import commsModel.RemoteLoadBalancer;
import connectionUtils.MessageType;
import testUtils.TestUtils;

/**
 * @author Joachim
 *         <p>
 *         Contains tests for the {@link AbstractLoadBalancer}'s static
 *         method(s).
 *         </p>
 *
 */
public class AbstractLoadBalancerTests {

	/**
	 * Test the {@link AbstractLoadBalancer}'s <code>coordinateState</code>
	 * method. This test checks that when a new load balancer node is started,
	 * it initially messages all other load balancers in the system to request
	 * their state. Then notes accordingly which node are in which state.
	 * Passive nodes should have their <code>isElectedBackup</code> field set
	 * accordingly. </br>
	 * In this test case, one remote load balancer confirms its state as active
	 * while the other return passive. The <code>coordinateState</code> method
	 * should therefore return <code>PASSIVE</code>.
	 */
	@Test
	public void testAbstractLoadBalancer_coordinateStateOneActiveRemote() {
		Set<RemoteLoadBalancer> remoteLoadBalancers = TestUtils.getRemoteLoadBalancerSet(3);

		// Use some global variables to do assertions in sub threads.
		Thread[] mockRemoteLBThreads = new Thread[3];
		boolean[] receivedMessageFlags = { false, false, false };

		for (int i = 0; i < 3; i++) {
			final int j = i;
			mockRemoteLBThreads[j] = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						ServerSocketChannel mockRemoteSocketChannel = ServerSocketChannel.open();
						mockRemoteSocketChannel.socket().bind(new InetSocketAddress(8000 + j));
						mockRemoteSocketChannel.configureBlocking(false);

						Selector acceptSelector = Selector.open();
						mockRemoteSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
						if (acceptSelector.select(1000) == 0) {
							throw new SocketTimeoutException();
						}
						SocketChannel acceptedSocketChannel = mockRemoteSocketChannel.accept();
						acceptSelector.close();
						acceptedSocketChannel.configureBlocking(true);

						ByteBuffer buffer = ByteBuffer.allocate(2);
						acceptedSocketChannel.socket().setSoTimeout(1000);

						acceptedSocketChannel.read(buffer);

						buffer.flip();
						MessageType messageType = MessageType.values()[buffer.get()];
						if (messageType.equals(MessageType.STATE_REQUEST)) {
							receivedMessageFlags[j] = true;
						}
						buffer.clear();

						if (j == 0) {
							buffer.put((byte) MessageType.ACTIVE_NOTIFY.getValue());
						} else {
							buffer.put((byte) MessageType.PASSIVE_NOTIFY.getValue());
							// isElectedBackup value
							if (j == 1) {
								buffer.put((byte)1); //true
							} else {
								buffer.put((byte)0); //false
							}
							
						}
						buffer.flip();
						while (buffer.hasRemaining()) {
							acceptedSocketChannel.write(buffer);
						}

						mockRemoteSocketChannel.close();
						acceptedSocketChannel.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			});
			mockRemoteLBThreads[i].start();
		}

		LoadBalancerState loadBalancerState = AbstractLoadBalancer.coordinateState(remoteLoadBalancers, 3);

		for (int i = 0; i < 3; i++) {
			try {
				mockRemoteLBThreads[i].join();
				assertTrue(receivedMessageFlags[i]);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
			if (remoteLoadBalancer.getAddress().getPort() == 8000) {
				assertEquals(LoadBalancerState.ACTIVE, remoteLoadBalancer.getState());
			} else {
				assertEquals(LoadBalancerState.PASSIVE, remoteLoadBalancer.getState());
				if (remoteLoadBalancer.getAddress().getPort() == 8001) {
					assertTrue(remoteLoadBalancer.isElectedBackup());
				} else {
					assertFalse(remoteLoadBalancer.isElectedBackup());
				}
			}
		}

		assertEquals(LoadBalancerState.PASSIVE, loadBalancerState);
	}

	/**
	 * Test the {@link AbstractLoadBalancer}'s <code>coordinateState</code>
	 * method. </br>
	 * In this test case, the remote nodes are unresponsive and a connection
	 * cannot be established, so the method should return <code>ACTIVE</code> in
	 * order to elevate the node to the active state.
	 */
	@Test
	public void testAbstractLoadBalancer_coordinateStateNoResponses() {
		Set<RemoteLoadBalancer> remoteLoadBalancers = TestUtils.getRemoteLoadBalancerSet(4);

		LoadBalancerState loadBalancerState = AbstractLoadBalancer.coordinateState(remoteLoadBalancers, 2);

		assertEquals(LoadBalancerState.ACTIVE, loadBalancerState);
	}

	/**
	 * Test the {@link AbstractLoadBalancer}'s <code>coordinateState</code>
	 * method. </br>
	 * In this test case, the passive nodes in the system are currently
	 * performing an election, so the <code>coordinateState</code> method should
	 * wait before periodically re-requesting the state of the other nodes,
	 * until it is notified that the election is finished.
	 * @throws IOException 
	 */
	@Test
	public void testAbstractLoadBalancer_coordinateStateElectionInProgress() throws IOException {
		Set<RemoteLoadBalancer> remoteLoadBalancers = TestUtils.getRemoteLoadBalancerSet(3);
		
		Thread[] mockRemoteLBThreads = new Thread[3];
		boolean[] receivedSecondRequestFlags = { false, false, false };
		
		for (int i = 0; i < 3; i++) {
			final int j = i;
			mockRemoteLBThreads[j] = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						ServerSocketChannel mockRemoteSocketChannel = ServerSocketChannel.open();
						mockRemoteSocketChannel.socket().bind(new InetSocketAddress(8000 + j));
						mockRemoteSocketChannel.configureBlocking(false);

						Selector acceptSelector = Selector.open();
						mockRemoteSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
						if (acceptSelector.select(1000) == 0) {
							throw new SocketTimeoutException();
						}
						SocketChannel acceptedSocketChannel = mockRemoteSocketChannel.accept();
						acceptSelector.close();
						acceptedSocketChannel.configureBlocking(true);

						ByteBuffer buffer = ByteBuffer.allocate(5);
						acceptedSocketChannel.socket().setSoTimeout(5000);

						acceptedSocketChannel.read(buffer);

						buffer.flip();
						buffer.get();
						buffer.clear();

						buffer.put((byte) MessageType.ELECTION_IN_PROGRESS_NOTIFY.getValue());

						buffer.flip();
						while (buffer.hasRemaining()) {
							acceptedSocketChannel.write(buffer);
						}
						
						buffer.clear();
						
						acceptedSocketChannel.read(buffer);
						buffer.flip();
						MessageType messageType = MessageType.values()[buffer.get()];
						
						if (messageType.equals(MessageType.STATE_REQUEST)) {
							receivedSecondRequestFlags[j] = true;
						}
						
						buffer.clear();
						if (j == 0) {
							buffer.put((byte) MessageType.ACTIVE_NOTIFY.getValue());
						} else {
							buffer.put((byte) MessageType.PASSIVE_NOTIFY.getValue());
							// isElectedBackup value
							if (j == 1) {
								buffer.put((byte)1); //true
							} else {
								buffer.put((byte)0); //false
							}
						}
						buffer.flip();
						while (buffer.hasRemaining()) {
							acceptedSocketChannel.write(buffer);
						}

						mockRemoteSocketChannel.close();
						acceptedSocketChannel.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			});
			mockRemoteLBThreads[i].start();
		}

		LoadBalancerState loadBalancerState = AbstractLoadBalancer.coordinateState(remoteLoadBalancers, 3);

		for (int i = 0; i < 3; i++) {
			try {
				mockRemoteLBThreads[i].join();
				assertTrue(receivedSecondRequestFlags[i]);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
			if (remoteLoadBalancer.getAddress().getPort() == 8000) {
				assertEquals(LoadBalancerState.ACTIVE, remoteLoadBalancer.getState());
			} else {
				assertEquals(LoadBalancerState.PASSIVE, remoteLoadBalancer.getState());
				if (remoteLoadBalancer.getAddress().getPort() == 8001) {
					assertTrue(remoteLoadBalancer.isElectedBackup());
				} else {
					assertFalse(remoteLoadBalancer.isElectedBackup());
				}
			}
			remoteLoadBalancer.getSocketChannel().close();
		}

		assertEquals(LoadBalancerState.PASSIVE, loadBalancerState);
	}
}
