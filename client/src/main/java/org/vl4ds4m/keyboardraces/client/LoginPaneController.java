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
    private TextField playerName;
    @FXML
    private TextField serverAddress;
    @FXML
    private TextField serverPort;

    @FXML
    private void initialize() {
        startButton.disableProperty().bind(serverAddress.textProperty().isEmpty());
        startButton.disableProperty().bind(serverPort.textProperty().isEmpty());
        startButton.disableProperty().bind(playerName.textProperty().isEmpty());
    }

    @FXML
    private void clickStartButton() {
        try {
            playerSettings = new PlayerSettings(
                    playerName.getText(),
                    serverAddress.getText(),
                    Integer.parseInt(serverPort.getText()));

            Pane gamePane = FXMLLoader.load(Main.getURL("/game-pane.fxml"));
            Stage stage = (Stage) startButton.getScene().getWindow();
            Main.setStageParams(stage, gamePane, "Keyboard Races - Game");

            stage.close();
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    record PlayerSettings(String name, String serverAddress, int serverPort) {
    }

    private static PlayerSettings playerSettings;

    static PlayerSettings getPlayerSettings() {
        return playerSettings;
    }
}
