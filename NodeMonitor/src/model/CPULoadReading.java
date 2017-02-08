package model;

/**
 * @author Joachim
 *         <p>
 *         Simple class used to hold a cpu load reading associated with a
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
	 * The time in milliseconds that this NodeMonitor process was started.
	 */
	private long applicationStartTime;

	/**
	 * @param cpuLoad
	 *            the CPU load reading for this instance
	 * @param timestamp
	 *            the timestamp recorded by the NodeMonitor
	 */
	public CPULoadReading(double cpuLoad, long timestamp, long applicationStartTime) {
		this.cpuLoad = cpuLoad;
		this.timestamp = timestamp;
		this.applicationStartTime = applicationStartTime;
	}

	/**
	 * @return the CPU load reading held in this instance.
	 */
	public double getCpuLoad() {
		return cpuLoad;
	}

	/**
	 * @return the time in milliseconds at which this reading was received by the
	 *         NodeMonitor.
	 */
	public long getTimestamp() {
		return timestamp;
	}
	
	public double getTimestampAsSecondsElapsed() {
		return (timestamp - applicationStartTime) / 1000;
	}
}