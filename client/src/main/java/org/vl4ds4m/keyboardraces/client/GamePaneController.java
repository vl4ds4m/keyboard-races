package org.vl4ds4m.keyboardraces.client;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.vl4ds4m.keyboardraces.game.GameSettings;
import org.vl4ds4m.keyboardraces.game.Player;
import org.vl4ds4m.keyboardraces.game.PlayerData;

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
    private Label playersResults;

    @FXML
    private void initialize() {
        LoginPaneController.PlayerSettings playerSettings = LoginPaneController.getPlayerSettings();

        player = new Player(playerSettings.name());
        player.connectToServer(playerSettings.serverAddress(), playerSettings.serverPort());

        input.disableProperty().bind(text.disableProperty());
        timerDescr.setText("Ожидание игроков:");
        timer.textProperty().bind(player.getRemainTimeProperty());

        player.getPlayerDataList().addListener(new ResultsListener());
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

    private class ResultsListener implements ListChangeListener<PlayerData> {
        @Override
        public void onChanged(Change<? extends PlayerData> change) {
            int place = 1;
            StringBuilder results = new StringBuilder();

            for (int i = 0; i < player.getPlayerDataList().size(); ++i) {
                PlayerData current = player.getPlayerDataList().get(i);
                if (i == 0) {
                    results.append(place).append(".\t").append(getPlayerResult(current));
                } else {
                    results.append("\n");
                    PlayerData previous = player.getPlayerDataList().get(i - 1);
                    if (PlayerData.RATE_COMP.compare(current, previous) == 0) {
                        results.append("\t").append(getPlayerResult(current));
                    } else {
                        results.append(++place).append(".\t").append(getPlayerResult(current));
                    }
                }
            }

            playersResults.setText(results.toString());
        }

        private String getPlayerResult(PlayerData playerData) {
            if (playerData.connected()) {
                if (!player.getGameStartProperty().get()) {
                    return playerData.getName() + (playerData.currentPlayer() ? " (Вы)" : "");
                }

                int textLength = text.getText().length();
                int progress = textLength != 0 ? playerData.getInputCharsCount() * 100 / textLength : 0;
                int time = GameSettings.GAME_DURATION_TIME - Integer.parseInt(player.getRemainTimeProperty().get());
                int speed = time != 0 ? playerData.getInputCharsCount() * 60 / time : 0;

                return playerData.getName() +
                        (playerData.currentPlayer() ? " (Вы), " : ", ") +
                        progress + " % готово" +
                        ", ошибки: " + playerData.getErrorsCount() +
                        ", " + speed + " сим/мин.";
            }
            return playerData.getName() + (playerData.currentPlayer() ? " (Вы)" : "") + ", соединение потеряно.";
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
