package controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

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

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		mainFeedTextArea.setText("Waiting for components to register...");
	}

}

