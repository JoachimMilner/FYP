package comms;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import controller.GUIController;
import log.LoggerUtility;
import logging.LogMessageType;
import model.ClientVirtualizer;
import model.LoadBalancer;
import model.LoadBalancer.LoadBalancerState;
import model.NameService;
import model.Server;
import model.CPULoadReading;
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
		// Variables initialised here so we can log when if the component
		// disconnects.
		int componentID = 0;
		String componentName = "";
		while (socketChannel.isConnected() && !Thread.currentThread().isInterrupted()) {
			try {
				ByteBuffer buffer = ByteBuffer.allocate(100);
				int bytesRead = socketChannel.read(buffer);

				if (bytesRead == -1) { // Something went wrong, close channel
										// and terminate
					socketChannel.close();
					break;
				} else {
					long messageReceivedTime = System.currentTimeMillis();
					buffer.flip();
					LogMessageType messageType = LogMessageType.values()[buffer.get()];

					switch (messageType) {
					// COMPONENT REGISTER MESSAGES
					case CLIENT_REGISTER:
						componentName = "ClientVirtualizer";
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
						componentName = "NameService";
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
						componentName = "Server";
						Server server = null;
						
						// Check if this is a server reconnecting
						boolean isReconnectedServer = false;
						String serverOutputInfoString = "";
						for (Server s : systemModel.getServers()) {
							if (socketChannel.socket().getInetAddress().getHostAddress()
									.equals(s.getSocketChannel().socket().getInetAddress().getHostAddress())) {
								server = s;
								isReconnectedServer = true;
								serverOutputInfoString = server.getComponentID() + " reconnected.";
								break;
							}
						}
						
						if (!isReconnectedServer) {
							int serverID = systemModel.getServers().size() + 1;
							server = new Server(serverID,
									(InetSocketAddress) socketChannel.socket().getRemoteSocketAddress());
							serverOutputInfoString = "at " + socketChannel.socket().getRemoteSocketAddress().toString() + " registered.";
							controller.addDataSeriesToGraph(server.getSeries());
							systemModel.getServers().add(server);
						}

						server.setSocketChannel(socketChannel);
						buffer.clear();
						buffer.put((byte) LogMessageType.REGISTRATION_CONFIRM.getValue());
						buffer.putInt(server.getComponentID());
						buffer.flip();
						while (buffer.hasRemaining()) {
							socketChannel.write(buffer);
						}
						
						controller.appendMainFeed("Server " + serverOutputInfoString);  
						componentID = server.getComponentID();
						break;
					case LOAD_BALANCER_REGISTER:
						componentName = "LoadBalancer";
						LoadBalancer loadBalancer = null;
						
						// Check if this is a server reconnecting
						boolean isReconnectedLoadBalancer = false;
						String loadBalancerOutputInfoString = "";
						for (LoadBalancer lb : systemModel.getLoadBalancers()) {
							if (socketChannel.socket().getInetAddress().getHostAddress()
									.equals(lb.getSocketChannel().socket().getInetAddress().getHostAddress())) {
								loadBalancer = lb;
								isReconnectedLoadBalancer = true;
								loadBalancerOutputInfoString = loadBalancer.getComponentID() + " reconnected.";
								break;
							}
						}
						
						if (!isReconnectedLoadBalancer) {
							int loadBalancerID = systemModel.getLoadBalancers().size() + 1;
							loadBalancer = new LoadBalancer(loadBalancerID, (InetSocketAddress) socketChannel.socket().getRemoteSocketAddress());
							loadBalancerOutputInfoString = "at " + socketChannel.socket().getRemoteSocketAddress().toString() + " registered.";
							systemModel.getLoadBalancers().add(loadBalancer);
						}
						
						loadBalancer.setSocketChannel(socketChannel);
						buffer.clear();
						buffer.put((byte) LogMessageType.REGISTRATION_CONFIRM.getValue());
						buffer.putInt(loadBalancer.getComponentID());
						buffer.flip();
						while (buffer.hasRemaining()) {
							socketChannel.write(buffer);
						}
						
						controller.appendMainFeed("LoadBalancer " + loadBalancerOutputInfoString);
						componentID = loadBalancer.getComponentID();
						break;

					// LOG MESSAGES
					case CLIENT_MESSAGE_COUNT:
						// get componentID (not needed here)
						buffer.getInt();
						int requestsSent = buffer.getInt();
						int responsesReceived = buffer.getInt();
						int totalClientConnectFailures = buffer.getInt();
						systemModel.getClientVirtualizer().setTotalRequestsSent(requestsSent);
						systemModel.getClientVirtualizer().setTotalResponsesReceived(responsesReceived);
						systemModel.getClientVirtualizer().setTotalClientConnectFailures(totalClientConnectFailures);
						controller.refreshClientRequestResponseCount();
						break;
					case NAME_SERVICE_ADDR_REGISTERED:
						// get componentID (not needed here)
						buffer.getInt();
						int registeredHostPort = buffer.getInt();
						CharBuffer charBuffer = Charset.forName("UTF-8").decode(buffer);
						String registeredHostAddress = charBuffer.toString();
						systemModel.getNameService().setCurrentHostAddress(registeredHostPort, registeredHostAddress);
						controller.appendNameServiceFeed("LoadBalancer at " + registeredHostAddress + ":"
								+ registeredHostPort + " registered.");
						break;
					case SERVER_CPU_LOAD:
						componentID = buffer.getInt();
						double cpuLoadReading = buffer.getDouble();
						if (cpuLoadReading != 0) {
							Server sendingServer = systemModel.getServerByID(componentID);
							CPULoadReading reading = new CPULoadReading(cpuLoadReading, messageReceivedTime,
									controller.getApplicationStartTime());
							sendingServer.pushCPULoadValue(reading);
							LoggerUtility.logServerLoadReading(reading.getTimestampAsSecondsElapsed(), componentID, cpuLoadReading);
						}
						break;
					case LOAD_BALANCER_ENTERED_ACTIVE:
						componentID = buffer.getInt();
						String activeInfoString = "";
						for (LoadBalancer lb : systemModel.getLoadBalancers()) {
							if (lb.getComponentID() == componentID) {
								if (lb.getState() == null) {
									activeInfoString = " entered ";
								} else {
									activeInfoString = " elevated to ";
								}
								lb.setState(LoadBalancerState.ACTIVE);
								break;
							}
						}
						controller.appendMainFeed("LoadBalancer " + componentID + activeInfoString + "active state.");
						break;
					case LOAD_BALANCER_ENTERED_PASSIVE:
						componentID = buffer.getInt();
						String passiveInfoString = "";
						for (LoadBalancer lb : systemModel.getLoadBalancers()) {
							if (lb.getComponentID() == componentID) {
								if (lb.getState() == null) {
									passiveInfoString = " entered ";
								} else {
									passiveInfoString = " demoted to ";
								}
								lb.setState(LoadBalancerState.PASSIVE);
								break;
							}
						}
						controller.appendMainFeed("LoadBalancer " + componentID +  passiveInfoString + "passive state.");
						break;
					case LOAD_BALANCER_ACTIVE_FAILURE_DETECTED:
						componentID = buffer.getInt();
						controller.appendMainFeed("LoadBalancer " + componentID + " detected failure of the active.");
						break;
					case LOAD_BALANCER_BACKUP_FAILURE_DETECTED:
						componentID = buffer.getInt();
						controller.appendMainFeed("LoadBalancer " + componentID + " detected failure or absence of a backup.");
						break;
					case LOAD_BALANCER_NO_ACTIVE_DETECTED:
						componentID = buffer.getInt();
						controller.appendMainFeed("LoadBalancer " + componentID + " detected absence of an active node.");
						break;
					case LOAD_BALANCER_FAILURE_DETECTION_DISMISSED:
						break;
					case LOAD_BALANCER_FAILURE_CONFIRMED:
						break;
					case LOAD_BALANCER_ELECTED_AS_BACKUP:
						componentID = buffer.getInt();
						controller.appendMainFeed("LoadBalancer " + componentID + " elected as backup.");
						break;
					case LOAD_BALANCER_PROMPTED_RE_ELECTION:
						componentID = buffer.getInt();
						controller.appendMainFeed("LoadBalancer " + componentID + " called for a re-election.");
						break;
					case LOAD_BALANCER_MULTIPLE_ACTIVES_DETECTED:
						componentID = buffer.getInt();
						controller.appendMainFeed("LoadBalancer " + componentID + " detected multiple actives and initiated an emergency election.");
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
		// System.out.println("test");
		if (systemModel.removeComponentBySocketChannel(socketChannel)) {
			String typeString = componentName.equals("ClientVirtualizer") || componentName.equals("NameService") ? ""
					: " with ID " + componentID;
			controller.appendMainFeed(componentName + typeString + " disconnected.");
		}
	}
}
