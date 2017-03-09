package faultModule;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import commsModel.LoadBalancerState;
import commsModel.RemoteLoadBalancer;
import commsModel.Server;
import connectionUtils.ConnectNIO;
import connectionUtils.MessageType;
import loadBalancer.AbstractLoadBalancer;
import logging.ComponentLogger;
import logging.LogMessageType;

/**
 * @author Joachim
 *         <p>
 *         The main class for running a passive load balancer. Implements the
 *         {@link Runnable} interface, and when started in a new threads, begins
 *         running backup responsibilities:
 *         <ul>
 *         <li>Monitors the heartbeat of the active load balancer and
 *         initialises the recovery protocol in the case that failure is
 *         detected.</li>
 *         <li>Periodically coordinates pre-election with other passive nodes
 *         and determines the suitability ranking for all members.</li>
 *         </ul>
 *         </p>
 *
 */
public class PassiveLoadBalancer extends AbstractLoadBalancer implements Runnable {

	/**
	 * The default timeout duration that this object will use when monitoring
	 * the active load balancer's heartbeat.
	 */
	private int defaultTimeoutSecs;
	
	/**
	 * Flag indicating whether this node is the currently elected backup.
	 */
	private boolean isElectedBackup = false;
	
	/**
	 * Flag indicating whether an election is currently in progress.
	 */
	private boolean electionInProgress = false;

	/**
	 * The remote node currently acting as the primary load balancer. Reference
	 * maintained here for ease of use.
	 */
	private RemoteLoadBalancer currentActive;

	/**
	 * The timer used to monitor the active load balancer's heartbeat.
	 */
	private Timer heartbeatTimer;

	/**
	 * The most recently calculated value for this node's average latency to the
	 * servers. Used as the election ID for this process.
	 */
	private int averageServerLatency;

	/**
	 * Flag indicating that failure of the active load balancer has been
	 * detected.
	 */
	private boolean activeFailureDetected = false;

	/**
	 * Flag indicating that this passive node is expecting an
	 * <code>ALIVE_CONFIRM</code> message from the active load balancer.
	 */
	private boolean expectingAliveConfirmation = false;

	/**
	 * Flag indicating that this node has received confirmation that the active
	 * is alive, in the case that a failure has been suspected.
	 */
	private boolean receivedAliveConfirmation = false;

	/**
	 * Creates a new PassiveLoadBalancer object that acts as a backup load
	 * balancer process in the system.
	 * 
	 * @param acceptPort
	 *            the port on which to accept incoming connection requests
	 * @param remoteLoadBalancers
	 *            the set of remote load balancers in the system
	 * @param servers
	 *            the set of all back-end servers in the system
	 * @param nameServiceAddress
	 *            the address of the name service
	 * @param defaultTimeoutSecs
	 *            the default time duration when monitoring the active load
	 *            balancer's heartbeat.
	 */
	public PassiveLoadBalancer(int acceptPort, Set<RemoteLoadBalancer> remoteLoadBalancers, Set<Server> servers,
			InetSocketAddress nameServiceAddress, int defaultTimeoutSecs) {
		if (remoteLoadBalancers == null || remoteLoadBalancers.isEmpty())
			throw new IllegalArgumentException("Remote load balancer set cannot be null or empty.");
		if (servers == null || servers.isEmpty())
			throw new IllegalArgumentException("Servers set cannot be null or empty.");
		if (nameServiceAddress == null)
			throw new IllegalArgumentException("Name service address cannot be null.");
		if (defaultTimeoutSecs < 1)
			throw new IllegalArgumentException("Default timeout value must be a positive, non-zero value.");

		this.acceptPort = acceptPort;
		this.remoteLoadBalancers = remoteLoadBalancers;
		this.servers = servers;
		this.nameServiceAddress = nameServiceAddress;
		this.defaultTimeoutSecs = defaultTimeoutSecs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run() Starts a new ElectionManager that will
	 * periodically coordinate elections with other passive nodes in the system
	 * and then begins monitoring the state of the active load balancer.
	 */
	@Override
	public void run() {
		System.out.println("Initialising passive load balancer service on port " + acceptPort + "...");
		ComponentLogger.getInstance().log(LogMessageType.LOAD_BALANCER_ENTERED_PASSIVE);

		// Set current active load balancer for easy accessibility
		for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
			if (remoteLoadBalancer.getState() != null
					&& remoteLoadBalancer.getState().equals(LoadBalancerState.ACTIVE)) {
				currentActive = remoteLoadBalancer;
				break;
			}
		}

		// Initialise heartbeat monitor
		startHeartbeatTimer();

		ServerSocketChannel serverSocketChannel = ConnectNIO.getServerSocketChannel(acceptPort);

		while (!Thread.currentThread().isInterrupted()) {
			SocketChannel connectRequestSocket = null;
			try {
				connectRequestSocket = serverSocketChannel.accept();
			} catch (IOException e) {
				if (Thread.currentThread().isInterrupted()) {
					break;
				}
				e.printStackTrace();
			}
			if (connectRequestSocket != null) {
				System.out.println("Received connection request.");
				String connectingIP = connectRequestSocket.socket().getInetAddress().getHostAddress();
				for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
					if (remoteLoadBalancer.getAddress().getAddress().getHostAddress().equals(connectingIP)) {
						remoteLoadBalancer.setSocketChannel(connectRequestSocket);
						break;
					}
				}
			}
		}
		System.out.println("Passive load balancer shutting down...");

		heartbeatTimer.cancel();
		try {
			serverSocketChannel.close();
		} catch (IOException e) {
		}
	}

	@Override
	public void startLoadBalancerMessageListener(Thread loadBalancerThread) {
		while (!Thread.currentThread().isInterrupted()) {
			for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
				ByteBuffer buffer = ByteBuffer.allocate(100);
				try {
					SocketChannel socketChannel = remoteLoadBalancer.getSocketChannel();
					if (socketChannel == null || !socketChannel.isConnected()) {
						continue;
					}
					while (socketChannel.read(buffer) > 0) {
						buffer.flip();
						MessageType messageType = MessageType.values()[buffer.get()];
						switch (messageType) {
						case STATE_REQUEST:
							System.out.println("Received state request");
							buffer.clear();
							buffer.put((byte) MessageType.PASSIVE_NOTIFY.getValue());
							buffer.put((byte) (isElectedBackup ? 1 : 0));
							buffer.flip();
							while (buffer.hasRemaining()) {
								socketChannel.write(buffer);
							}
							break;
						case ALIVE_CONFIRM:
							if (remoteLoadBalancer.equals(currentActive)) {
								resetHeartbeatTimer();
							}
							break;
						case ACTIVE_HAS_FAILED:

							break;
						case ACTIVE_IS_ALIVE:

							break;
						case ELECTION_MESSAGE:

							break;
						default:
							break;
						}
					}
				} catch (IOException e) {
					// e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Starts a new timer that will begin the fault tolerance protocol after it
	 * has run for the specified duration.
	 */
	private void startHeartbeatTimer() {
		TimerTask timerTask = new TimerTask() {

			@Override
			public void run() {
				ComponentLogger.getInstance().log(LogMessageType.LOAD_BALANCER_FAILURE_DETECTED);
				System.out.println("Active load balancer failure detected.");
				Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
				// Suspected failure - ping active
				boolean isConnected = false;
				for (int i = 0; i < 10; i++) {
					if (currentActive.connect(0)) {
						isConnected = true;
						break;
					}
					try {
						Thread.sleep(defaultTimeoutSecs * 100);
					} catch (InterruptedException e) {

					}
				}
				if (!isConnected) {
					activeFailureDetected = true;

				} else {
					ByteBuffer buffer = ByteBuffer.allocate(1);
					buffer.put((byte) MessageType.ALIVE_REQUEST.getValue());
					buffer.flip();
					try {
						while (buffer.hasRemaining()) {
							currentActive.getSocketChannel().write(buffer);
						}
					} catch (IOException e) {
						//e.printStackTrace();

					}
				}

			}

		};
		heartbeatTimer = new Timer();
		heartbeatTimer.schedule(timerTask, defaultTimeoutSecs * 1000);
	}

	/**
	 * Resets the heartbeat timer - called whenever a heartbeat is received from
	 * the active load balancer.
	 */
	private void resetHeartbeatTimer() {
		heartbeatTimer.cancel();
		startHeartbeatTimer();
	}

}
