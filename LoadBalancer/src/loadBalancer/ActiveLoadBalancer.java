package loadBalancer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

import commsModel.LoadBalancerState;
import commsModel.RemoteLoadBalancer;
import commsModel.Server;
import connectionUtils.ConnectNIO;
import connectionUtils.MessageType;
import faultModule.HeartbeatBroadcaster;
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
	 * The address of the name resolution service.
	 */
	protected InetSocketAddress nameServiceAddress;
	
	/**
	 * The frequency at which to send heartbeat messages to the backup nodes. To
	 * be passed to the {@link HeartbeatBroadcaster}
	 */
	private int heartbeatIntervalMillis;
	
	/**
	 * Randomly generated timeout that this node will wait before broadcasting
	 * an <code>ACTIVE_DECLARATION</code> message. Used to avoid concurrency
	 * issues.
	 */
	private int randomBroadcastTimoutMillis;

	/**
	 * Flag indicating whether this active load balancer is in a resolution
	 * state. That is, the node has been alerted that the system is in a
	 * multiple-active state and is attempting to resolve this with the other
	 * actives. When this flag is set, any other multiple-active messages are
	 * ignored.
	 */
	private boolean inResolutionState = false;

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
	 * @param heartbeatIntervalMillis
	 *            the frequency at which to send heartbeat messages to the
	 *            backup nodes
	 */
	public ActiveLoadBalancer(LoadBalancerConnectionHandler connectionHandler, Set<RemoteLoadBalancer> remoteLoadBalancers, Set<Server> servers,
			InetSocketAddress nameServiceAddress, int heartbeatIntervalMillis) {
		if (remoteLoadBalancers == null || remoteLoadBalancers.isEmpty())
			throw new IllegalArgumentException("Remote load balancer set cannot be null or empty.");
		if (servers == null || servers.isEmpty())
			throw new IllegalArgumentException("Servers set cannot be null or empty.");
		if (nameServiceAddress == null)
			throw new IllegalArgumentException("Name service address cannot be null.");

		this.connectionHandler = connectionHandler;
		this.remoteLoadBalancers = remoteLoadBalancers;
		this.servers = servers;
		this.nameServiceAddress = nameServiceAddress;
		this.heartbeatIntervalMillis = heartbeatIntervalMillis;
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
		System.out.println("Initialising active load balancer service...");
		ComponentLogger.getInstance().log(LogMessageType.LOAD_BALANCER_ENTERED_ACTIVE);
		
		//broadcastActiveDeclaration();
		
		randomBroadcastTimoutMillis = ThreadLocalRandom.current().nextInt(heartbeatIntervalMillis);
		
		ServerManager serverManager = new ServerManager(servers);
		new Thread(serverManager).start();

		connectionHandler.setActive(serverManager);
		notifyNameService();
		
		HeartbeatBroadcaster heartbeatBroadcaster = new HeartbeatBroadcaster(remoteLoadBalancers,
				heartbeatIntervalMillis, LoadBalancerState.ACTIVE);
		new Thread(heartbeatBroadcaster).start();
		
		listenForLoadBalancerMessages();

		System.out.println("Active load balancer terminating...");

		serverManager.cancel();
		heartbeatBroadcaster.cancel();
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
		buffer.putInt(connectionHandler.getAcceptPort());
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
	public void listenForLoadBalancerMessages() {
		while (!terminateThread.get()) {
			for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
				ByteBuffer buffer = ByteBuffer.allocate(100);
				try {
					
					if (!remoteLoadBalancer.isConnected()) {
						continue;
					}
					SocketChannel socketChannel = remoteLoadBalancer.getSocketChannel();
					while (socketChannel.read(buffer) > 0) {
						buffer.flip();
						MessageType messageType = MessageType.values()[buffer.get()];
						switch (messageType) {
						case STATE_REQUEST:
							System.out.println("Received state request");
							buffer.clear();
							buffer.put((byte) MessageType.ACTIVE_DECLARATION.getValue());
							buffer.flip();
							while (buffer.hasRemaining()) {
								socketChannel.write(buffer);
							}
							break;
						case ALIVE_REQUEST:
							System.out.println("Received alive request");
							buffer.clear();
							buffer.put((byte) MessageType.ACTIVE_ALIVE_CONFIRM.getValue());
							buffer.flip();
							while (buffer.hasRemaining()) {
								socketChannel.write(buffer);
							}
							break;
						case ACTIVE_DECLARATION:
							// Received an active declaration from another node - immediately 
							// move to passive state
							System.out.println("Received active declaration - demoting to passive state");
							terminateThread.set(true);
							remoteLoadBalancer.setState(LoadBalancerState.ACTIVE);
							new Thread(LoadBalancer.getNewPassiveLoadBalancer()).start();
							break;
						case ACTIVE_ALIVE_CONFIRM:
							// Detected another active node - broadcast active declaration after random timeout
							// prompting any other active to demote
							System.out.println("Detected another active - broadcasting active declaration");
							broadcastActiveDeclaration();
							break;
/*						case MULTIPLE_ACTIVES_WARNING:
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
												new Thread(LoadBalancer.getNewPassiveLoadBalancer()).start();
												terminateThread.set(true);
											}
										} catch (IOException e) {
										}
									}

								}).start();
							}
							break;*/
						case EMERGENCY_ELECTION_MESSAGE:
							double candidacyValue = buffer.getDouble();
							remoteLoadBalancer.setCandidacyValue(candidacyValue);
							if (!inResolutionState) {
								inResolutionState = true;
								boolean remainAsActive = buffer.get() != 0;
								if (remainAsActive) {
									notifyNameService();
									
									// Maintain resolution state for a second so we don't keep receiving 
									// emergency election messages and notifying name service.
									new Timer().schedule(new java.util.TimerTask() {
										@Override
										public void run() {
											inResolutionState = false;
										}
									}, 1000);
								} else {
									new Thread(LoadBalancer.getNewPassiveLoadBalancer()).start();
									terminateThread.set(true);
								}
							}
							break;
						default:
							break;
						}
					}
				} catch (IOException e) {
					if (e != null & e.getMessage().equals("An existing connection was forcibly closed by the remote host")) {
						remoteLoadBalancer.setSocketChannel(null);
					}
				}
			}
			if (!terminateThread.get()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
	
				}
			}
		}
	}
	
	/**
	 * Sends an <code>ACTIVE_DECLARATION</code> message to all other known
	 * load balancer declaring that this node is the only active and any other
	 * nodes in the active state should immediately move to the passive state. 
	 */
	private void broadcastActiveDeclaration() {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				ByteBuffer buffer = ByteBuffer.allocate(1);
				for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
					if (remoteLoadBalancer.isConnected()) {
						buffer.clear();
						buffer.put((byte) MessageType.ACTIVE_DECLARATION.getValue());
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
			}
		}, randomBroadcastTimoutMillis);
	}

	/**
	 * Attempts to resolve a multiple-active system state by performing an
	 * emergency election with any other active nodes in the system.
	 * 
	 * @return true if this node is still the active load balancer, false if it
	 *         should demote itself to the passive state.
	 * @throws IOException
	 */
/*	private boolean performEmergencyElection(List<RemoteLoadBalancer> otherActives) throws IOException {
		long electionStartTime = System.currentTimeMillis();

		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

		// Use last octet of IP address for election message value
		int ownCandidacyValue = Integer.parseInt(InetAddress.getLocalHost().getHostAddress().split("\\.")[3]);
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
	}*/
}
