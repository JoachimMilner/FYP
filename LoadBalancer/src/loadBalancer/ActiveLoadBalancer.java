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
	private int randomBroadcastTimeoutMillis;

	/**
	 * Flag used to indicate that the a timer has been started to broadcast an
	 * active declaration. Stops multiple broadcasts from being sent in the case
	 * that more than 2 load balancer nodes have simultaneously elected
	 * themselves to active.
	 */
	private boolean inBroadcastDelayPeriod = false;

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
	public ActiveLoadBalancer(LoadBalancerConnectionHandler connectionHandler,
			Set<RemoteLoadBalancer> remoteLoadBalancers, Set<Server> servers, InetSocketAddress nameServiceAddress,
			int heartbeatIntervalMillis) {
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

		randomBroadcastTimeoutMillis = ThreadLocalRandom.current().nextInt(heartbeatIntervalMillis * 2);

		ServerManager serverManager = new ServerManager(servers);
		new Thread(serverManager).start();

		connectionHandler.setActive(serverManager);
		notifyNameService();

		HeartbeatBroadcaster heartbeatBroadcaster = new HeartbeatBroadcaster(remoteLoadBalancers,
				heartbeatIntervalMillis, LoadBalancerState.ACTIVE);
		new Thread(heartbeatBroadcaster).start();

		listenForLoadBalancerMessages();

		heartbeatBroadcaster.cancel();
		serverManager.cancel();

		System.out.println("Active load balancer terminating...");
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
							// Received an active declaration from another node
							// - immediately
							// move to passive state
							if (!terminateThread.get()) {
								System.out.println("Received active declaration - demoting to passive state");
								terminateThread.set(true);
								remoteLoadBalancer.setState(LoadBalancerState.ACTIVE);
								new Thread(LoadBalancer.getNewPassiveLoadBalancer()).start();
							}
							break;
						case ACTIVE_ALIVE_CONFIRM:
							if (!inBroadcastDelayPeriod) {
								inBroadcastDelayPeriod = true;
								// Detected another active node - broadcast active
								// declaration after random timeout
								// prompting any other active to demote
								new Timer().schedule(new TimerTask() {
									@Override
									public void run() {
										if (!terminateThread.get()) {
											System.out.println("Detected another active - broadcasting active declaration");
											broadcastActiveDeclaration();
											notifyNameService();
											inBroadcastDelayPeriod = false;
										}
									}
								}, randomBroadcastTimeoutMillis);
							}
							break;
						default:
							break;
						}
					}
				} catch (IOException e) {
					if (e.getMessage() != null
							&& e.getMessage().equals("An existing connection was forcibly closed by the remote host")) {
						try {
							remoteLoadBalancer.getSocketChannel().close();
						} catch (IOException e1) {
						}
					}
				}
			}
		}
	}

	/**
	 * Sends an <code>ACTIVE_DECLARATION</code> message to all other known load
	 * balancer declaring that this node is the only active and any other nodes
	 * in the active state should immediately move to the passive state.
	 */
	private void broadcastActiveDeclaration() {
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
					if (e.getMessage() != null
							&& e.getMessage().equals("An existing connection was forcibly closed by the remote host")) {
						try {
							remoteLoadBalancer.getSocketChannel().close();
						} catch (IOException e1) {
						}
					}
				}
			}
		}
	}
}
