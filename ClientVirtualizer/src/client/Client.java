package client;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

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
		try
		{
		    XMLConfiguration config = configs.xml("clientConfig.xml");
		    maxClients = config.getInt("maxClients");
		    minSendFrequencyMs = config.getInt("minSendFrequencyMs");
		    maxSendFrequencyMs = config.getInt("maxSendFrequencyMs");
		    minClientRequests = config.getInt("minClientRequests");
		    maxClientRequests = config.getInt("maxClientRequests");
		}
		catch (ConfigurationException cex)
		{
		    cex.printStackTrace();
		    return;
		}
		VirtualClientManager clientManager = new VirtualClientManager(maxClients, minSendFrequencyMs, maxSendFrequencyMs, minClientRequests, maxClientRequests);
		clientManager.initialiseClientPool();
	}
}
