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
        FXMLLoader gameLoader;
        Pane gamePane;

        try {
            gameLoader = new FXMLLoader(Main.getURL("/game-pane.fxml"));
            gamePane = gameLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ((GamePaneController) gameLoader.getController()).createPlayer(
                playerName.getText(),
                serverAddress.getText(),
                Integer.parseInt(serverPort.getText()));

        Stage stage = (Stage) startButton.getScene().getWindow();
        Main.setStageParams(stage, gamePane);

        stage.close();
        stage.show();
    }
}
