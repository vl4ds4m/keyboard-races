package org.vl4ds4m.keyboardraces.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginPaneController {
    @FXML
    private Button startButton;
    @FXML
    private TextField playerName;
    @FXML
    private Text wrongName;
    @FXML
    private TextField serverAddress;
    @FXML
    private TextField serverPort;
    @FXML
    private Text wrongPort;

    @FXML
    private void initialize() {
        startButton.disableProperty().bind(serverAddress.textProperty().isEmpty()
                .or(serverPort.textProperty().isEmpty())
                .or(playerName.textProperty().isEmpty())
                .or(wrongName.visibleProperty())
                .or(wrongPort.visibleProperty()));

        wrongName.visibleProperty().bind(playerName.textProperty().length().greaterThan(15));

        serverPort.textProperty().addListener((observableValue, s, t1) -> {
            int port;
            try {
                port = Integer.parseInt(t1);
            } catch (NumberFormatException e) {
                port = -1;
            }
            wrongPort.visibleProperty().set(port < 0 || port >= (1 << 16));
        });
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
