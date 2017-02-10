package model;

import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Joachim
 *         <p>
 *         The object used to represent the entire system in the NodeMonitor.
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
	 * @param clientVirtualizer
	 *            The ClientVirtualizer in the system.
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
	 * @param nameService
	 *            The NameService in the system.
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
	 * Finds and returns the server object with the given ID, or null if the
	 * server does not exist in the Set.
	 * 
	 * @param serverID
	 *            the ID of the server to be found
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

	/**
	 * Removes the component with the specified {@link SocketChannel} from
	 * the system model by settings its reference to null (prompting GC). This
	 * is so that the {@link RunnableMessageProcessor} can call this method if a
	 * remote component's {@link SocketChannel} disconnects.
	 * 
	 * @param socketChannel
	 *            the SocketChannel of the component that this method will attempt to find
	 *            and remove.
	 * @return true if a component with the specified SocketChannel was found and remove,
	 *         else false.
	 */
	public boolean removeComponentBySocketChannel(SocketChannel socketChannel) {
		if (clientVirtualizer != null && socketChannel.equals(clientVirtualizer.getSocketChannel())) {
			clientVirtualizer = null;
			return true;
		} else if (nameService != null && socketChannel.equals(nameService.getSocketChannel())) {
			nameService = null;
			return true;
		}
		for (Iterator<Server> iterator = servers.iterator(); iterator.hasNext();) {
			Server server = iterator.next();
			if (socketChannel.equals(server.getSocketChannel())) {
				//iterator.remove();
				return true;
			}
		}
		for (Iterator<LoadBalancer> iterator = loadBalancers.iterator(); iterator.hasNext();) {
			LoadBalancer loadBalancer = iterator.next();
			if (socketChannel.equals(loadBalancer.getSocketChannel())) {
				//iterator.remove();
				return true;
			}
		}
		return false;
	}
}
