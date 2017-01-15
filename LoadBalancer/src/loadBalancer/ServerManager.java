package loadBalancer;

import java.util.Set;

import commsModel.Server;

/**
 * @author Joachim
 *         <p>
 * 		Object for managing a set of servers. Implements the {@link Runnable}
 *         interface and when started in a new thread, will periodically update
 *         the load status of each of the servers in its <code>servers</code>
 *         list, allowing the objects to retrieve a suitable server when required.
 *         </p>
 */
public class ServerManager implements Runnable {

	/**
	 * The remote servers that this object manages.
	 */
	private Set<Server> servers;
	
	
	/**
	 * Creates a new ServerManager object containing the specified Set of {@link Server}
	 * objects.
	 * @param servers the remote servers that this object will manage. 
	 */
	public ServerManager(Set<Server> servers) {
		if (servers == null || servers.isEmpty()) 
			throw new IllegalArgumentException("Server Set must be initialised and contain at least one server.");
		
		this.servers = servers;
	}


	/*
	 * (non-Javadoc) To be called on <code>Thread.start()</code> to periodically
	 * update the CPU load status for each remote server.
	 *
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		while (!Thread.currentThread().isInterrupted()) {
			for (Server server : servers) {
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						server.updateServerState();
					}
					
				}).start();
			}
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		
	}
	
	
	public Server getAvailableServer() {
		Server availableServer = null;
		boolean foundLiveServer = false;
		for (Server server : servers) {
			if (server.isAlive()) {
				if (!foundLiveServer) {
					availableServer = server;
					foundLiveServer = true;
				} else if (server.getCPULoad() < availableServer.getCPULoad()) {
					availableServer = server;
				}
			}
		}
		return availableServer;
	}
}
