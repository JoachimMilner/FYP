package model;

import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Joachim
 *         <p>
 *         Class object used to represent a remote server for handling of
 *         incoming log messages.
 *         </p>
 *
 */
public class Server extends AbstractRemoteSystemComponent {

	/**
	 * The CPU load values that are held for this server. Limited to x values.
	 */
	private Queue<CPULoadReading> cpuLoadValues = new LinkedBlockingQueue<>();

	/**
	 * Constructs a new Server object with the given componentID and
	 * remoteAddress that will be used primarily for storing CPU load values.
	 * 
	 * @param componentID
	 *            the unique ID of this server
	 * @param remoteAddress
	 *            the remote address of this server
	 */
	public Server(int componentID, InetSocketAddress remoteAddress) {
		this.componentID = componentID;
		this.remoteAddress = remoteAddress;
	}
	
	/**
	 * @return the CPU load values that are held for this server.
	 */
	public Queue<CPULoadReading> getCPULoadValues() {
		return cpuLoadValues;
	}

	/**
	 * @author Joachim
	 *         <p>
	 * 		Simple class used to hold a cpu load reading associated with a
	 *         timestamp
	 *         </p>
	 *
	 */
	public class CPULoadReading {

		/**
		 * The CPU load value as a percentage
		 */
		private double cpuLoad;

		/**
		 * The timestamp in milliseconds of this reading, recorded by the
		 * NodeMonitor.
		 */
		private long timestamp;

		/**
		 * @param cpuLoad
		 *            the CPU load reading for this instance
		 * @param timestamp
		 *            the timestamp recorded by the NodeMonitor
		 */
		public CPULoadReading(double cpuLoad, long timestamp) {
			this.cpuLoad = cpuLoad;
			this.timestamp = timestamp;
		}

		/**
		 * @return the CPU load reading held in this instance.
		 */
		public double getCpuLoad() {
			return cpuLoad;
		}

		/**
		 * @return the time at which this reading was received by the
		 *         NodeMonitor.
		 */
		public long getTimestamp() {
			return timestamp;
		}
	}
}
