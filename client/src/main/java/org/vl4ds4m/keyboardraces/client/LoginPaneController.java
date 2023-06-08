package org.vl4ds4m.keyboardraces.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
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
                playerName.getText(), serverAddress.getText(), serverPort.getText());

        Stage stage = (Stage) startButton.getScene().getWindow();
        Main.setStageParams(stage, gamePane);

        stage.close();
        stage.show();
    }

    @FXML
    private void showInfo() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);

        info.setTitle("Keyboard Races");
        info.setHeaderText("Информация об игре");
        info.setContentText("""
                Игра 'Клавогонки' - соревновательная многопользовательская игра,
                в которой Вы должны быстрее свои соперников набрать текст на клавиатуре.
                
                Автор реализации: Самсонов Владислав (vl4d.s4m@gmail.com).""");

        info.show();
    }
}
