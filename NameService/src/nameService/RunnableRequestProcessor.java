package nameService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import connectionUtils.MessageType;

/**
 * @author Joachim
 *         <p>
 * 		Class for handling incoming requests. An instance is created by an
 *         {@link AddressResolutionService} object for each request received.
 *         </p>
 *
 */
public class RunnableRequestProcessor implements Runnable {

	/**
	 * The SocketChannel that was passed in from the
	 * {@link AddressResolutionService} to get the incoming request on.
	 */
	private SocketChannel socketChannel;

	/**
	 * The AddressResolutionService that created this instance. This
	 * RunnableRequestProcessor will call its methods to get or update the
	 * <code>hostAddress</code>.
	 */
	private AddressResolutionService addressResolutionService;

	/**
	 * Creates an {@link RunnableRequestProcessor} instance that will process an
	 * incoming request. When the <code>run</code> method is called it will
	 * listen for either a <code>HOST_ADDR_NOTIFY</code> request from the
	 * primary load balancer or a <code>HOST_ADDR_REQUEST</code> request from a
	 * client.
	 * 
	 * @param socketChannel
	 * @param addressResolutionService
	 */
	public RunnableRequestProcessor(SocketChannel socketChannel, AddressResolutionService addressResolutionService) {
		if (socketChannel == null || !socketChannel.isConnected())
			throw new IllegalArgumentException("SocketChannel must be initialised and connected");
		if (addressResolutionService == null)
			throw new IllegalArgumentException("AddressResolutionService cannot be null.");

		this.socketChannel = socketChannel;
		this.addressResolutionService = addressResolutionService;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run() Listens for an incoming requests on the
	 * specified SocketChannel and then processes accordingly. Assumes that the
	 * message is either from a load balancer notifying that it has been set as
	 * the primary or a client requesting the primary load balancer's address.
	 */
	@Override
	public void run() {
		while (socketChannel.isConnected()) {
			try {
				ByteBuffer buffer = ByteBuffer.allocate(16);
				int bytesRead = socketChannel.read(buffer);

				if (bytesRead == -1) { // Something went wrong, close channel
										// and terminate
					socketChannel.close();
					break;
				} else {
					buffer.flip();
					MessageType messageType = MessageType.values()[buffer.get()];

					switch (messageType) {
					case HOST_ADDR_NOTIFY:
						String hostAddress = socketChannel.socket().getInetAddress().getHostName();
						int hostPort = socketChannel.socket().getPort();
						addressResolutionService.setHostAddress(hostAddress);
						addressResolutionService.setHostPort(hostPort);
						System.out.println("Received host address notification message from host at: " + hostAddress + ":" + hostPort);
						break;
					case HOST_ADDR_REQUEST:
						CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
						buffer.clear();
						buffer.put((byte)MessageType.HOST_ADDR_RESPONSE.getValue());
						buffer.putInt(addressResolutionService.getHostPort());
						buffer.flip();
						while(buffer.hasRemaining()) {
							socketChannel.write(buffer);
						}
						socketChannel.write(encoder.encode(CharBuffer.wrap(addressResolutionService.getHostAddress())));
						System.out.println("Received host address request from client.");
						break;
					default:
						// Received a bad request
						throw new IOException("Bad MessageType received");
					}
				}
			} catch (IOException e) {
				// e.printStackTrace();

				break;
			}
		}
	}

}
