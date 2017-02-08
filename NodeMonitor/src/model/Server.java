package model;

import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import javafx.application.Platform;
import javafx.scene.chart.XYChart;

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
	 * The XY Series of data points that are plotted on the UI graph.
	 */
	private XYChart.Series<Number, Number> series = new XYChart.Series<>();;
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
		series.setName("Server " + componentID);
	}

	/**
	 * @return the CPU load values that are held for this server.
	 */
	public Queue<CPULoadReading> getCPULoadValues() {
		return cpuLoadValues;
	}

	/**
	 * @return The XY Series of data points that are plotted on the UI graph.
	 */
	public XYChart.Series<Number, Number> getSeries() {
		return series;
	}

	/**
	 * Pushes the given cpu load reading on to this Server's queue of CPU load
	 * values, and removes values from the head of this queue if they are more
	 * than 60 seconds old.
	 * 
	 * @param cpuLoadReading
	 *            the CPU load reading to be pushed on to the queue
	 */
	public void pushCPULoadValue(CPULoadReading cpuLoadReading) {
		cpuLoadValues.add(cpuLoadReading);
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				series.getData().add(new XYChart.Data<Number, Number>(cpuLoadReading.getTimestampAsSecondsElapsed(),
						cpuLoadReading.getCpuLoad()));
			}
			
		});

		while (!cpuLoadValues.isEmpty() && 
				cpuLoadValues.peek().getTimestamp() < System.currentTimeMillis() - 60000) {
			cpuLoadValues.poll();
			series.getData().remove(0);
		}
	}
}
