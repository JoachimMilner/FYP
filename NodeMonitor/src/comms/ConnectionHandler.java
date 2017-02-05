package comms;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import connectionUtils.ConnectNIO;
import controller.GUIController;
import model.SystemModel;

/**
 * @author Joachim
 *         <p>
 *         Acts as a thread pooled server (using a ServerSocketChannel) to
 *         listen for and accept incoming requests from system components.
 *         Starts a new {@link RunnableMessageProcessor} for each new
 *         connection.
 *         </p>
 *
 */
public class ConnectionHandler implements Runnable {

	/**
	 * The data model representing all nodes in the system.
	 */
	private SystemModel systemModel;
	
	/**
	 * The controller object for the GUI.
	 */
	private GUIController controller;

	/**
	 * Constructs a new ConnectionHandler with the specified SystemModel that
	 * will listen for incoming component registration requests and then
	 * delegate the connection to a {@link RunnableMessageProcessor}.
	 * 
	 * @param systemModel
	 */
	public ConnectionHandler(SystemModel systemModel, GUIController controller) {
		this.systemModel = systemModel;
		this.controller = controller;
	}

	@Override
	public void run() {
		controller.appendMainFeed("Initialising socket");
		ServerSocketChannel serverSocketChannel = ConnectNIO.getServerSocketChannel(8000);
		ExecutorService threadPoolExecutor = Executors.newCachedThreadPool();
		while (!Thread.currentThread().isInterrupted()) {
			SocketChannel connectRequestSocket = null;
			try {
				connectRequestSocket = serverSocketChannel.accept();
			} catch (IOException e) {
				if (Thread.currentThread().isInterrupted()) {
					break;
				}
				e.printStackTrace();
			}
			if (connectRequestSocket != null) {
				threadPoolExecutor.execute(new RunnableMessageProcessor(connectRequestSocket, systemModel, controller));
			}
		}
		
		try {
			serverSocketChannel.close();
		} catch (IOException e) {
		}
		threadPoolExecutor.shutdown();
	}

}
