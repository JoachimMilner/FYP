package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import connectionUtils.ConnectNIO;
import connectionUtils.MessageType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Joachim
 *         <p>
 *         This class represents a single virtualized client process and can
 *         only be instantiated by using <code>initialiseClientPool</code> on a
 *         {@link VirtualClientManager} object.
 *         </p>
 */
public class RunnableClientProcess implements Runnable {

	/**
	 * The static address of the address resolution service.
	 */
	private InetSocketAddress nameServiceAddress;

	/**
	 * The address that is currently stored for the active load balancer.
	 */
	private InetSocketAddress loadBalancerAddress;

	/**
	 * The VirtualClientManager instance that this object will use to update the
	 * total requests sent and responses received.
	 */
	private VirtualClientManager clientManager;

	/**
	 * All SocketChannels that have been or are in use by this client.
	 * Previously used channels are stored here to ensure that any server
	 * responses sent after the token expires can still be received.
	 */
	private List<SocketChannel> socketChannels = new ArrayList<>();

	/**
	 * The socket that this virtual client is currently using to send and
	 * receive messages.
	 */
	private SocketChannel currentSocketChannel;

	/**
	 * The frequency that this client will send requests at in milliseconds.
	 */
	private int sendFrequencyMs;

	/**
	 * The total number of requests that this virtual client will send to the
	 * server.
	 */
	private int totalRequests;

	/**
	 * 
	 */
	private int messagesReceived = 0;

	/**
	 * The token that is currently stored for this client. Contains the details
	 * of the most recent remote server that was provided by the load balancer,
	 * to connect and send requests to.
	 */
	private ServerToken currentServerToken;

	/**
	 * Creates a RunnableClientProcess which will connect to the supplied
	 * <code>nameServiceAddress</code> to retrieve the load balancer's address,
	 * then request an available server. When <code>run</code> is called, this
	 * object will create a non-blocking socket to send the supplied number of
	 * requests at the specified <code>sendFrequencyMs</code>. When all requests
	 * have been sent, the virtual client will continue listening to messages
	 * for a few seconds, then close the {@link SocketChannel} and the host
	 * thread will die.
	 * 
	 * @param nameServiceAddress
	 *            the static address for the name service.
	 * @param clientManager
	 *            the {@link VirtualClientManager} that created this
	 *            RunnableClientProcess.
	 * @param sendFrequencyMs
	 *            the frequency at which the client will send requests in
	 *            milliseconds.
	 * @param totalRequests
	 *            the number of TCP requests that will be sent before this
	 *            client terminates
	 * @throws IllegalArgumentException
	 *             if the <code>serverAddress</code> or
	 *             <code>clientManager</code> passed in is null.
	 */
	public RunnableClientProcess(InetSocketAddress nameServiceAddress, VirtualClientManager clientManager,
			int sendFrequencyMs, int totalRequests) {
		if (nameServiceAddress == null)
			throw new IllegalArgumentException("serverAddress cannot be null.");
		if (clientManager == null)
			throw new IllegalArgumentException("clientManager cannot be null.");
		if (totalRequests < 1)
			throw new IllegalArgumentException("numberOfRequests must be non-zero.");

		this.nameServiceAddress = nameServiceAddress;
		this.clientManager = clientManager;
		this.sendFrequencyMs = sendFrequencyMs;
		this.totalRequests = totalRequests;
	}

	/*
	 * (non-Javadoc) Called on RunnableClientProcess thread creation - transmits
	 * requests to the server at the specified frequency until the request limit
	 * is reached.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// TODO
		// Get load balancer address from name service then get server details
		// from LB.
		requestHostNameResolution();
		requestServerToken();
		currentSocketChannel = ConnectNIO.getNonBlockingSocketChannel(currentServerToken.getServerAddress());
		socketChannels.add(currentSocketChannel);
		int requestsSent = 0;
		while (!Thread.currentThread().isInterrupted() && requestsSent < totalRequests) {
			sendDataRequest();
			requestsSent++;
			checkForMessages();

			if (requestsSent < totalRequests) {
				try {
					Thread.sleep(sendFrequencyMs);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

			if (System.currentTimeMillis() / 1000 >= currentServerToken.getTokenExpiry()) {
				// Token has expired, get a new one.
				// Keep old socket in memory to check there are no messages
				// remaining
				socketChannels.add(currentSocketChannel);
				requestServerToken();
				if (!currentSocketChannel.socket().getInetAddress().getHostAddress()
						.equals(currentServerToken.getServerAddress().getAddress().getHostAddress())) {
					currentSocketChannel = ConnectNIO
							.getNonBlockingSocketChannel(currentServerToken.getServerAddress());
				}
			}
		}

		// Keep client alive until all responses have been received.
		while (messagesReceived < requestsSent) {
			checkForMessages();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		clientManager.notifyThreadFinished();

		try {
			for (SocketChannel socketChannel : socketChannels) {
				socketChannel.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method sends a <code>HOST_ADDR_REQUEST</code> message to the address
	 * that is stored for the name service every second until it receives a
	 * response containing the load balancer's address.
	 */
	private void requestHostNameResolution() {
		// System.out.println("Connecting to name service to resolve load
		// balancer address...");
		boolean receivedResponse = false;

		try (SocketChannel socketChannel = ConnectNIO.getNonBlockingSocketChannel(nameServiceAddress);
				Selector readSelector = Selector.open();) {
			socketChannel.register(readSelector, SelectionKey.OP_READ);
			ByteBuffer buffer = ByteBuffer.allocate(17);
			while (!receivedResponse && !Thread.currentThread().isInterrupted()) {
				// Request load balancer address
				buffer.put((byte) MessageType.HOST_ADDR_REQUEST.getValue());
				buffer.flip();
				while (buffer.hasRemaining()) {
					socketChannel.write(buffer);
				}

				// Listen for response
				buffer.clear();
				if (readSelector.select(1000) != 0) {
					// System.out.println("Received response for host name
					// resolution");
					socketChannel.read(buffer);
					buffer.flip();
					MessageType messageType = MessageType.values()[buffer.get()];

					int loadBalancerPort = 0;
					String loadBalancerIP = "";

					loadBalancerPort = buffer.getInt();
					CharBuffer charBuffer = Charset.forName("UTF-8").decode(buffer);
					loadBalancerIP = charBuffer.toString();

					if (messageType.equals(MessageType.HOST_ADDR_RESPONSE) && loadBalancerPort != 0
							&& !loadBalancerIP.equals("")) {
						loadBalancerAddress = new InetSocketAddress(loadBalancerIP, loadBalancerPort);
						receivedResponse = true;
					}
				} else {
					System.out.println("Failed to contact name service, retrying...");
				}
			}
		} catch (IOException e) {
			// e.printStackTrace();
		}
	}

	/**
	 * Attempts to request the details of an available server to use from the
	 * load balancer, assuming that the load balancer's address has been
	 * retrieved from the name service. Periodically requests unless the load
	 * balancer appears to be down, in which case the the name service is
	 * contacted to resolve the LB address.
	 */
	private void requestServerToken() {
		// System.out.println("Connecting to load balancer to request available
		// server...");
		boolean receivedResponse = false;
		int retryCount = 0;
		SocketChannel socketChannel = ConnectNIO.getNonBlockingSocketChannel(loadBalancerAddress);

		try (Selector readSelector = Selector.open();) {
			socketChannel.register(readSelector, SelectionKey.OP_READ);
			ByteBuffer buffer = ByteBuffer.allocate(28);
			while (!receivedResponse && !Thread.currentThread().isInterrupted()) {
				// Request available server details
				buffer.put((byte) MessageType.AVAILABLE_SERVER_REQUEST.getValue());
				buffer.flip();
				while (buffer.hasRemaining()) {
					socketChannel.write(buffer);
				}

				// Listen for response
				buffer.clear();
				if (readSelector.select(1000) != 0) {
					// System.out.println("Received server token from load
					// balancer");
					socketChannel.read(buffer);
					buffer.flip();
					MessageType messageType = MessageType.values()[buffer.get()];

					long tokenExpiry = 0;
					int serverPort = 0;
					String serverIP = "";

					tokenExpiry = buffer.getLong();
					serverPort = buffer.getInt();
					CharBuffer charBuffer = Charset.forName("UTF-8").decode(buffer);
					serverIP = charBuffer.toString();

					if (messageType.equals(MessageType.SERVER_TOKEN) && tokenExpiry != 0 && serverPort != 0
							&& !serverIP.equals("")) {
						currentServerToken = new ServerToken(tokenExpiry, new InetSocketAddress(serverIP, serverPort));
						receivedResponse = true;
					}
				} else {
					System.out.println("Failed to contact load balancer, retrying...");
					// failed to contact LB
					retryCount++;
					if (retryCount > 2) {
						System.out
								.println("Failed to connect to load balancer 3 times, retrying address resolution...");
						requestHostNameResolution();
						socketChannel.close();
						socketChannel = ConnectNIO.getNonBlockingSocketChannel(nameServiceAddress);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				socketChannel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Generates 10 random long values and sends to the server on the provided
	 * <code>SocketChannel</code>.
	 * 
	 * @throws IOException
	 */
	private void sendDataRequest() {
		ByteBuffer buffer = ByteBuffer.allocate(81);
		// buffer.clear();
		buffer.put((byte) MessageType.CLIENT_REQUEST.getValue());
		for (int i = 0; i < 10; i++) {
			long random = (long) (10000 + Math.random() * 100000);
			buffer.putLong(random);
		}
		buffer.flip();
		while (buffer.hasRemaining()) {
			try {
				currentSocketChannel.write(buffer);
			} catch (IOException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		clientManager.incrementTotalRequestsSent();
		// System.out.println("Virtual Client (ID:" +
		// Thread.currentThread().getId() + ") sent request to server.");
	}

	/**
	 * Checks this virtual client's {@link SocketChannel}s for messages and
	 * increments the {@link VirtualClientManager}'s
	 * <code>totalResponsesReceived</code> value if a valid message is received.
	 */
	private void checkForMessages() {
		for (SocketChannel socketChannel : socketChannels) {
			ByteBuffer buffer = ByteBuffer.allocate(81);
			try {
				while (socketChannel.read(buffer) > 0) {
					buffer.flip();
					MessageType messageType = MessageType.values()[buffer.get()];
					if (messageType.equals(MessageType.SERVER_RESPONSE)) {
						int i = 0;
						for (; i < 10; i++) {
							try {
								buffer.getLong();
							} catch (BufferUnderflowException e) {
								// Bad/Incomplete message received
								e.printStackTrace();
								break;
							}
						}

						if (i == 10) { // Messaged received
							clientManager.incrementTotalResponsesReceived();
							messagesReceived++;
						}
					}
				}
			} catch (IOException e) {
				// e.printStackTrace();
			}
		}
	}

	/**
	 * @author Joachim
	 *         <p>
	 * 		Class used to model a server token received from the load
	 *         balancer so it can be handled more easily within a
	 *         {@link RunnableClientProcess}.
	 *         </p>
	 *
	 */
	private class ServerToken {

		/**
		 * The expiration timestamp of this server token, represented in unix
		 * seconds.
		 */
		private long tokenExpiry;

		/**
		 * The address representing the remote server to connect to.
		 */
		private InetSocketAddress serverAddress;

		/**
		 * Constructs a new ServerToken object with the specified expiry and
		 * server address.
		 * 
		 * @param tokenExpiry
		 *            the expiration of this token in unix seconds.
		 * @param serverAddress
		 *            the address of the remote server.
		 */
		public ServerToken(long tokenExpiry, InetSocketAddress serverAddress) {
			this.tokenExpiry = tokenExpiry;
			this.serverAddress = serverAddress;
		}

		/**
		 * @return the expiration time of this server token.
		 */
		public long getTokenExpiry() {
			return tokenExpiry;
		}

		/**
		 * @return the address of the remote server.
		 */
		public InetSocketAddress getServerAddress() {
			return serverAddress;
		}
	}
}
