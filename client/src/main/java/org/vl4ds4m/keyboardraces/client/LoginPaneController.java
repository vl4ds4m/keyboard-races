package org.vl4ds4m.keyboardraces.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

import static org.vl4ds4m.keyboardraces.client.Main.setStageParams;
import static org.vl4ds4m.keyboardraces.client.Main.loadPane;

public class LoginPaneController {
    @FXML
    private Button startButton;
    @FXML
    private TextField servAddrTextField;
    @FXML
    private TextField servPortTextField;
    @FXML
    private TextField userNameTextField;

    @FXML
    private void initialize() {
        /*startButton.disableProperty().bind(servAddrTextField.textProperty().isEmpty());
        startButton.disableProperty().bind(servPortTextField.textProperty().isEmpty());
        startButton.disableProperty().bind(userNameTextField.textProperty().isEmpty());*/
    }

    @FXML
    private void clickStartButton() {
        try {
            Stage stage = (Stage) startButton.getScene().getWindow();
            Pane gamePane = loadPane("/game-pane.fxml");
            setStageParams(stage, gamePane, "Keyboard Races - Game");
            stage.close();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
