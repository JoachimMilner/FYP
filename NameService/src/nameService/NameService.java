package nameService;

import java.net.InetSocketAddress;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import logging.ComponentLogger;
import logging.LogMessageType;

public class NameService {

	public static void main(String[] args) {
		Configurations configs = new Configurations();
		int acceptPort = 0;
		int nodeMonitorPort = 0;
		String nodeMonitorIP = "";
		try
		{
		    XMLConfiguration config = configs.xml("nameServiceConfig.xml");
		    acceptPort = config.getInt("acceptPort");
		    nodeMonitorPort = config.getInt("nodeMonitorPort");
		    nodeMonitorIP = config.getString("nodeMonitorIP");
		}
		catch (ConfigurationException cex)
		{
		    cex.printStackTrace();
		    return;
		}
		ComponentLogger.setMonitorAddress(new InetSocketAddress(nodeMonitorIP, nodeMonitorPort));
		ComponentLogger.getInstance().registerWithNodeMonitor(LogMessageType.NAME_SERVICE_REGISTER);
		AddressResolutionService addressResolutionService = new AddressResolutionService(acceptPort);
		addressResolutionService.startService();
	}
}
