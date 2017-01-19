package commsModel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Deque;

import connectionUtils.ConnectNIO;
import connectionUtils.MessageType;
import loadBalancer.ServerManager;

/**
 * @author Joachim
 *         <p>
 *         Class used to represent a back-end server and provide abstractions
 *         for TCP communication.
 *         </p>
 *
 */
public class Server extends AbstractRemote {

	/**
	 * The default server usage token expiry value to be used when this server
	 * does not have enough CPU load data to calculate an accurate coefficient
	 * of variation.
	 */
	private static int defaultTokenExpiration;

	/**
	 * The current CPU load of this remote server.
	 */
	private double cpuLoad = -1;

	/**
	 * List of the last 20 (max) CPU load values for this server. Used to
	 * calculate the coefficient of variation and generate a token expiry when
	 * sending the details of this server.
	 */
	private Deque<Double> cpuLoadRecords = new ArrayDeque<>();

	/**
	 * Time stamp in Unix seconds representing the time that this server should
	 * be used until, to be sent to a service-requesting client.
	 */
	private long tokenExpiry;

	/**
	 * Creates a new Server object that stores relevant information about the
	 * specified remote machine.
	 * 
	 * @param address
	 *            the remote address of this server.
	 * @throws IllegalArgumentException
	 *             if the {@link InetSocketAddress} passed in is null.
	 */
	public Server(InetSocketAddress address) {
		if (address == null)
			throw new IllegalArgumentException("Server address cannot be null.");

		this.address = address;
	}

	/**
	 * @return the current CPU load of the remote server that this object
	 *         represents.
	 */
	public double getCPULoad() {
		return cpuLoad;
	}

	/**
	 * @return the current token expiry for this server, as a Unix seconds
	 *         timestamp.
	 */
	public long getTokenExpiry() {
		return tokenExpiry;
	}
	
	/**
	 * @param value the default token expiration in seconds to be set for all server objects. 
	 */
	public static void setDefaultTokenExpiration(int value) {
		defaultTokenExpiration = value;
	}

	/**
	 * Attempts to connect to and retrieve the CPU load of the remote server
	 * that this object represents. As this method uses a blocking socket, it
	 * should always be run in a new thread from a {@link ServerManager}
	 * instance. In the case that the remote server is down or unresponsive and
	 * the connection fails, this method will set the <code>isAlive</code> state
	 * of this object to false and return, otherwise sets it to true.
	 */
	public void updateServerState() {
		SocketChannel socketChannel = ConnectNIO.getBlockingSocketChannel(address);
		if (socketChannel == null) {
			isAlive = false;
			return;
		}
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
			cpuLoadRecords.push(cpuLoad);
			while (cpuLoadRecords.size() > 20) {
				cpuLoadRecords.pollLast();
			}
			isAlive = true;
			System.out.println("Test " + address.getPort() + " " + cpuLoad);
		}
		try {
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Uses the <code>cpuLoadRecords</code> values to calculate the coefficient
	 * of variation for this server and then generate a token expiry time based
	 * on the variance. Uses the default token expiry value (set in
	 * lbConfig.xml) if the collection has less than 12 values.
	 */
	public void calculateTokenExpiry() {
		int tokenDurationSeconds = 0;
		if (cpuLoadRecords.size() < 12) {
			tokenDurationSeconds = defaultTokenExpiration;
		} else {
			// Calculate a token expiry time based on the CPU load data variance.
			tokenDurationSeconds = (int) Math.round(Math.abs(1 - getCoV()) * 100);
		}
		tokenExpiry = System.currentTimeMillis() * 1000 + tokenDurationSeconds;
	}
	
	/**
	 * Calculates the coefficient of variation using the values in <code>cpuLoadRecords</code>
	 * @return the coefficient of variation of this server's cpu load data.
	 */
	private double getCoV() {
		int dataSetSize = cpuLoadRecords.size();
		// Calculate mean
		double mean = 0;
		for (double value : cpuLoadRecords) {
			mean += value;
		}
		mean = mean / dataSetSize;
		
		// Calculate Variance
		double varianceSum = 0;
		for (double value : cpuLoadRecords) {
			varianceSum += Math.pow(value - mean, 2); 
		}
		double variance = varianceSum / (dataSetSize - 1);
		
		double standardDeviation = Math.sqrt(variance);
		
		return standardDeviation / mean;
	}
}
