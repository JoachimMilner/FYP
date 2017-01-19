package nameService;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class NameService {

	public static void main(String[] args) {
		Configurations configs = new Configurations();
		int acceptPort = 0;
		try
		{
		    XMLConfiguration config = configs.xml("nameServiceConfig.xml");
		    acceptPort = config.getInt("acceptPort");
		}
		catch (ConfigurationException cex)
		{
		    cex.printStackTrace();
		    return;
		}
		
		AddressResolutionService addressResolutionService = new AddressResolutionService(acceptPort);
		addressResolutionService.startService();
	}
}
