package client;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import logging.ComponentLogger;
import logging.LogMessageType;

public class Client {

	public static void main(String[] args) {
		Client instance = new Client();
		instance.launch(args);

	}

	private void launch(String[] args) {
		Configurations configs = new Configurations();
		int maxClients = 0;
		int minSendFrequencyMs = 0;
		int maxSendFrequencyMs = 0;
		int minClientRequests = 0;
		int maxClientRequests = 0;
		String nameServiceIP = "";
		int nameServicePort = 0;
		int nodeMonitorPort = 0;
		String nodeMonitorIP = "";
		try {
			XMLConfiguration config = configs.xml("clientConfig.xml");

			maxClients = config.getInt("maxClients");
			minSendFrequencyMs = config.getInt("minSendFrequencyMs");
			maxSendFrequencyMs = config.getInt("maxSendFrequencyMs");
			minClientRequests = config.getInt("minClientRequests");
			maxClientRequests = config.getInt("maxClientRequests");

			nameServiceIP = config.getString("nameServiceIP");
			nameServicePort = config.getInt("nameServicePort");

			nodeMonitorPort = config.getInt("nodeMonitorPort");
			nodeMonitorIP = config.getString("nodeMonitorIP");
		} catch (ConfigurationException cex) {
			cex.printStackTrace();
			return;
		}
		ComponentLogger.setMonitorAddress(new InetSocketAddress(nodeMonitorIP, nodeMonitorPort));
		SocketChannel nodeMonitorSocketChannel = ComponentLogger.getInstance().registerWithNodeMonitor(
				LogMessageType.CLIENT_REGISTER, maxClients, minSendFrequencyMs, maxSendFrequencyMs, minClientRequests,
				maxClientRequests);

		VirtualClientManager clientManager = new VirtualClientManager(maxClients, minSendFrequencyMs,
				maxSendFrequencyMs, minClientRequests, maxClientRequests,
				new InetSocketAddress(nameServiceIP, nameServicePort));
		clientManager.initialiseClientPool();
		clientManager.listenForConfigurationUpdates(nodeMonitorSocketChannel);
	}
}
