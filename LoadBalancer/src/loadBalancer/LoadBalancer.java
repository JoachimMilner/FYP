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
import logging.ComponentLogger;
import logging.LogMessageType;

public class LoadBalancer {

	public static void main(String[] args) {
		LoadBalancer instance = new LoadBalancer();
		instance.launch(args);
	}
	
	private void launch(String[] args) {
		Configurations configs = new Configurations();
		
		int acceptPort = 0;
		Set<Server> servers = new HashSet<>();
		Set<RemoteLoadBalancer> remoteLoadBalancers = new HashSet<>();
		InetSocketAddress nameServiceAddress = null;
		int defaultServerTokenExpiry = 0;
		int nodeMonitorPort = 0;
		String nodeMonitorIP = "";
		try
		{
		    HierarchicalConfiguration<ImmutableNode> config = configs.xml("lbConfig.xml");
		    
		    // Accept port
		    acceptPort = config.getInt("connectPort");
		    
		    // List of backend servers
			List<HierarchicalConfiguration<ImmutableNode>> serverNodes = config.configurationsAt("servers.server");
			for(HierarchicalConfiguration<ImmutableNode> server : serverNodes) {
				String ipAddress = server.getString("ipAddress");
				int port = server.getInt("port");
				servers.add(new Server(new InetSocketAddress(ipAddress, port)));
			}
			
			// List of other load balancer nodes in the system
			List<HierarchicalConfiguration<ImmutableNode>> remoteLBNodes = config.configurationsAt("remoteLoadBalancers.remoteNode");
			for(HierarchicalConfiguration<ImmutableNode> remoteLoadBalancer : remoteLBNodes) {
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
		}
		catch (ConfigurationException cex)
		{
		    cex.printStackTrace();
		    return;
		}
		// Set Server class default token expiration value
		Server.setDefaultTokenExpiration(defaultServerTokenExpiry);
		
		ComponentLogger.setMonitorAddress(new InetSocketAddress(nodeMonitorIP, nodeMonitorPort));
		ComponentLogger.getInstance().registerWithNodeMonitor(LogMessageType.LOAD_BALANCER_REGISTER);
		
		// Call determine LB state
		AbstractLoadBalancer loadBalancer = new ActiveLoadBalancer(acceptPort, remoteLoadBalancers, servers, nameServiceAddress);
		new Thread(loadBalancer).start();
	}
}
