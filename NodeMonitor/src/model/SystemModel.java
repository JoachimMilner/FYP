package model;

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
	private Set<Server> servers;
	
	/**
	 * The set of LoadBalancers in the system. 
	 */
	private Set<LoadBalancer> loadBalancers;

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
	 * @param servers The set of Servers in the system. 
	 */
	public void setServers(Set<Server> servers) {
		this.servers = servers;
	}

	/**
	 * @return The set of LoadBalancers in the system. 
	 */
	public Set<LoadBalancer> getLoadBalancers() {
		return loadBalancers;
	}

	/**
	 * @param loadBalancers The set of LoadBalancers in the system. 
	 */
	public void setLoadBalancers(Set<LoadBalancer> loadBalancers) {
		this.loadBalancers = loadBalancers;
	}
}
