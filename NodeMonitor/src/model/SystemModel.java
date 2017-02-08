package model;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Joachim
 *         <p>
 * 		The object used to represent the entire system in the NodeMonitor.
 *         Allows an MVC-like structure to be implemented.
 *         </p>
 *
 */
public class SystemModel {

	/**
	 * The ClientVirtualizer in the system.
	 */
	private ClientVirtualizer clientVirtualizer;
	
	/**
	 * The NameService in the system. 
	 */
	private NameService nameService;
	
	/**
	 * The set of Servers in the system. 
	 */
	private Set<Server> servers = new HashSet<>();
	
	/**
	 * The set of LoadBalancers in the system. 
	 */
	private Set<LoadBalancer> loadBalancers = new HashSet<>();

	/**
	 * @return The ClientVirtualizer in the system.
	 */
	public ClientVirtualizer getClientVirtualizer() {
		return clientVirtualizer;
	}

	/**
	 * @param clientVirtualizer The ClientVirtualizer in the system.
	 */
	public void setClientVirtualizer(ClientVirtualizer clientVirtualizer) {
		this.clientVirtualizer = clientVirtualizer;
	}

	/**
	 * @return The NameService in the system. 
	 */
	public NameService getNameService() {
		return nameService;
	}

	/**
	 * @param nameService The NameService in the system. 
	 */
	public void setNameService(NameService nameService) {
		this.nameService = nameService;
	}

	/**
	 * @return The set of Servers in the system. 
	 */
	public Set<Server> getServers() {
		return servers;
	}

	/**
	 * Finds and returns the server object with the given ID, or null if 
	 * the server does not exist in the Set.
	 * @param serverID the ID of the server to be found
	 * @return the server with the given ID, or null if it is not located
	 */
	public Server getServerByID(int serverID) {
		for (Server server : servers) {
			if (server.getComponentID() == serverID) {
				return server;
			}
		}
		return null;
	}

	/**
	 * @return The set of LoadBalancers in the system. 
	 */
	public Set<LoadBalancer> getLoadBalancers() {
		return loadBalancers;
	}
}
