package comms;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import controller.GUIController;
import logging.LogMessageType;
import model.ClientVirtualizer;
import model.NameService;
import model.Server;
import model.SystemModel;

/**
 * @author Joachim
 *         <p>
 *         Used to manage a connection with a remote system component and handle
 *         all log messages.
 *         </p>
 *
 */
public class RunnableMessageProcessor implements Runnable {

	/**
	 * The connected SocketChannel on which to listen for messages.
	 */
	private SocketChannel socketChannel;

	/**
	 * The data model representing all nodes in the system.
	 */
	private SystemModel systemModel;

	/**
	 * The controller object for the GUI.
	 */
	private GUIController controller;

	public RunnableMessageProcessor(SocketChannel socketChannel, SystemModel systemModel, GUIController controller) {
		this.socketChannel = socketChannel;
		this.systemModel = systemModel;
		this.controller = controller;
	}

	@Override
	public void run() {

		while (socketChannel.isConnected()) {

			try {
				ByteBuffer buffer = ByteBuffer.allocate(100);
				int bytesRead = socketChannel.read(buffer);

				if (bytesRead == -1) { // Something went wrong, close channel
										// and terminate
					socketChannel.close();
					break;
				} else {
					buffer.flip();
					LogMessageType messageType = LogMessageType.values()[buffer.get()];
					
					switch (messageType) {
					// COMPONENT REGISTER MESSAGES
					case CLIENT_REGISTER:
						int maxClients = buffer.getInt();
						int minSendFrequency = buffer.getInt();
						int maxSendFrequency = buffer.getInt();
						int minClientRequests = buffer.getInt();
						int maxClientRequests = buffer.getInt();

						ClientVirtualizer clientVirtualizer = new ClientVirtualizer(1,
								(InetSocketAddress) socketChannel.socket().getRemoteSocketAddress());
						clientVirtualizer.setSocketChannel(socketChannel);
						buffer.clear();
						buffer.put((byte) LogMessageType.REGISTRATION_CONFIRM.getValue());
						buffer.putInt(1);
						buffer.flip();
						while (buffer.hasRemaining()) {
							socketChannel.write(buffer);
						}
						controller.setClientConfigOptions(maxClients, minSendFrequency, maxSendFrequency,
								minClientRequests, maxClientRequests);
						controller.appendMainFeed("ClientVirtualizer at "
								+ socketChannel.socket().getRemoteSocketAddress().toString() + " registered.");
						systemModel.setClientVirtualizer(clientVirtualizer);
						break;
					case NAME_SERVICE_REGISTER:
						NameService nameService = new NameService(1,
								(InetSocketAddress) socketChannel.socket().getRemoteSocketAddress());
						nameService.setSocketChannel(socketChannel);
						buffer.clear();
						buffer.put((byte) LogMessageType.REGISTRATION_CONFIRM.getValue());
						buffer.putInt(1);
						buffer.flip();
						while (buffer.hasRemaining()) {
							socketChannel.write(buffer);
						}
						controller.appendMainFeed("NameService at "
								+ socketChannel.socket().getRemoteSocketAddress().toString() + " registered.");
						systemModel.setNameService(nameService);
						break;
					case SERVER_REGISTER:
						int serverID = systemModel.getServers().size() + 1;
						Server server = new Server(serverID,
								(InetSocketAddress) socketChannel.socket().getRemoteSocketAddress());
						server.setSocketChannel(socketChannel);
						buffer.clear();
						buffer.put((byte) LogMessageType.REGISTRATION_CONFIRM.getValue());
						buffer.putInt(serverID);
						buffer.flip();
						while (buffer.hasRemaining()) {
							socketChannel.write(buffer);
						}
						controller.appendMainFeed("Server at "
								+ socketChannel.socket().getRemoteSocketAddress().toString() + " registered.");
						systemModel.getServers().add(server);
						break;
					case LOAD_BALANCER_REGISTER:
						break;

					// LOG MESSAGES
					case CLIENT_MESSAGE_COUNT:
						break;
					case NAME_SERVICE_ADDR_REGISTERED:
						// get componentID (not needed here)
						buffer.getInt();
						int registeredHostPort = buffer.getInt();
						CharBuffer charBuffer = Charset.forName("UTF-8").decode(buffer);
						String registeredHostAddress = charBuffer.toString();
						systemModel.getNameService().setCurrentHostAddress(registeredHostPort, registeredHostAddress);
						controller.appendNameServiceFeed("Load balancer at " + registeredHostAddress + ":" + registeredHostPort + " registered with name service.");
						break;
					case SERVER_CPU_LOAD:
						break;
					case LOAD_BALANCER_PROMOTED:
						break;
					case LOAD_BALANCER_DEMOTED:
						break;
					case LOAD_BALANCER_FAILURE_DETECTED:
						break;
					case LOAD_BALANCER_ELECTION_RESULT:
						break;
					case LOAD_BALANCER_MULTIPLE_ACTIVES_DETECTED:
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
