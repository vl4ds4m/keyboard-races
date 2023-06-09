package org.vl4ds4m.keyboardraces.client;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.vl4ds4m.keyboardraces.game.GameSettings;
import org.vl4ds4m.keyboardraces.game.GameState;
import org.vl4ds4m.keyboardraces.game.PlayerData;

import java.util.ArrayList;
import java.util.List;

public class GamePaneController {
    @FXML
    private Label promptText;
    @FXML
    private Label timer;
    @FXML
    private Label timerDescr;
    @FXML
    private Label playersResults;
    @FXML
    private Button newGameButton;
    @FXML
    private StackPane inputPane;
    private final TextField input = new TextField() {
        @Override
        public void paste() {
        }
    };

    @FXML
    private void initialize() {
        input.setPromptText("Ввод");
        input.setMaxWidth(750.0);
        input.setFont(Font.font(16.0));

        inputPane.getChildren().add(input);

        textPane.setVisible(false);
    }

    @FXML
    private void playAgain() {
        createPlayer(player.getData().getName(), player.getServerAddress(), player.getServerPort());
    }

    private Player player;

    void createPlayer(String name, String serverAddress, String serverPort) {
        player = new Player(name, serverAddress, serverPort);

        newGameButton.setDisable(true);
        newGameButton.setVisible(false);

        promptText.setDisable(true);
        promptText.setText("Текст");
        promptText.setVisible(true);

        input.clear();
        input.setDisable(true);

        timerDescr.setText("Ожидание игроков");
        timer.textProperty().bind(player.getRemainTimeProperty());

        playersResults.setText("Список игроков");

        player.getPlayerDataList().addListener(new ResultsListener());
        player.getGameStateProperty().addListener(new GameStateListener());
        player.getConnectedProperty().addListener(new ConnectionListener());

        Thread playerThread = new Thread(player);
        playerThread.setDaemon(true);
        playerThread.start();
    }

    private static class ConnectionListener implements ChangeListener<String> {
        @Override
        public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
            Alert alert = new Alert(Alert.AlertType.ERROR);

            alert.setTitle("Keyboard Races");
            alert.setHeaderText("Ошибка соединения с сервером.");
            alert.setContentText("Причина: " + t1);

            alert.showAndWait();
            Platform.exit();
        }
    }

    private List<String> words;
    @FXML
    private TextFlow textPane;
    @FXML
    private Text leftText;
    @FXML
    private Text currentLeft;
    @FXML
    private Text currentRight;
    @FXML
    private Text rightText;

    private class InputCharsListener implements ChangeListener<String> {
        private int currentWordNum = 0;
        private boolean oldWordRight = true;
        private int wrongCharPos = -1;
        private int maxLengthRightWord = 0;

        private void initialize() {
            currentWordNum = 0;
            oldWordRight = true;
            wrongCharPos = -1;
            maxLengthRightWord = 0;
            currentWord = words.get(currentWordNum);

            currentLeft.setText("");
            currentRight.setText(currentWord);
            rightText.setText(rightText.getText().substring(currentWord.length()));
        }

        private String newWord;
        private String currentWord;

        @Override
        public void changed(ObservableValue<? extends String> observableWord, String oldValue, String newValue) {
            synchronized (player.getData()) {
                newWord = newValue;

                System.out.println("Old word: <" + currentLeft.getText() + "> -> <" + currentRight.getText() + ">");
                System.out.println(oldValue + " -> " + newWord);

                if (currentWord.startsWith(newWord)) {
                    if (!oldWordRight) {
                        unsetError();
                    }
                    if (newWord.length() > maxLengthRightWord) {
                        increaseInputCharsCount();
                        if (currentWord.equals(newWord)) {
                            setNextWord();
                        } else {
                            changeWordSplit();
                        }
                    } else {
                        changeWordSplit();
                    }
                } else if (oldWordRight) {
                    setError();
                }
            }
        }

        private void changeWordSplit() {
            if (wrongCharPos < 0) {
                currentLeft.setText(newWord);
                currentRight.setText(currentWord.substring(newWord.length()));
            } else {
                currentLeft.setText(newWord.substring(0, wrongCharPos));
                currentRight.setText(currentWord.substring(wrongCharPos));
            }
        }

        private void increaseInputCharsCount() {
            player.getData().setInputCharsCount(
                    player.getData().getInputCharsCount() + (newWord.length() - maxLengthRightWord));
            maxLengthRightWord = newWord.length();
        }

        private void setNextWord() {
            maxLengthRightWord = 0;

            leftText.setText(leftText.getText() + currentWord);

            if (currentWordNum == words.size() - 1) {
                player.getData().setFinishTime(Integer.parseInt(player.getRemainTimeProperty().get()));
                player.getGameStateProperty().set(GameState.STOPPED);
            } else {
                currentWord = words.get(++currentWordNum);
                currentLeft.setText("");
                currentRight.setText(currentWord);
                rightText.setText(rightText.getText().substring(currentWord.length()));
            }

            Platform.runLater(input::clear);
        }

        private void setError() {
            oldWordRight = false;
            player.getData().setErrorsCount(player.getData().getErrorsCount() + 1);
            wrongCharPos = newWord.length() - 1;
            currentRight.setFill(Color.RED);
        }

        private void unsetError() {
            oldWordRight = true;
            wrongCharPos = -1;
            currentRight.setFill(Color.BLACK);
        }
    }

    private class GameStateListener implements ChangeListener<GameState> {
        private final InputCharsListener inputCharsListener = new InputCharsListener();

        @Override
        public void changed(ObservableValue<? extends GameState> observableValue, GameState t0, GameState gs) {
            if (gs == GameState.READY) {
                promptText.setVisible(false);

                timerDescr.setText("Игра начнется через:");

                words = new ArrayList<>(List.of(player.getText().get().split(" ")));
                for (int i = 0; i < words.size() - 1; ++i) {
                    words.set(i, words.get(i) + " ");
                }

                leftText.setText("");
                currentLeft.setText("");
                currentRight.setText("");
                rightText.setText(String.join("", words));

                textPane.setOpacity(0.2);
                textPane.setVisible(true);

            } else if (gs == GameState.STARTED) {
                timerDescr.setText("Игра закончится через:");

                textPane.setOpacity(1.0);

                inputCharsListener.initialize();

                input.clear();
                input.setDisable(false);
                input.requestFocus();
                input.textProperty().addListener(inputCharsListener);

            } else if (gs == GameState.STOPPED) {
                input.setDisable(true);
                input.textProperty().removeListener(inputCharsListener);

                textPane.setVisible(false);

                timerDescr.setText("Игра окончена");
                timer.textProperty().unbind();
                timer.setText("");

                promptText.setDisable(false);
                printResult();
                promptText.setVisible(true);

                newGameButton.setVisible(true);
                newGameButton.setDisable(false);
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
                promptText.setText("Поздравляю! Вы заняли 1-e место!");
            } else {
                promptText.setText("Вы заняли " + (playerNum + 1) + "-е место.");
            }
        }
    }

    private class ResultsListener implements ListChangeListener<PlayerData> {
        @Override
        public void onChanged(Change<? extends PlayerData> change) {
            int place = 1;
            StringBuilder results = new StringBuilder();
            GameState currentState = player.getGameStateProperty().get();

            for (int i = 0; i < player.getPlayerDataList().size(); ++i) {
                PlayerData current = player.getPlayerDataList().get(i);

                if (currentState == GameState.INIT || currentState == GameState.READY) {
                    if (i != 0) {
                        results.append("\n");
                    }
                    results.append(getPlayerResult(current));
                } else {
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
            }

            playersResults.setText(results.toString());
        }

        private String getPlayerResult(PlayerData playerData) {
            if (playerData.connected() || playerData.getFinishTime() >= 0) {
                GameState currentState = player.getGameStateProperty().get();
                if (currentState == GameState.INIT || currentState == GameState.READY) {
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
}
