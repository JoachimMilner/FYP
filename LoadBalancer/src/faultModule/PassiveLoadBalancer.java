package faultModule;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

import commsModel.LoadBalancerState;
import commsModel.RemoteLoadBalancer;
import commsModel.Server;
import connectionUtils.MessageType;
import loadBalancer.AbstractLoadBalancer;
import loadBalancer.LoadBalancer;
import loadBalancer.LoadBalancerConnectionHandler;
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
	 * The timeout duration used to calculate the heartbeat timeout intervals
	 * for the active and backup node.
	 */
	private int defaultTimeoutMillis;

	/**
	 * The default timeout duration that this object will use when monitoring
	 * the active load balancer's heartbeat.
	 */
	private int activeTimeoutMillis;

	/**
	 * The default timeout duration that this object will use when monitoring
	 * the elected backup load balancer's heartbeat.
	 */
	private int backupTimeoutMillis;
	
	/**
	 * The interval at which this load balancer will send heartbeat messages if
	 * it is the elected backup. 
	 */
	private int backupHeartbeatIntervalMillis;

	/**
	 * Flag indicating whether this node is the currently elected backup.
	 */
	private boolean isElectedBackup = false;

	/**
	 * Flag used to indicate that the system is currently in an
	 * election-in-progress state and should handle election messages
	 * accordingly.
	 */
	private boolean preElectionInProgress = false;

	/**
	 * If this load balancer is the elected backup, this is used as the node's
	 * heartbeat broadcaster.
	 */
	private HeartbeatBroadcaster backupHeartbeatBroadcaster;

	/**
	 * Flag indicating that an emergency election has been initiated due to
	 * multiple active load balancers being detected in the system.
	 */
	private boolean emergencyElectionInProgress = false;

	/**
	 * The remote node currently acting as the primary load balancer. Reference
	 * maintained here for ease of use.
	 */
	private RemoteLoadBalancer currentActive;

	/**
	 * The timer used to monitor the active load balancer's heartbeat.
	 */
	private Timer activeHeartbeatTimer;

	/**
	 * The timer used to schedule periodic calculation of this load balancer's
	 * average latency to the servers.
	 */
	private Timer serverLatencyProcessorTimer;

	/**
	 * The timer used to monitor the backup (passive) load balancer's heartbeat.
	 */
	private Timer backupHeartbeatTimer;
	
	/**
	 * Timer used to schedule a re-election when this node is the elected backup. 
	 */
	private Timer reElectionTimer;

	/**
	 * The most recently calculated value for this node's average latency to the
	 * servers. Used as the election ID for this load balancer.
	 */
	private double averageServerLatency;

	/**
	 * Flag indicating that failure of the active load balancer has been
	 * detected.
	 */
	//private boolean activeFailureDetected = false;

	/**
	 * Flag indicating that this passive node is expecting an
	 * <code>ACTIVE_ALIVE_CONFIRM</code> message from the active load balancer.
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
	 * @param defaultTimeoutMillis
	 *            the default time duration when monitoring the active load
	 *            balancer's heartbeat.
	 */
	public PassiveLoadBalancer(LoadBalancerConnectionHandler connectionHandler,
			Set<RemoteLoadBalancer> remoteLoadBalancers, Set<Server> servers, int defaultTimeoutMillis) {
		if (remoteLoadBalancers == null || remoteLoadBalancers.isEmpty())
			throw new IllegalArgumentException("Remote load balancer set cannot be null or empty.");
		if (servers == null || servers.isEmpty())
			throw new IllegalArgumentException("Servers set cannot be null or empty.");
		if (defaultTimeoutMillis < 1)
			throw new IllegalArgumentException("Default timeout value must be a positive, non-zero value.");

		this.connectionHandler = connectionHandler;
		this.remoteLoadBalancers = remoteLoadBalancers;
		this.servers = servers;
		this.defaultTimeoutMillis = defaultTimeoutMillis;
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
		System.out.println("Initialising passive load balancer service...");
		ComponentLogger.getInstance().log(LogMessageType.LOAD_BALANCER_ENTERED_PASSIVE);

		connectionHandler.setPassive();

		// Add a random time period between 0 and defaultTimeoutMillis to the
		// current value to address concurrency issues.
		activeTimeoutMillis = defaultTimeoutMillis + ThreadLocalRandom.current().nextInt(defaultTimeoutMillis);
		backupTimeoutMillis = defaultTimeoutMillis * 6;
		backupHeartbeatIntervalMillis = defaultTimeoutMillis * 4;

		// Initialise heartbeat monitors & start average server latency
		// calculator scheduler
		startActiveHeartbeatTimer();
		startServerLatencyProcessorTimer();
		startBackupHeartbeatTimer();

		listenForLoadBalancerMessages();

		System.out.println("Passive load balancer terminating...");

		shutdownThreads();
	}
	
	/**
	 * Cancels all timers and threads if not already done.
	 */
	private void shutdownThreads() {
		if (backupHeartbeatBroadcaster != null) {
			backupHeartbeatBroadcaster.cancel();
		}
		if (activeHeartbeatTimer != null) {
			activeHeartbeatTimer.cancel();
		}
		if (serverLatencyProcessorTimer != null) {
			serverLatencyProcessorTimer.cancel();
		}
		if (backupHeartbeatTimer != null) {
			backupHeartbeatTimer.cancel();
		}
		if (reElectionTimer != null) {
			reElectionTimer.cancel();
		}
	}

	@Override
	public void listenForLoadBalancerMessages() {
		while (!terminateThread.get()) {
			int activeCount = 0;
			int backupCount = 0;
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
							buffer.put((byte) MessageType.PASSIVE_NOTIFY.getValue());
							buffer.put((byte) (isElectedBackup ? 1 : 0));
							buffer.flip();
							while (buffer.hasRemaining()) {
								socketChannel.write(buffer);
							}
							break;
						case ACTIVE_ALIVE_CONFIRM:
							remoteLoadBalancer.setState(LoadBalancerState.ACTIVE);
							if (currentActive == null) {
								System.out.println("Identified active at:" + remoteLoadBalancer.getAddress().getHostString());
								currentActive = remoteLoadBalancer;
							}
							resetActiveHeartbeatTimer();

							if (expectingAliveConfirmation) {
								if (remoteLoadBalancer.equals(currentActive)) {
									receivedAliveConfirmation = true;
								}
								expectingAliveConfirmation = false;
							}
							break;
						case BACKUP_ALIVE_CONFIRM:
							if (!remoteLoadBalancer.isElectedBackup()) {
								System.out.println("Identified backup at:" + remoteLoadBalancer.getAddress().getHostString());
							}
							remoteLoadBalancer.setIsElectedBackup(true);
							resetBackupHeartbeatTimer();
							break;
						case ACTIVE_HAS_FAILED:

							break;
						case ACTIVE_IS_ALIVE:

							break;
						case ELECTION_MESSAGE:
							remoteLoadBalancer.setCandidacyValue(buffer.getDouble());
							if (!preElectionInProgress) {
								System.out.println("Initiated pre-election");
								initiatePreElection();
							}
							break;
						default:
							break;
						}
					}
				} catch (IOException e) {
					if (e != null
							& e.getMessage().equals("An existing connection was forcibly closed by the remote host")) {
						remoteLoadBalancer.setSocketChannel(null);
					}
				}
				if (remoteLoadBalancer.getState().equals(LoadBalancerState.ACTIVE)) {
					activeCount++;
				} else if (remoteLoadBalancer.isElectedBackup()) {
					backupCount++;
				}
			}
			if (!emergencyElectionInProgress && activeCount > 1) {
				initiateEmergencyElection();
			}
			if (backupCount > 1) {
				System.out.println("Initiated pre-election");
				initiatePreElection();
			}
		}
	}

	/**
	 * Starts a new timer for the active load balancer that will begin the fault
	 * tolerance protocol after it has run for the specified duration.
	 */
	private void startActiveHeartbeatTimer() {
		TimerTask timerTask = new TimerTask() {

			@Override
			public void run() {
				if (currentActive == null) {
					// No active was present - elevate own state.
					ComponentLogger.getInstance().log(LogMessageType.LOAD_BALANCER_NO_ACTIVE_DETECTED);
					System.out.println("Detected absence of an active node.");
					new Thread(LoadBalancer.getNewActiveLoadBalancer()).start();
					terminateThread.set(true);
					return;
				}

				// Suspected failure - attempt to contact active
				ComponentLogger.getInstance().log(LogMessageType.LOAD_BALANCER_ACTIVE_FAILURE_DETECTED);
				System.out.println("Active load balancer failure detected.");
				Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

				boolean isConnected = false;
				for (int i = 0; i < 10; i++) {
					if (currentActive.connect(0)) {
						isConnected = true;
						break;
					}
					try {
						Thread.sleep(activeTimeoutMillis / 100);
					} catch (InterruptedException e) {

					}
				}
				if (!isConnected) {
					// activeFailureDetected = true;
					if (isElectedBackup) {
						new Thread(LoadBalancer.getNewActiveLoadBalancer()).start();
						terminateThread.set(true);
					} else {
						currentActive = remoteLoadBalancers.stream().filter(x -> x.isElectedBackup()).findFirst().get();
						currentActive.setIsElectedBackup(false);
						resetActiveHeartbeatTimer();
					}
				} else {
					ByteBuffer buffer = ByteBuffer.allocate(1);
					buffer.put((byte) MessageType.ALIVE_REQUEST.getValue());
					buffer.flip();
					try {
						while (buffer.hasRemaining()) {
							currentActive.getSocketChannel().write(buffer);
						}
						expectingAliveConfirmation = true;
					} catch (IOException e) {
						// e.printStackTrace();
					}
					new Timer().schedule(new TimerTask() {
						@Override
						public void run() {
							if (receivedAliveConfirmation) {

							} else {
								if (isElectedBackup) {
									new Thread(LoadBalancer.getNewActiveLoadBalancer()).start();
									terminateThread.set(true);
								} else {
									currentActive = remoteLoadBalancers.stream().filter(x -> x.isElectedBackup())
											.findFirst().get();
									currentActive.setIsElectedBackup(false);
									resetActiveHeartbeatTimer();
								}
							}
							expectingAliveConfirmation = false;
						}
					}, activeTimeoutMillis);
				}
			}

		};
		activeHeartbeatTimer = new Timer();
		activeHeartbeatTimer.schedule(timerTask, activeTimeoutMillis);
	}

	/**
	 * Resets the heartbeat timer for the active load balancer - called whenever
	 * a heartbeat is received from the active load balancer.
	 */
	private void resetActiveHeartbeatTimer() {
		activeHeartbeatTimer.cancel();
		startActiveHeartbeatTimer();
	}

	/**
	 * Instantiates the <code>serverLatencyProcessorTimer</code> to periodically
	 * run a {@link TimerTask} that calculates this load balancer's average ping
	 * time to the servers on the network (at 3x the timeout duration for the
	 * backup load balancer).
	 */
	private void startServerLatencyProcessorTimer() {
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				double totalLatency = 0;
				for (Server server : servers) {
					long pingStart = System.currentTimeMillis();
					try {
						InetAddress.getByName(server.getAddress().getHostName()).isReachable(defaultTimeoutMillis);
					} catch (IOException e) {
					}
					long pingTime = System.currentTimeMillis() - pingStart;
					totalLatency += pingTime;
				}
				// Add small value to average server latency in case two passives calculate the same average.
				averageServerLatency = (totalLatency / servers.size()) + ThreadLocalRandom.current().nextDouble(0.001);
				System.out.println("Average server latency: " + averageServerLatency + "ms");
			}
		};
		serverLatencyProcessorTimer = new Timer();
		serverLatencyProcessorTimer.scheduleAtFixedRate(timerTask, 0, backupTimeoutMillis * 3);
	}

	/**
	 * Starts a timer that will prompt an election for a passive backup when the
	 * timeout value is reached.
	 */
	private void startBackupHeartbeatTimer() {
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				if (remoteLoadBalancers.size() == 1) {
					// No need to elect or monitor server latency
					isElectedBackup = true;
					serverLatencyProcessorTimer.cancel();
					ComponentLogger.getInstance().log(LogMessageType.LOAD_BALANCER_ELECTED_AS_BACKUP);
					System.out.println("Elected as backup");
				} else if (!preElectionInProgress) {
					ComponentLogger.getInstance().log(LogMessageType.LOAD_BALANCER_BACKUP_FAILURE_DETECTED);
					System.out.println("Detected failed/absent backup - initiating election");
					initiatePreElection();
				}
			}
		};

		backupHeartbeatTimer = new Timer();
		backupHeartbeatTimer.schedule(timerTask, backupTimeoutMillis);
	}

	/**
	 * Resets the timer used to monitor the live-state of the elected backup
	 * load balancer. Should be called whenever a heartbeat is received.
	 */
	private void resetBackupHeartbeatTimer() {
		backupHeartbeatTimer.cancel();
		startBackupHeartbeatTimer();
	}
	
	/**
	 * 
	 */
	private void startReElectionTimer() {
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				ComponentLogger.getInstance().log(LogMessageType.LOAD_BALANCER_PROMPTED_RE_ELECTION);
				System.out.println("Prompting for a re-election");
				initiatePreElection();
			}
		};
		
		reElectionTimer = new Timer();
		reElectionTimer.schedule(timerTask, backupTimeoutMillis * 5);
	}

	/**
	 * Initiates a pre-election by broadcasting this load balancer's average
	 * latency to the set of system servers and setting this node to the
	 * election-in-progress state. Then starts a timer task that will identify
	 * the new backup at the end of the timer.
	 */
	private void initiatePreElection() {
		preElectionInProgress = true;
		backupHeartbeatTimer.cancel();
		reElectionTimer.cancel();
		// Broadcast election ordinality
		for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
			if (remoteLoadBalancer.isConnected() && remoteLoadBalancer.getState().equals(LoadBalancerState.PASSIVE)) {
				ByteBuffer buffer = ByteBuffer.allocate(9);
				buffer.put((byte) MessageType.ELECTION_MESSAGE.getValue());
				buffer.putDouble(averageServerLatency);
				buffer.flip();
				try {
					while (buffer.hasRemaining()) {
						remoteLoadBalancer.getSocketChannel().write(buffer);
					}
				} catch (IOException e) {
				}
			}
		}

		// TimerTask created that will determine the election results after the
		// timeout occurs.
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				RemoteLoadBalancer lowestLatencyCandidate = null;
				for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
					if (remoteLoadBalancer.getState().equals(LoadBalancerState.PASSIVE)
							&& remoteLoadBalancer.getCandidacyValue() != null) {
						if (lowestLatencyCandidate == null
								&& remoteLoadBalancer.getCandidacyValue() < averageServerLatency) {
							lowestLatencyCandidate = remoteLoadBalancer;
						} else if (lowestLatencyCandidate != null && remoteLoadBalancer
								.getCandidacyValue() < lowestLatencyCandidate.getCandidacyValue()) {
							lowestLatencyCandidate = remoteLoadBalancer;
						} else {
							remoteLoadBalancer.setIsElectedBackup(false);
						}
					}
				}

				// Didn't get a lowest latency election message so assume this
				// load balancer is now the backup
				if (lowestLatencyCandidate == null) {
					backupHeartbeatTimer.cancel();
					isElectedBackup = true;
					backupHeartbeatBroadcaster = new HeartbeatBroadcaster(remoteLoadBalancers, backupHeartbeatIntervalMillis,
							LoadBalancerState.PASSIVE);
					new Thread(backupHeartbeatBroadcaster).start();
					
					// Start timer for next pre-election
					startReElectionTimer();
					
					ComponentLogger.getInstance().log(LogMessageType.LOAD_BALANCER_ELECTED_AS_BACKUP);
					System.out.println("Elected as backup");
				} else {
					lowestLatencyCandidate.setIsElectedBackup(true);
					isElectedBackup = false;
					if (backupHeartbeatBroadcaster != null) {
						backupHeartbeatBroadcaster.cancel();
					}
					resetBackupHeartbeatTimer();
				}

				// Clear candidacy values for future elections
				for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
					remoteLoadBalancer.setCandidacyValue(null);
				}
				preElectionInProgress = false;
			}
		};

		new Timer().schedule(timerTask, defaultTimeoutMillis);
	}

	/**
	 * Initiates an emergency election prompted by a scenario in which multiple
	 * load balancers have elevated themselves to the active state. Sends
	 * <code>MULTIPLE_ACTIVES_WARNING</code> messages to all active nodes,
	 * forcing them to immediately re-elect a single active as quickly as
	 * possible. A timer is started in which the actives (should) decide which
	 * node will remain as the active.
	 */
	private void initiateEmergencyElection() {
		emergencyElectionInProgress = true;
		ComponentLogger.getInstance().log(LogMessageType.LOAD_BALANCER_MULTIPLE_ACTIVES_DETECTED);
		System.out.println("Detected multiple actives - initiating emergency election.");
		for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
			if (remoteLoadBalancer.getState().equals(LoadBalancerState.ACTIVE)) {
				ByteBuffer buffer = ByteBuffer.allocate(1);
				buffer.put((byte) MessageType.MULTIPLE_ACTIVES_WARNING.getValue());
				buffer.flip();
				try {
					remoteLoadBalancer.getSocketChannel().write(buffer);
				} catch (IOException e) {
				}
			}
		}

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				// Clear remote load balancer states so we don't initiate
				// another election
				for (RemoteLoadBalancer remoteLoadBalancer : remoteLoadBalancers) {
					remoteLoadBalancer.setState(LoadBalancerState.PASSIVE);
				}
				currentActive = null;

				// End emergency election period
				emergencyElectionInProgress = false;
			}
		}, defaultTimeoutMillis);
	}
}
