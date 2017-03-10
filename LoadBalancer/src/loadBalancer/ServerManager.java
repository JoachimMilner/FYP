package loadBalancer;

import java.io.IOException;
import java.util.Set;

import commsModel.Server;

/**
 * @author Joachim
 *         <p>
 *         Object for managing a set of servers. Implements the {@link Runnable}
 *         interface and when started in a new thread, will periodically update
 *         the load status of each of the servers in its <code>servers</code>
 *         list, allowing the objects to retrieve a suitable server when
 *         required.
 *         </p>
 */
public class ServerManager implements Runnable {

	/**
	 * The remote servers that this object manages.
	 */
	private Set<Server> servers;

	/**
	 * Creates a new ServerManager object containing the specified Set of
	 * {@link Server} objects.
	 * 
	 * @param servers
	 *            the remote servers that this object will manage.
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
			// Final declaration used for < Java 8 compatibility
			for (final Server server : servers) {
				new Thread(new Runnable() {
					
					@Override
					public void run() {	
						// Give a very short timeout to server connect as we are assuming servers are robust and reliable
						if (server.connect(5)) {
							server.updateServerState();
						}
					}

				}).start();
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		disconnectServers();
	}

	/**
	 * This method selects the {@Server} with the lowest CPU load that is
	 * currently known to be in a live state. It then calls
	 * <code>calculateTokenExpiry</code> on the selected {@Server} and returns
	 * the object.
	 * 
	 * @return an available {@Server} to be passed onto a client.
	 */
	public synchronized Server getAvailableServer() {
		Server availableServer = null;
		boolean foundLiveServer = false;
		for (Server server : servers) {
			if (server.isConnected()) {
				if (!foundLiveServer) {
					availableServer = server;
					foundLiveServer = true;
				} else if (server.getCPULoad() < availableServer.getCPULoad()) {
					availableServer = server;
				}
			}
		}
		if (availableServer != null) {
			availableServer.calculateTokenExpiry();
		}
		return availableServer;
	}
	
	/**
	 * Closes all socket channels that are connected to servers.
	 */
	private void disconnectServers() {
		for (Server server : servers) {
			try {
				server.getSocketChannel().close();
			} catch (IOException e) {
				
			}
		}
	}
}
