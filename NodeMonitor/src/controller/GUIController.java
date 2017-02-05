package controller;

import java.net.URL;
import java.util.ResourceBundle;

import comms.ConnectionHandler;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
public class GUIController implements Initializable {

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

	private SystemModel systemModel;

	private int[] currentClientConfigValues = new int[5];

	private Thread connectionHandlerThread;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		updateClientOptionsButton.setDisable(true);
		appendMainFeed("Waiting for components to register...");
		systemModel = new SystemModel();
		connectionHandlerThread = new Thread(new ConnectionHandler(systemModel, this));
		connectionHandlerThread.start();
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
	public void updateClientRequestResponseCount(int totalRequests, int totalResponses) {
		requestsSentLabel.setText(totalRequests + "");
		responsesReceivedLabel.setText(totalResponses + "");
	}

	public void shutdown() {
		connectionHandlerThread.interrupt();
	}
}
