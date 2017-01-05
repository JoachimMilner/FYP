package loadBalancer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import connectionUtils.ConnectNIO;
import connectionUtils.MessageType;

/**
 * @author Joachim</br>
 *         <p>
 * 		Class used to represent a back-end server and provide abstractions
 *         for TCP communication.
 *         </p>
 *
 */
public class Server {
	
	/**
	 * The remote address of this server.
	 */
	private InetSocketAddress address;
	
	
	/**
	 * The current CPU load of this remote server.
	 */
	private double cpuLoad = -1;
	

	/**
	 * Creates a new Server object that stores relevant information
	 * about the specified remote machine.
	 * @param address the remote address of this server.
	 * @throws IllegalArgumentException if the {@link InetSocketAddress} passed in is null.
	 */
	public Server(InetSocketAddress address) {
		if (address == null)
			throw new IllegalArgumentException("Server address cannot be null.");
		
		this.address = address;
	}


	/**
	 * @return the address of the remote server that this object represents.
	 */
	public InetSocketAddress getAddress() {
		return address;
	}


	/**
	 * @return the current CPU load of the remote server that this object represents.
	 */
	public double getCPULoad() {
		return cpuLoad;
	}
	
	
	/**
	 * Attempts to retrieve the CPU load of the remote server that this object represents.
	 * As this method uses a blocking socket, it should always be run in a new thread from
	 * a {@link ServerManager} instance. 
	 */
	public void updateCPULoad() {
		SocketChannel socketChannel = ConnectNIO.getBlockingSocketChannel(address);
		ByteBuffer buffer = ByteBuffer.allocate(9);
		buffer.put((byte) MessageType.SERVER_CPU_REQUEST.getValue());
		buffer.flip();
		while (buffer.hasRemaining()) {
			try {
				socketChannel.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		buffer.clear();
		try {
			socketChannel.read(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		buffer.flip();
		MessageType messageType = MessageType.values()[buffer.get()];
		if (!messageType.equals(MessageType.SERVER_CPU_NOTIFY)) {
			System.out.println("Error retrieving CPU load for Server at: " + address.getHostName());
		} else {
			cpuLoad = buffer.getDouble();
		}
		try {
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
