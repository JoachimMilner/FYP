package loadBalancer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;

import commsModel.RemoteLoadBalancer;
import commsModel.Server;
import faultModule.PassiveLoadBalancer;
import logging.ComponentLogger;
import logging.LogMessageType;

public class LoadBalancer {

	// Configu variables stored here statically so they don't have to be re-read
	// from the xml again.
	private static LoadBalancerConnectionHandler connectionHandler;
	private static Set<Server> servers = new HashSet<>();
	private static Set<RemoteLoadBalancer> remoteLoadBalancers = new HashSet<>();
	private static InetSocketAddress nameServiceAddress = null;
	private static int defaultServerTokenExpiry = 0;
	private static int nodeMonitorPort = 0;
	private static String nodeMonitorIP = "";
	private static int heartbeatIntervalMillis = 0;
	private static int heartbeatTimeoutMillis = 0;

	public static void main(String[] args) {
		LoadBalancer instance = new LoadBalancer();
		instance.launch(args);
	}

	private void launch(String[] args) {
		Configurations configs = new Configurations();
		int acceptPort = 0;
		boolean forceStartAsActive = false;
		try {
			HierarchicalConfiguration<ImmutableNode> config = configs.xml("lbConfig.xml");

			// Accept port
			acceptPort = config.getInt("connectPort");

			// List of backend servers
			List<HierarchicalConfiguration<ImmutableNode>> serverNodes = config.configurationsAt("servers.server");
			for (HierarchicalConfiguration<ImmutableNode> server : serverNodes) {
				String ipAddress = server.getString("ipAddress");
				int port = server.getInt("port");
				servers.add(new Server(new InetSocketAddress(ipAddress, port)));
			}

			// List of other load balancer nodes in the system
			List<HierarchicalConfiguration<ImmutableNode>> remoteLBNodes = config
					.configurationsAt("remoteLoadBalancers.remoteNode");
			for (HierarchicalConfiguration<ImmutableNode> remoteLoadBalancerNode : remoteLBNodes) {
				String ipAddress = remoteLoadBalancerNode.getString("ipAddress");
				int port = remoteLoadBalancerNode.getInt("port");
				RemoteLoadBalancer remoteLoadBalancer = new RemoteLoadBalancer(new InetSocketAddress(ipAddress, port));
				try {
					remoteLoadBalancer.setConnectionPrecedence(Integer.parseInt(ipAddress.split("\\.")[3]));
				} catch (NumberFormatException e) {
				}
				remoteLoadBalancers.add(remoteLoadBalancer);
			}

			// Name service address
			String nameServiceIP = config.getString("nameServiceAddress.ipAddress");
			int nameServicePort = config.getInt("nameServiceAddress.port");
			nameServiceAddress = new InetSocketAddress(nameServiceIP, nameServicePort);

			// Default server token expiration length
			defaultServerTokenExpiry = config.getInt("defaultServerTokenExpiry");

			// NodeMonitor address
			nodeMonitorPort = config.getInt("nodeMonitorPort");
			nodeMonitorIP = config.getString("nodeMonitorIP");

			// Heartbeat values
			heartbeatIntervalMillis = config.getInt("heartbeatIntervalMillis");
			heartbeatTimeoutMillis = config.getInt("heartbeatTimeoutMillis");
			
			// Check for active force start
			forceStartAsActive = config.getBoolean("startAsActive");
		} catch (ConfigurationException e) {
			e.printStackTrace();
			return;
		}
		// Set Server class default token expiration value
		Server.setDefaultTokenExpiration(defaultServerTokenExpiry);

		ComponentLogger.setMonitorAddress(new InetSocketAddress(nodeMonitorIP, nodeMonitorPort));
		SocketChannel loggerSocketChannel = ComponentLogger.getInstance().registerWithNodeMonitor(LogMessageType.LOAD_BALANCER_REGISTER);
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			
			@Override
			public void run() {
				ComponentLogger.getInstance().log(LogMessageType.LOAD_BALANCER_TERMINATED);
			}
			
		}));
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					char a = (char) System.in.read();
					if (a == 'c') {
						System.exit(0);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}).start();
		
		connectionHandler = new LoadBalancerConnectionHandler(acceptPort, remoteLoadBalancers);
		

		Thread loadBalancerThread;
		if (forceStartAsActive) {
			loadBalancerThread = new Thread(getNewActiveLoadBalancer());
			try {
				Selector readSelector = Selector.open();
				loggerSocketChannel.register(readSelector, SelectionKey.OP_READ);
				System.out.println("Force-started in active state...Waiting to be released");
				if (readSelector.select(1000 * 60 * 5) != 0) {
					new Thread(connectionHandler).start();
				}
			} catch (IOException e) {
			}
		} else {
			new Thread(connectionHandler).start();
			loadBalancerThread = new Thread(getNewPassiveLoadBalancer());
		}
		loadBalancerThread.start();
	}

	public static ActiveLoadBalancer getNewActiveLoadBalancer() {
		return new ActiveLoadBalancer(connectionHandler, remoteLoadBalancers, servers, nameServiceAddress,
				heartbeatIntervalMillis);
	}

	public static PassiveLoadBalancer getNewPassiveLoadBalancer() {
		return new PassiveLoadBalancer(connectionHandler, remoteLoadBalancers, servers, heartbeatTimeoutMillis);
	}
}
