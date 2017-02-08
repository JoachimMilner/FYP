package server;

import java.net.InetSocketAddress;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import logging.ComponentLogger;
import logging.LogMessageType;

public class Server {

	public static void main(String[] args) {
		Server instance = new Server();
		instance.launch(args);
	}
	
	private void launch(String[] args) {
		Configurations configs = new Configurations();
		int threadPoolSize = 0;
		int connectPort = 0;
		int nodeMonitorPort = 0;
		String nodeMonitorIP = "";
		try
		{
		    XMLConfiguration config = configs.xml("serverConfig.xml");
		    threadPoolSize = config.getInt("threadPoolSize");
		    connectPort = config.getInt("connectPort");
		    nodeMonitorPort = config.getInt("nodeMonitorPort");
		    nodeMonitorIP = config.getString("nodeMonitorIP");
		}
		catch (ConfigurationException cex)
		{
		    cex.printStackTrace();
		    return;
		}
		ComponentLogger.setMonitorAddress(new InetSocketAddress(nodeMonitorIP, nodeMonitorPort));
		ComponentLogger.getInstance().registerWithNodeMonitor(LogMessageType.SERVER_REGISTER);
		ThreadPooledServer server = new ThreadPooledServer(threadPoolSize, connectPort);
		new Thread(server).start();
	}

}
