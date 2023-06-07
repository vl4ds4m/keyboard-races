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
    private TextField input;
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
    private void playAgain() {
        createPlayer(player.getData().getName(), player.getServerAddress(), player.getServerPort());
    }

    private Player player;

    void createPlayer(String name, String serverAddress, int serverPort) {
        player = new Player(name, serverAddress, serverPort);

        newGameButton.setDisable(true);
        newGameButton.setVisible(false);

        promptText.setDisable(true);
        promptText.setText("Текст");
        promptText.setVisible(true);

        input.setDisable(true);

        timerDescr.setText("Ожидание игроков");
        timer.textProperty().bind(player.getRemainTimeProperty());

        player.getPlayerDataList().addListener(new ResultsListener());
        player.getGameStateProperty().addListener(new GameStateListener());
        player.getConnectedProperty().addListener(new ConnectionListener());

        new Thread(player).start();
    }

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

    private List<String> words;

    private class InputCharsListener implements ChangeListener<String> {
        private int currentWordNum = 0;
        private boolean wordWrong = false;
        private int wrongCharPos = -1;
        private int maxLenRightWord = 0;

        private void initialize() {
            currentWordNum = 0;
            wordWrong = false;
            wrongCharPos = -1;
            maxLenRightWord = 0;

            ((Text) wordsPane.getChildren().get(currentWordNum)).setUnderline(true);
        }

        // TODO Lock ctrl+V and selection text
        @Override
        public void changed(ObservableValue<? extends String> observableWord, String oldWord, String newWord) {
            synchronized (player.getData()) {
                System.out.println(oldWord + " -> " + newWord);

                int lastCharPos = newWord.length() - 1;
                Text leftPart = (Text) wordsPane.getChildren().get(currentWordNum);
                Text rightPart = (Text) wordsPane.getChildren().get(currentWordNum + 1);
                String currentWord = words.get(currentWordNum);

                System.out.println("<" + leftPart.getText() + "> -> <" + rightPart.getText() + ">");

                if (!wordWrong) {
                    if (currentWord.startsWith(newWord)) {
                        if (newWord.length() > maxLenRightWord) {
                            player.getData().setInputCharsCount(player.getData().getInputCharsCount() + 1);
                            ++maxLenRightWord;
                            if (currentWord.equals(newWord)) {
                                maxLenRightWord = 0;
                                ((Text) wordsPane.getChildren().get(currentWordNum)).setUnderline(false);

                                if (currentWordNum == words.size() - 1) {
                                    player.getData().setFinishTime(
                                            Integer.parseInt(player.getRemainTimeProperty().get()));
                                    player.getGameStateProperty().set(GameState.STOPPED);
                                } else {
                                    ++currentWordNum;
                                    ((Text) wordsPane.getChildren().get(currentWordNum)).setUnderline(true);
                                }

                                Platform.runLater(() -> input.setText(""));
                            } else {
                                leftPart.setText(leftPart.getText() + rightPart.getText().charAt(0));
                                rightPart.setText(rightPart.getText().substring(1));
                            }
                        } else if (newWord.length() > oldWord.length()) {
                            leftPart.setText(leftPart.getText() + rightPart.getText().charAt(0));
                            rightPart.setText(rightPart.getText().substring(1));
                        } else if (newWord.length() < oldWord.length() && leftPart.getText().length() > 0) {
                            rightPart.setText(leftPart.getText().charAt(leftPart.getText().length() - 1) +
                                    rightPart.getText());
                            leftPart.setText(leftPart.getText().substring(0, leftPart.getText().length() - 1));
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

    private final InputCharsListener inputCharsListener = new InputCharsListener();

    private class GameStateListener implements ChangeListener<GameState> {
        @Override
        public void changed(ObservableValue<? extends GameState> observableValue, GameState gameState, GameState t1) {
            if (t1 == GameState.READY) {
                promptText.setVisible(false);

                timerDescr.setText("Игра начнется через:");

                words = new ArrayList<>(List.of(player.getText().get().split(" ")));

                wordsPane.getChildren().clear();
                for (int i = 0; i < words.size(); ++i) {
                    if (i == 0) {
                        Text left = new Text("");
                        left.setFont(Font.font(16.0));
                        left.setOpacity(0.2);
                        wordsPane.getChildren().add(left);

                        Text right = new Text((words.get(0)));
                        right.setFont(Font.font(16.0));
                        right.setOpacity(0.2);
                        wordsPane.getChildren().add(right);

                    } else {
                        Text text = new Text(words.get(i));
                        text.setFont(Font.font(16.0));
                        text.setOpacity(0.2);
                        wordsPane.getChildren().add(text);
                    }
                }

                for (int i = 0; i < words.size() - 1; ++i) {
                    words.set(i, words.get(i) + " ");
                }

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
}
