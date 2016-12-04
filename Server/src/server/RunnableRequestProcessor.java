package server;

import java.nio.channels.SocketChannel;

/**
 * @author Joachim
 * Class used to processing received client requests. When a request is received at the <code>ServerSocketChannel</code>
 * in the {@link ThreadPooledServer}, it is delegated to RunnableRequestProcessor which will handle the request processing.
 */
public class RunnableRequestProcessor implements Runnable {
	
	/**
	 * The accepted client's SocketChannel.
	 */
	private SocketChannel socketChannel;
	
	/**
	 * Constructor for creating a new RunnableRequestProcessor. Accepts a {@link SocketChannel}, reads the request bytes from it,
	 * processes a response and sends it. 
	 * @param socketChannel
	 */
	public RunnableRequestProcessor(SocketChannel socketChannel) {
		if (socketChannel == null || !socketChannel.isConnected()) throw new IllegalArgumentException("Null or disconnected SocketChannel.");
		
		this.socketChannel = socketChannel;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

}
