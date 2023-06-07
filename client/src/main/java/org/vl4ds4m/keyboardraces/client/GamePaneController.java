package org.vl4ds4m.keyboardraces.client;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.vl4ds4m.keyboardraces.game.GameSettings;
import org.vl4ds4m.keyboardraces.game.GameState;
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

    private Player player;

    void createPlayer(String name, String serverAddress, int serverPort) {
        player = new Player(name, serverAddress, serverPort);

        text.setDisable(true);
        input.setDisable(true);
        timer.textProperty().bind(player.getRemainTimeProperty());

        player.getPlayerDataList().addListener(new ResultsListener());
        player.getGameStateProperty().addListener(new GameStateListener());
        player.getConnectedProperty().addListener(new ConnectionListener());

        new Thread(player).start();
    }

    private List<String> words;
    private int currentWordNum;
    private boolean wordWrong;
    private int wrongCharPos;
    private int maxLenRightWord;

    private static class ConnectionListener implements ChangeListener<String> {
        @Override
        public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Соединение с сервером потеряно.");
            alert.setContentText("Причина: " + t1);
            alert.show();
        }
    }

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
                if (player.getGameStateProperty().get() == GameState.INIT ||
                        player.getGameStateProperty().get() == GameState.READY) {
                    return playerData.getName() + (playerData.currentPlayer() ? " (Вы)" : "");
                }

                int textLength = player.getText().length().get();
                int progress = textLength != 0 ? playerData.getInputCharsCount() * 100 / textLength : 0;

                int time = GameSettings.GAME_DURATION_TIME;
                if (playerData.getFinishTime() == -1) {
                    time -= Integer.parseInt(player.getRemainTimeProperty().get());
                } else {
                    time -= playerData.getFinishTime();
                }
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

    private class GameStateListener implements ChangeListener<GameState> {
        @Override
        public void changed(ObservableValue<? extends GameState> observableValue, GameState gameState, GameState t1) {
            if (t1 == GameState.READY) {
                text.textProperty().bind(player.getText());
                timerDescr.setText("Игра начнется через:");

            } else if (t1 == GameState.STARTED) {
                text.setDisable(false);
                input.setDisable(false);
                input.requestFocus();
                timerDescr.setText("Игра закончится через:");

                words = new ArrayList<>(List.of(text.getText().split(" ")));
                for (int i = 0; i < words.size() - 1; ++i) {
                    words.set(i, words.get(i) + " ");
                }

                wordWrong = false;
                currentWordNum = 0;
                synchronized (player.getData()) {
                    player.getData().setInputCharsCount(0);
                    player.getData().setErrorsCount(0);
                }
                maxLenRightWord = 0;
                wrongCharPos = -1;

                input.textProperty().addListener(new InputCharsListener());

            } else if (t1 == GameState.STOPPED) {
                input.setDisable(true);
                timerDescr.setText("Игра окончена");
                timer.textProperty().unbind();
                timer.setText("");
                text.textProperty().unbind();
                printResult();
            }
        }

        private void printResult() {
            int playerNum = -1;
            for (int i = 0; i < player.getPlayerDataList().size(); ++i) {
                if (player.getPlayerDataList().get(i).currentPlayer()) {
                    playerNum = i;
                    break;
                }
            }
            if (playerNum == 0) {
                text.setText("Поздравляю! Вы заняли 1-e место!");
            } else {
                text.setText("Вы заняли " + (playerNum + 1) + "-е место.");
            }
        }
    }

    // TODO Lock ctrl+V and selection text
    private class InputCharsListener implements ChangeListener<String> {
        @Override
        public void changed(ObservableValue<? extends String> observableWord, String oldWord, String newWord) {
            synchronized (player.getData()) {
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
                                    player.getData().setFinishTime(
                                            Integer.parseInt(player.getRemainTimeProperty().get()));
                                    player.getGameStateProperty().set(GameState.STOPPED);
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
}
