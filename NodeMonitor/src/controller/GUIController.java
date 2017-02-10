package controller;

import java.net.URL;
import java.util.ResourceBundle;

import comms.ConnectionHandler;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import model.CPULoadReading;
import model.Server;
import model.SystemModel;

/**
 * @author Joachim
 *         <p>
 *         Controller class for the GUI. While the NodeMonitor has an MVC-based
 *         approach, the interface is too simple to justify using a refresh view
 *         approach and instead has designated methods for updating each
 *         component.
 *         </p>
 */
/**
 * @author Joachim
 *
 */
public class GUIController implements Initializable {

	@FXML
	private LineChart<Number, Number> serverLoadLineChart;

	@FXML
	private NumberAxis lineChartXAxis;

	@FXML
	private NumberAxis lineChartYAxis;

	@FXML
	private TextArea mainFeedTextArea;

	@FXML
	private TextArea nameServiceTextArea;

	@FXML
	private TextField maxClientsTextField;

	@FXML
	private TextField minSendFrequencyTextField;

	@FXML
	private TextField maxSendFrequencyTextField;

	@FXML
	private TextField minMessageCountTextField;

	@FXML
	private TextField maxMessageCountTextField;

	@FXML
	private Button updateClientOptionsButton;

	@FXML
	private Label requestsSentLabel;

	@FXML
	private Label responsesReceivedLabel;

	/**
	 * The time in milliseconds that this process was started, used for
	 * timestamping.
	 */
	private long applicationStartTime;

	/**
	 * The data model used to represent the entire system
	 */
	private SystemModel systemModel;

	/**
	 * Used to store the most recent client configuration option values so we
	 * can enable/disable the update button as the fields are edited.
	 */
	private int[] currentClientConfigValues = new int[5];

	/**
	 * Set as global variables so we can safely close the program
	 */
	private Thread connectionHandlerThread;
	private Thread graphHandlerThread;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		applicationStartTime = System.currentTimeMillis();
		updateClientOptionsButton.setDisable(true);
		appendMainFeed("Waiting for components to register...");
		systemModel = new SystemModel();
		connectionHandlerThread = new Thread(new ConnectionHandler(systemModel, this));
		connectionHandlerThread.start();
		initializeServerGraphThread();
	}

	/**
	 * Initialises change listeners on the GUI's text field to only allow
	 * numeric values and only enable the update button when a value has
	 * actually changed.
	 */
	private void initializeClientConfigListeners() {
		TextField[] textFields = { maxClientsTextField, minSendFrequencyTextField, maxSendFrequencyTextField,
				minMessageCountTextField, maxMessageCountTextField };
		for (int i = 0; i < textFields.length; i++) {
			final int j = i;

			textFields[i].textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					if (!newValue.matches("\\d*")) {
						textFields[j].setText(newValue.replaceAll("[^\\d]", ""));
					} else if (!newValue.equals("") && !newValue.equals(currentClientConfigValues[j] + "")) {
						updateClientOptionsButton.setDisable(false);
					} else {
						updateClientOptionsButton.setDisable(true);
					}
				}
			});
		}
	}

	private void initializeServerGraphThread() {
		serverLoadLineChart.setCreateSymbols(false);

		lineChartXAxis.setAutoRanging(false);
		lineChartXAxis.setLabel("Time elapsed since t=0 (s)");
		// lineChartXAxis.setTickUnit(60);

		lineChartYAxis.setAutoRanging(false);
		lineChartYAxis.setLabel("CPU Load (%)");
		// lineChartYAxis.setTickUnit(100);
		lineChartYAxis.setLowerBound(0);
		lineChartYAxis.setUpperBound(100);

		// Concurrent updating of the JavaFX UI based on code from:
		// http://stackoverflow.com/a/20498014
		@SuppressWarnings("rawtypes")
		Task task = new Task<Void>() {
			@Override
			public Void call() {
				while (!Thread.currentThread().isInterrupted()) {

					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							refreshServerGraph();
						}

					});

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				return null;
			}
		};
		graphHandlerThread = new Thread(task);
		graphHandlerThread.setDaemon(true);
		graphHandlerThread.start();
	}

	private void refreshServerGraph() {
		long msElapsedSinceStart = System.currentTimeMillis() - applicationStartTime;
		int xAxisMin = msElapsedSinceStart < 60000 ? 0 : (int) Math.floor(msElapsedSinceStart / 1000) - 60;
		int xAxisMax = msElapsedSinceStart < 60000 ? 60 : (int) Math.floor(msElapsedSinceStart / 1000);
		lineChartXAxis.setLowerBound(xAxisMin);
		lineChartXAxis.setUpperBound(xAxisMax);

		for (Server server : systemModel.getServers()) {

			Series<Number, Number> serverDataSeries = server.getSeries();

			while (server.getNewCPULoadValueCount() > 0) {
				CPULoadReading cpuLoadReading = server.getCPULoadValues().poll();
				server.decrementNewCPULoadValueCount();
				serverDataSeries.getData().add(new XYChart.Data<Number, Number>(
						cpuLoadReading.getTimestampAsSecondsElapsed(), cpuLoadReading.getCpuLoad()));
			}

			while (!serverDataSeries.getData().isEmpty()
					&& (double) serverDataSeries.getData().get(0).getXValue() < xAxisMin) {
				serverDataSeries.getData().remove(0);
			}
			//System.out.println(serverDataSeries.getData().size());
		}
	}

	/**
	 * Adds the specified XYChart data series to the UI's graph.
	 * 
	 * @param series
	 *            the series to add to the graph
	 */
	public void addDataSeriesToGraph(XYChart.Series<Number, Number> series) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				serverLoadLineChart.getData().add(series);
			}

		});

	}

	/**
	 * Action listener for the client update button.
	 */
	@FXML
	private void updateClientConfiguration() {
		int maxClients = Integer.parseInt(maxClientsTextField.getText());
		int minSendFrequency = Integer.parseInt(minSendFrequencyTextField.getText());
		int maxSendFrequency = Integer.parseInt(maxSendFrequencyTextField.getText());
		int minClientRequests = Integer.parseInt(minMessageCountTextField.getText());
		int maxClientRequests = Integer.parseInt(maxMessageCountTextField.getText());

		boolean updateSuccessful = systemModel.getClientVirtualizer().sendClientConfigurationUpdate(maxClients,
				minSendFrequency, maxSendFrequency, minClientRequests, maxClientRequests);
		if (updateSuccessful) {
			appendMainFeed("Successfully updated client configuration settings.");
			currentClientConfigValues[0] = maxClients;
			currentClientConfigValues[1] = minSendFrequency;
			currentClientConfigValues[2] = maxSendFrequency;
			currentClientConfigValues[3] = minClientRequests;
			currentClientConfigValues[4] = maxClientRequests;
			updateClientOptionsButton.setDisable(true);
		} else {
			appendMainFeed("Updating client configuration settings was not successful, values have not been changed.");
		}
	}

	/**
	 * Appends the given string to the <code>mainFeedTextArea</code>
	 * 
	 * @param message
	 *            the message to be appended
	 */
	public void appendMainFeed(String message) {
		mainFeedTextArea.appendText(message + "\n");
	}

	/**
	 * Appends the given string to the <code>nameServiceTextArea</code>.
	 * 
	 * @param message
	 */
	public void appendNameServiceFeed(String message) {
		nameServiceTextArea.appendText(message + "\n");
	}

	/**
	 * Sets the initial client configuration option values that are sent by the
	 * ClientVirtualizer
	 * 
	 * @param maxClients
	 * @param minSendFrequency
	 * @param maxSendFrequency
	 * @param minClientRequests
	 * @param maxClientRequests
	 */
	public void setClientConfigOptions(int maxClients, int minSendFrequency, int maxSendFrequency,
			int minClientRequests, int maxClientRequests) {
		maxClientsTextField.setText(maxClients + "");
		maxClientsTextField.setDisable(false);
		currentClientConfigValues[0] = maxClients;

		minSendFrequencyTextField.setText(minSendFrequency + "");
		minSendFrequencyTextField.setDisable(false);
		currentClientConfigValues[1] = minSendFrequency;

		maxSendFrequencyTextField.setText(maxSendFrequency + "");
		maxSendFrequencyTextField.setDisable(false);
		currentClientConfigValues[2] = maxSendFrequency;

		minMessageCountTextField.setText(minClientRequests + "");
		minMessageCountTextField.setDisable(false);
		currentClientConfigValues[3] = minClientRequests;

		maxMessageCountTextField.setText(maxClientRequests + "");
		maxMessageCountTextField.setDisable(false);
		currentClientConfigValues[4] = maxClientRequests;

		initializeClientConfigListeners();
	}

	/**
	 * Updates the <code>requestsSentLabel</code> and
	 * <code>responsesReceivedLabel</code> values with the given values.
	 * 
	 * @param totalRequests
	 * @param totalResponses
	 */
	public void refreshClientRequestResponseCount() {
		requestsSentLabel.setText(systemModel.getClientVirtualizer().getTotalRequestsSent() + "");
		responsesReceivedLabel.setText(systemModel.getClientVirtualizer().getTotalResponsesReceived() + "");
	}

	/**
	 * @return the start time of this application process in milliseconds.
	 */
	public long getApplicationStartTime() {
		return applicationStartTime;
	}

	/**
	 * Called when the program exits in order to make sure sockets are closed
	 * and threads are killed properly.
	 */
	public void shutdown() {
		connectionHandlerThread.interrupt();
		graphHandlerThread.interrupt();
	}
}
