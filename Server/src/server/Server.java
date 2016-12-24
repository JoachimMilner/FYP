package server;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class Server {

	public static void main(String[] args) {
		Server instance = new Server();
		instance.launch(args);
	}
	
	private void launch(String[] args) {
		Configurations configs = new Configurations();
		int threadPoolSize = 0;
		int connectPort = 0;
		try
		{
		    XMLConfiguration config = configs.xml("serverConfig.xml");
		    threadPoolSize = config.getInt("threadPoolSize");
		    connectPort = config.getInt("connectPort");
		}
		catch (ConfigurationException cex)
		{
		    cex.printStackTrace();
		    return;
		}
		ThreadPooledServer server = new ThreadPooledServer(threadPoolSize, connectPort);
		new Thread(server).start();
	}

}
