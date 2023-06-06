package org.vl4ds4m.keyboardraces.client;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.vl4ds4m.keyboardraces.game.Player;
import org.vl4ds4m.keyboardraces.game.PlayerResult;

import java.util.ArrayList;
import java.util.List;


public class GamePaneController {
    @FXML
    private Label text;
    @FXML
    private TextField input;
    @FXML
    private Label timer;
    @FXML
    private Label timerDescr;
    @FXML
    private Label firstPlace;
    @FXML
    private Label secondPlace;
    @FXML
    private Label thirdPlace;


    @FXML
    private void initialize() {
        LoginPaneController.PlayerSettings playerSettings = LoginPaneController.getPlayerSettings();

        player = new Player(playerSettings.name());
        player.connectToServer(playerSettings.serverAddress(), playerSettings.serverPort());

        input.disableProperty().bind(text.disableProperty());
        timerDescr.setText("Ожидание игроков:");
        timer.textProperty().bind(player.getRemainTimeProperty());

        player.getPlayersResultsList().addListener(new ResultsListener());
        player.getGameReadyProperty().addListener(new ReadyGameListener());
        player.getGameStartProperty().addListener(new StartGameListener());
        player.getGameStopProperty().addListener(new StopGameListener());
    }

    private Player player;
    private List<String> words;
    private int currentWordNum;
    private boolean wordWrong;
    private int wrongCharPos;
    private int maxLenRightWord;

    private class ResultsListener implements ListChangeListener<PlayerResult> {
        @Override
        public void onChanged(Change<? extends PlayerResult> change) {
            if (player.getPlayersResultsList().size() >= 1) {
                firstPlace.setText("1. " + player.getPlayersResultsList().get(0));
            } else {
                firstPlace.setText("");
            }
            if (player.getPlayersResultsList().size() >= 2) {
                secondPlace.setText("2. " + player.getPlayersResultsList().get(1));
            } else {
                secondPlace.setText("");
            }
            if (player.getPlayersResultsList().size() >= 3) {
                thirdPlace.setText("3. " + player.getPlayersResultsList().get(2));
            } else {
                thirdPlace.setText("");
            }
        }
    }

    private class ReadyGameListener implements ChangeListener<Boolean> {
        @Override
        public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
            text.textProperty().bind(player.getText());
            timerDescr.setText("Игра начнется через:");
        }
    }

    private class StartGameListener implements ChangeListener<Boolean> {
        @Override
        public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
            text.setDisable(false);
            input.requestFocus();
            timerDescr.setText("Игра закончится через:");

            words = new ArrayList<>(List.of(text.getText().split(" ")));
            for (int i = 0; i < words.size() - 1; ++i) {
                words.set(i, words.get(i) + " ");
            }

            wordWrong = false;
            currentWordNum = 0;
            player.getData().setInputCharsCount(0);
            player.getData().setErrorsCount(0);
            maxLenRightWord = 0;
            wrongCharPos = -1;

            input.textProperty().addListener(new InputCharsListener());
        }
    }

    private class StopGameListener implements ChangeListener<Boolean> {
        @Override
        public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
            text.setDisable(true);
            timerDescr.setText("Игра окончена");
            timer.textProperty().unbind();
            timer.setText("");
        }
    }

    // TODO Lock ctrl+V and selection text
    private class InputCharsListener implements ChangeListener<String> {
        @Override
        public void changed(ObservableValue<? extends String> observableWord, String oldWord, String newWord) {
            System.out.println(oldWord + " -> " + newWord);

            int lastCharPos = newWord.length() - 1;
            String currentWord = words.get(currentWordNum);

            if (!wordWrong) {
                if (currentWord.startsWith(newWord)) {
                    if (newWord.length() > maxLenRightWord) {
                        player.getData().setInputCharsCount(player.getData().getInputCharsCount() + 1);
                        ++maxLenRightWord;

                        if (currentWord.equals(newWord)) {
                            maxLenRightWord = 0;
                            if (currentWordNum == words.size() - 1) {
                                player.getGameStopProperty().set(true);
                            } else {
                                ++currentWordNum;
                            }

                            Platform.runLater(() -> input.setText(""));
                        }
                    }
                } else {
                    wordWrong = true;
                    player.getData().setErrorsCount(player.getData().getErrorsCount() + 1);
                    wrongCharPos = lastCharPos;
                }
            } else {
                if (currentWord.startsWith(newWord) && newWord.length() == wrongCharPos) {
                    wordWrong = false;
                    wrongCharPos = -1;
                }
            }
        }
    }
}
