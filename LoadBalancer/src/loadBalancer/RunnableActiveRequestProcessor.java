package loadBalancer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import commsModel.Server;
import connectionUtils.MessageType;

/**
 * @author Joachim
 *         <p>
 *         This class is used for processing incoming messages to this load
 *         balancer instance. The run method will prompt the object instance to
 *         read data from the supplied {@link SocketChannel} and then process
 *         the message contents and send a response where appropriate.
 *         </p>
 */
public class RunnableActiveRequestProcessor extends AbstractRequestProcessor {

	/**
	 * The server manager that this object will use to get available server
	 * details to send to clients.
	 */
	private ServerManager serverManager;

	/**
	 * Creates a new RunnableActiveRequestProcessor that will handle incoming message requests 
	 * on the specified {@link SocketChannel}. 
	 * @param socketChannel
	 * @param activeLoadBalancer
	 * @param serverManager
	 */
	public RunnableActiveRequestProcessor(SocketChannel socketChannel, ActiveLoadBalancer activeLoadBalancer,
			ServerManager serverManager) {
		if (socketChannel == null || !socketChannel.isConnected())
			throw new IllegalArgumentException("Null or disconnected SocketChannel.");
		if (activeLoadBalancer == null)
			throw new IllegalArgumentException("ActiveLoadBalancer cannot be null.");
		if (serverManager == null)
			throw new IllegalArgumentException("ServerManager cannot be null.");

		this.socketChannel = socketChannel;
		this.loadBalancer = activeLoadBalancer;
		this.serverManager = serverManager;
	}

	@Override
	public void run() {
		while (socketChannel.isConnected()) {
			try {
				ByteBuffer buffer = ByteBuffer.allocate(28);
				int bytesRead = socketChannel.read(buffer);

				if (bytesRead == -1) { // Something went wrong, close channel and terminate
					socketChannel.close();
					break;
				} else {
					buffer.flip();
					MessageType messageType = MessageType.values()[buffer.get()];
					processMessage(messageType);
				}
			} catch (IOException e) {
				//e.printStackTrace();
				
				break;
			}
		}
		System.out.println("Client disconected.");

	}

	@Override
	protected void processMessage(MessageType messageType) {
		try {
			ByteBuffer buffer;
			switch (messageType) {
			case AVAILABLE_SERVER_REQUEST:
				buffer = ByteBuffer.allocate(38);
				CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
				Server server = serverManager.getAvailableServer();
				buffer.clear();
				buffer.put((byte) MessageType.SERVER_TOKEN.getValue());
				buffer.putLong(server.getTokenExpiry());
				buffer.putInt(server.getAddress().getPort());
				buffer.put(encoder.encode(CharBuffer.wrap(server.getAddress().getHostString())));
				buffer.flip();
				while (buffer.hasRemaining()) {
					socketChannel.write(buffer);
				}
				break;
			case ALIVE_REQUEST:
				buffer = ByteBuffer.allocate(1);
				buffer.put((byte) MessageType.ALIVE_CONFIRM.getValue());
				buffer.flip();
				while (buffer.hasRemaining()){
					socketChannel.write(buffer);
				}
				break;
			default:
				// Received a bad request
				throw new IOException("Bad MessageType received");
			}
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}

}
