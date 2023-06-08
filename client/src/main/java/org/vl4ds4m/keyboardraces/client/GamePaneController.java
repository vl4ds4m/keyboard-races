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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
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
    private FlowPane wordsPane;
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
    private static final Font USUAL_FONT = Font.font("System", 20.0);
    private static final Font BOLD_FONT = new Font("System Bold", 20.0);

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

            ((Text) wordsPane.getChildren().get(currentWordNum)).setFill(Color.GREEN);
            ((Text) wordsPane.getChildren().get(currentWordNum)).setFont(BOLD_FONT);
            ((Text) wordsPane.getChildren().get(currentWordNum + 1)).setFont(BOLD_FONT);
        }

        private String newWord;
        private String currentWord;
        private Text leftPart;
        private Text rightPart;

        @Override
        public void changed(ObservableValue<? extends String> observableWord, String oldValue, String newValue) {
            synchronized (player.getData()) {
                newWord = newValue;

                leftPart = (Text) wordsPane.getChildren().get(currentWordNum);
                rightPart = (Text) wordsPane.getChildren().get(currentWordNum + 1);

                currentWord = words.get(currentWordNum);

                System.out.println("Old word: <" + leftPart.getText() + "> -> <" + rightPart.getText() + ">");
                System.out.println(oldValue + " -> " + newWord);

                if (currentWord.startsWith(newWord)) {
                    if (!oldWordRight) {
                        unsetError();
                    }
                    if (newWord.length() > maxLengthRightWord) {
                        increaseInputCharsCount();
                        if (currentWord.equals(newWord)) {
                            setNextWord();
                        }
                    }
                } else if (oldWordRight) {
                    setError();
                }

                changeWordSplit();
            }
        }

        private void changeWordSplit() {
            if (wrongCharPos < 0) {
                leftPart.setText(newWord);
                rightPart.setText(currentWord.substring(newWord.length()));
            } else {
                leftPart.setText(newWord.substring(0, wrongCharPos));
                rightPart.setText(currentWord.substring(wrongCharPos));
            }
        }

        private void increaseInputCharsCount() {
            player.getData().setInputCharsCount(
                    player.getData().getInputCharsCount() + (newWord.length() - maxLengthRightWord));
            maxLengthRightWord = newWord.length();
        }

        private void setNextWord() {
            maxLengthRightWord = 0;

            leftPart.setFill(Color.BLACK);
            leftPart.setFont(USUAL_FONT);

            if (currentWordNum == words.size() - 1) {
                player.getData().setFinishTime(Integer.parseInt(player.getRemainTimeProperty().get()));
                player.getGameStateProperty().set(GameState.STOPPED);
            } else {
                ++currentWordNum;
                rightPart.setFill(Color.GREEN);
                ((Text) wordsPane.getChildren().get(currentWordNum + 1)).setFont(BOLD_FONT);
            }

            Platform.runLater(input::clear);
        }

        private void setError() {
            oldWordRight = false;
            player.getData().setErrorsCount(player.getData().getErrorsCount() + 1);
            wrongCharPos = newWord.length() - 1;
            rightPart.setFill(Color.RED);
        }

        private void unsetError() {
            oldWordRight = true;
            wrongCharPos = -1;
            rightPart.setFill(Color.BLACK);
        }
    }

    private class GameStateListener implements ChangeListener<GameState> {
        private final InputCharsListener inputCharsListener = new InputCharsListener();

        @Override
        public void changed(ObservableValue<? extends GameState> observableValue, GameState gameState, GameState t1) {
            if (t1 == GameState.READY) {
                promptText.setVisible(false);

                timerDescr.setText("Игра начнется через:");

                words = new ArrayList<>(List.of(player.getText().get().split(" ")));
                for (int i = 0; i < words.size() - 1; ++i) {
                    words.set(i, words.get(i) + " ");
                }

                initializeWordsPane();
                wordsPane.setVisible(true);

            } else if (t1 == GameState.STARTED) {
                timerDescr.setText("Игра закончится через:");

                wordsPane.getChildren().forEach(text -> text.setOpacity(1.0));

                inputCharsListener.initialize();

                input.clear();
                input.setDisable(false);
                input.requestFocus();
                input.textProperty().addListener(inputCharsListener);

            } else if (t1 == GameState.STOPPED) {
                input.setDisable(true);
                input.textProperty().removeListener(inputCharsListener);

                wordsPane.setVisible(false);

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

        private void initializeWordsPane() {
            wordsPane.getChildren().clear();

            wordsPane.getChildren().add(new Text(""));
            words.forEach(word -> wordsPane.getChildren().add(new Text(word)));

            wordsPane.getChildren().forEach(text -> {
                ((Text) text).setFont(USUAL_FONT);
                text.setOpacity(0.2);
            });
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
}
