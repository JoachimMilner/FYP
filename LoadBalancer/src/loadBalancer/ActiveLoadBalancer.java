package loadBalancer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import commsModel.LoadBalancerState;
import commsModel.RemoteLoadBalancer;
import commsModel.Server;
import connectionUtils.ConnectNIO;
import connectionUtils.MessageType;
import faultModule.HeartbeatBroadcaster;
import faultModule.PassiveLoadBalancer;
import logging.ComponentLogger;
import logging.LogMessageType;

/**
 * @author Joachim
 *         <p>
 *         The main active load balancer class. This object implements the
 *         {@link Runnable} interface and when started in a new thread, begins
 *         handling incoming client requests and distributing server tokens
 *         (i.e. providing clients with a suitable server to connect to).
 *         Contains a {@link ServerManager} that is used to monitor the status
 *         of the live servers and retrieve a server's details when necessary.
 *         </p>
 *
 */
public class ActiveLoadBalancer extends AbstractLoadBalancer {

	/**
	 * The frequency at which to send heartbeat messages to the backup nodes. To
	 * be passed to the {@link HeartbeatBroadcaster}
	 */
	private int heartbeatIntervalSecs;

	/**
	 * Flag indicating whether this active load balancer is in a resolution
	 * state. That is, the node has been alerted that the system is in a
	 * multiple-active state and is attempting to resolve this with the other
	 * actives. When this flag is set, any other multiple-active messages are
	 * ignored.
	 */
	private boolean inResolutionState = false;

	/**
	 * Thread that the load balancer message listener is running on. Kept as a
	 * global variable so it can be interrupted easily.
	 */
	private Thread loadBalancerMessageListenerThread;

	/**
	 * Creates a new ActiveLoadBalancer object that acts as the primary load
	 * balancer process in the system. The <code>run</code> method prompts this
	 * object to start listening to requests and responding accordingly.
	 * 
	 * @param acceptPort
	 *            the port on which to accept incoming connection requests
	 * @param remoteLoadBalancers
	 *            the set of remote load balancers in the system
	 * @param servers
	 *            the set of all backend servers in the system
	 * @param nameServiceAddress
	 *            the address of the name service
	 * @param heartbeatIntervalSecs
	 *            the frequency at which to send heartbeat messages to the
	 *            backup nodes
	 */
	public ActiveLoadBalancer(int acceptPort, Set<RemoteLoadBalancer> remoteLoadBalancers, Set<Server> servers,
			InetSocketAddress nameServiceAddress, int heartbeatIntervalSecs) {
		if (remoteLoadBalancers == null || remoteLoadBalancers.isEmpty())
			throw new IllegalArgumentException("Remote load balancer set cannot be null or empty.");
		if (servers == null || servers.isEmpty())
			throw new IllegalArgumentException("Servers set cannot be null or empty.");
		if (nameServiceAddress == null)
			throw new IllegalArgumentException("Name service address cannot be null.");

		this.acceptPort = acceptPort;
		this.remoteLoadBalancers = remoteLoadBalancers;
		this.servers = servers;
		this.nameServiceAddress = nameServiceAddress;
		this.heartbeatIntervalSecs = heartbeatIntervalSecs;
	}

	/*
	 * (non-Javadoc) Called on <code>Thread.start()</code> in order to
	 * initialise a new cached thread pool that delegates incoming connections
	 * to a new {@RunnableRequestProcessor}.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		System.out.println("Initialising active load balancer service on port " + acceptPort + "...");
		ComponentLogger.getInstance().log(LogMessageType.LOAD_BALANCER_ENTERED_ACTIVE);

		notifyNameService();

		ServerSocketChannel serverSocketChannel = ConnectNIO.getServerSocketChannel(acceptPort);
		ExecutorService threadPoolExecutor = Executors.newCachedThreadPool();
		ServerManager serverManager = new ServerManager(servers);
		Thread serverManagerThread = new Thread(serverManager);
		serverManagerThread.start();

		HeartbeatBroadcaster heartbeatBroadcaster = new HeartbeatBroadcaster(remoteLoadBalancers,
				heartbeatIntervalSecs);
		Thread heartbeatBroadcasterThread = new Thread(heartbeatBroadcaster);
		heartbeatBroadcasterThread.start();

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
				boolean isLoadBalancerNode = false;
				String connectingIP = connectRequestSocket.socket().getInetAddress().getHostAddress();
				for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
					if (remoteLoadBalancer.getAddress().getAddress().getHostAddress().equals(connectingIP)) {
						remoteLoadBalancer.setSocketChannel(connectRequestSocket);
						isLoadBalancerNode = true;
						break;
					}
				}
				if (!isLoadBalancerNode) {
					threadPoolExecutor
							.execute(new RunnableActiveRequestProcessor(connectRequestSocket, this, serverManager));
				}
			}
		}
		System.out.println("Active load balancer shutting down...");

		serverManagerThread.interrupt();
		heartbeatBroadcasterThread.interrupt();
		loadBalancerMessageListenerThread.interrupt();
		try {
			serverSocketChannel.close();
		} catch (IOException e) {
		}
		threadPoolExecutor.shutdown();
	}

	/**
	 * Opens a {@link SocketChannel} and sends a <code>HOST_ADDR_NOTIFY</code>
	 * message to the address that is stored for the name service, alerting the
	 * service that this process is acting as the active load balancer.
	 */
	private void notifyNameService() {
		System.out.println("Sending host address notification message to name service...");
		SocketChannel socketChannel = ConnectNIO.getBlockingSocketChannel(nameServiceAddress);
		ByteBuffer buffer = ByteBuffer.allocate(5);
		buffer.put((byte) MessageType.HOST_ADDR_NOTIFY.getValue());
		buffer.putInt(acceptPort);
		buffer.flip();
		try {
			while (buffer.hasRemaining()) {
				socketChannel.write(buffer);
			}
			System.out.println("Notified name service.");
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startLoadBalancerMessageListener(Thread loadBalancerThread) {
		while (!Thread.currentThread().isInterrupted()) {
			for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
				ByteBuffer buffer = ByteBuffer.allocate(100);
				try {
					SocketChannel socketChannel = remoteLoadBalancer.getSocketChannel();
					while (socketChannel.read(buffer) > 0) {
						buffer.flip();
						MessageType messageType = MessageType.values()[buffer.get()];
						switch (messageType) {
						case STATE_REQUEST:
							System.out.println("Received state request");
							buffer.clear();
							buffer.put((byte) MessageType.ACTIVE_NOTIFY.getValue());
							buffer.flip();
							while (buffer.hasRemaining()) {
								socketChannel.write(buffer);
							}
							break;
						case ALIVE_REQUEST:
							System.out.println("Received alive request");
							buffer.clear();
							buffer.put((byte) MessageType.ALIVE_CONFIRM.getValue());
							buffer.flip();
							while (buffer.hasRemaining()) {
								socketChannel.write(buffer);
							}
							break;
						case MULTIPLE_ACTIVES_WARNING:
							if (!inResolutionState) {
								inResolutionState = true;
								CharBuffer charBuffer = Charset.forName("UTF-8").decode(buffer);
								String hostAddressesUnparsed = charBuffer.toString();
								// Pipe delimiter used as it is illegal in
								// hostnames
								// and IP addresses.
								String[] hostAddresses = hostAddressesUnparsed.split("|");

								List<RemoteLoadBalancer> otherActives = new ArrayList<>();
								for (int i = 0; i < hostAddresses.length; i++) {
									for (RemoteLoadBalancer remoteLB : remoteLoadBalancers) {
										if (remoteLB.getAddress().getAddress().getHostAddress()
												.equals(hostAddresses[i])) {
											otherActives.add(remoteLB);
										}
									}
								}
								new Thread(new Runnable() {

									@Override
									public void run() {
										try {
											boolean isStillActive = performEmergencyElection(otherActives);
											if (isStillActive) {
												notifyNameService();
											} else {
												loadBalancerThread.interrupt();
												PassiveLoadBalancer passiveLoadBalancer = LoadBalancer
														.getNewPassiveLoadBalancer();
												Thread loadBalancerThread = new Thread(passiveLoadBalancer);
												loadBalancerThread.start();
												passiveLoadBalancer
														.startLoadBalancerMessageListener(loadBalancerThread);
											}
										} catch (IOException e) {
										}
									}

								}).start();
							}
							break;
						case EMERGENCY_ELECTION_MESSAGE:
							double candidacyValue = buffer.getDouble();
							remoteLoadBalancer.setCandidacyValue(candidacyValue);
							break;
						default:
							break;
						}
					}
				} catch (IOException e) {
					// e.printStackTrace();
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {

			}
		}
	}

	/**
	 * Attempts to resolve a multiple-active system state by performing an
	 * emergency election with any other active nodes in the system.
	 * 
	 * @return true if this node is still the active load balancer, false if it
	 *         should demote itself to the passive state.
	 * @throws IOException
	 */
	private boolean performEmergencyElection(List<RemoteLoadBalancer> otherActives) throws IOException {
		long electionStartTime = System.currentTimeMillis();

		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

		// Use last octet of IP address for election message value
		int ownCandidacyValue = Integer.parseInt(InetAddress.getLocalHost().getHostAddress().split(".")[3]);
		for (RemoteLoadBalancer remoteLoadBalancer : otherActives) {
			ByteBuffer buffer = ByteBuffer.allocate(9);
			SocketChannel socketChannel = remoteLoadBalancer.getSocketChannel();
			Selector writeSelector = Selector.open();
			socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
			if (writeSelector.select() == 0) {
				buffer.put((byte) MessageType.EMERGENCY_ELECTION_MESSAGE.getValue());
				buffer.putDouble(ownCandidacyValue);
				buffer.flip();
				while (buffer.hasRemaining()) {
					socketChannel.write(buffer);
				}
			}
			writeSelector.close();
		}

		boolean isStillActive = true;
		boolean isResolved = false;

		while (!isResolved) {
			int electionMessagesReceived = 0;
			for (RemoteLoadBalancer remoteLoadBalancer : otherActives) {
				if (remoteLoadBalancer.getCandidacyValue() != null) {
					electionMessagesReceived++;
				}
			}
			// Received election messages from all other actives or timed out
			if (electionMessagesReceived == otherActives.size()
					|| System.currentTimeMillis() - electionStartTime > 5000) {
				// Create a fake RemoteLoadbalancer object representing self so
				// we can sort the nodes.
				RemoteLoadBalancer self = new RemoteLoadBalancer(new InetSocketAddress(0));
				self.setCandidacyValue((double) ownCandidacyValue);
				otherActives.add(self);
				Collections.sort(otherActives, new Comparator<RemoteLoadBalancer>() {
					@Override
					public int compare(RemoteLoadBalancer rlb1, RemoteLoadBalancer rlb2) {
						return Double.compare(rlb1.getCandidacyValue(), rlb2.getCandidacyValue());
					}
				});
				if (!otherActives.get(0).equals(self)) {
					isStillActive = false;
				} else {
					otherActives.get(0).setState(LoadBalancerState.ACTIVE);
				}
				for (int i = 1; i < otherActives.size(); i++) {
					otherActives.get(i).setState(LoadBalancerState.PASSIVE);

				}
				isResolved = true;
			}
		}
		// Clear candidacy values until next election
		for (RemoteLoadBalancer remoteLoadBalancer : otherActives) {
			remoteLoadBalancer.setCandidacyValue(null);
		}
		// Wait a second before resetting the resolutuon state flag in case any
		// MULTIPLE_ACTIVES_WARNING messages arrive late.
		new Timer().schedule(new java.util.TimerTask() {
			@Override
			public void run() {
				inResolutionState = false;
			}
		}, 1000);

		return isStillActive;
	}
}
