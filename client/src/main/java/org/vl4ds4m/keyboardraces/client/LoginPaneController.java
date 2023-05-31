package org.vl4ds4m.keyboardraces.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

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
        startButton.disableProperty().bind(servAddrTextField.textProperty().isEmpty());
        startButton.disableProperty().bind(servPortTextField.textProperty().isEmpty());
        startButton.disableProperty().bind(userNameTextField.textProperty().isEmpty());
    }

    @FXML
    private void clickStartButton() {
        try {
            Pane gamePane = FXMLLoader.load(Main.getURL("/game-pane.fxml"));
            Stage stage = (Stage) startButton.getScene().getWindow();
            Main.setStageParams(stage, gamePane, "Keyboard Races - Game");

            GamePaneController.createPlayer(servAddrTextField.getText(),
                    Integer.parseInt(servPortTextField.getText()));
            GamePaneController.connectToServer();

            stage.close();
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
