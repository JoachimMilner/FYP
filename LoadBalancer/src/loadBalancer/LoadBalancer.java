package loadBalancer;

import java.net.InetSocketAddress;
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
			for (HierarchicalConfiguration<ImmutableNode> remoteLoadBalancer : remoteLBNodes) {
				String ipAddress = remoteLoadBalancer.getString("ipAddress");
				int port = remoteLoadBalancer.getInt("port");
				remoteLoadBalancers.add(new RemoteLoadBalancer(new InetSocketAddress(ipAddress, port)));
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
		} catch (ConfigurationException e) {
			e.printStackTrace();
			return;
		}
		// Set Server class default token expiration value
		Server.setDefaultTokenExpiration(defaultServerTokenExpiry);

		ComponentLogger.setMonitorAddress(new InetSocketAddress(nodeMonitorIP, nodeMonitorPort));
		ComponentLogger.getInstance().registerWithNodeMonitor(LogMessageType.LOAD_BALANCER_REGISTER);

		connectionHandler = new LoadBalancerConnectionHandler(acceptPort, remoteLoadBalancers);
		new Thread(connectionHandler).start();

		Thread loadBalancerThread = new Thread(getNewPassiveLoadBalancer());
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
