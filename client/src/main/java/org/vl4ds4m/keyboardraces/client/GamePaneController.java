package org.vl4ds4m.keyboardraces.client;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.vl4ds4m.keyboardraces.player.Player;
import org.vl4ds4m.keyboardraces.player.PlayerData;

import java.util.ArrayList;
import java.util.List;


public class GamePaneController {
    @FXML
    private Label text;
    @FXML
    private TextField input;
    @FXML
    private ListView<PlayerData> resultsTable;

    @FXML
    private void initialize() {
        input.disableProperty().bind(text.disableProperty());
    }

    @FXML
    private void clickStartButton() {
        resultsTable.setItems(player.getPlayersDataList());
        text.textProperty().bind(player.getText());
        text.setDisable(false);
        input.requestFocus();
        playGame();
    }

    private static Player player;
    private static List<String> words;
    private static final SimpleBooleanProperty gameOver = new SimpleBooleanProperty();
    private static int currentWordNum;
    private static boolean wordWrong;
    private static int wrongCharPos;
    private static int maxLenRightWord;
    private static ChangeListener<String> inputCharsListener;

    static void createPlayer(String name, String serverAddress, int serverPort) {
        player = new Player(name);
        player.connectToServer(serverAddress, serverPort);
    }

    private void playGame() {
        initGameVar();

        inputCharsListener = this::listenInputChars;
        input.textProperty().addListener(inputCharsListener);

        gameOver.addListener((observableValue, oldValue, newValue) -> {
            if (newValue) {
                input.textProperty().removeListener(inputCharsListener);
                text.setDisable(true);
            }
        });
    }

    private void initGameVar() {
        words = new ArrayList<>(List.of(text.getText().split(" ")));
        for (int i = 0; i < words.size() - 1; ++i) {
            words.set(i, words.get(i) + " ");
        }

        gameOver.set(false);
        wordWrong = false;
        currentWordNum = 0;
        player.getData().setInputCharsCount(0);
        player.getData().setErrorsCount(0);
        maxLenRightWord = 0;
        wrongCharPos = -1;
    }

    // TODO Lock ctrl+V and selection text
    private void listenInputChars(
            ObservableValue<? extends String> observableWord,
            String oldWord,
            String newWord) {
        for (int i = 0; i < player.getPlayersDataList().size(); ++i) {
            System.out.println("i: " + player.getPlayersDataList().get(i));
        }
        System.out.println(oldWord + " -> " + newWord);

        int lastCharPos = newWord.length() - 1;
        String currentWord = words.get(currentWordNum);

        if (!wordWrong) {
            if (currentWord.startsWith(newWord)) {
                if (newWord.length() > maxLenRightWord) {
                    player.getData().setInputCharsCount(player.getData().getInputCharsCount() + 1);
                    ++maxLenRightWord;

                    if (currentWord.equals(newWord)) {
                        input.setText(""); // TODO Correct exceptions

                        maxLenRightWord = 0;

                        if (currentWordNum == words.size() - 1) {
                            gameOver.set(true);
                        } else {
                            ++currentWordNum;
                        }
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
