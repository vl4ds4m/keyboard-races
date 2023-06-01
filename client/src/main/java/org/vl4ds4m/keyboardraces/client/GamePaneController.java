package org.vl4ds4m.keyboardraces.client;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.vl4ds4m.keyboardraces.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class GamePaneController {
    @FXML
    private Label text;
    @FXML
    private TextField input;
    @FXML
    private ListView<PlayerResult> resultsTable;

    @FXML
    private void initialize() {
        input.disableProperty().bind(text.disableProperty());
    }

    @FXML
    private void clickStartButton() {
        if (player.textReceived()) {
            text.setText(player.getText());
            text.setDisable(false);
            input.requestFocus();
            playGame();
        } else {
            System.out.println("Text hasn't been received yet!");
        }
    }

    static void createPlayer(String name, String serverAddress, int serverPort) {
        player = new Player(name);
        player.connectToServer(serverAddress, serverPort);
    }

    private static Player player;
    private List<String> words;
    private final SimpleBooleanProperty gameOver = new SimpleBooleanProperty(false);
    private final AtomicInteger currentWordNum = new AtomicInteger(0);
    private final AtomicInteger inputCharsCount = new AtomicInteger(0);
    private final AtomicInteger inputErrorsCount = new AtomicInteger(0);
    private final AtomicBoolean wrongWord = new AtomicBoolean(false);
    private final AtomicInteger wrongCharPos = new AtomicInteger(-1);
    private final AtomicInteger maxLenRightWord = new AtomicInteger(-1);
    private ChangeListener<String> inputCharsListener;

    @FXML
    private Label chCnt;
    @FXML
    private Label errCnt;

    private void playGame() {
        words = new ArrayList<>(List.of(text.getText().split(" ")));
        for (int i = 0; i < words.size() - 1; ++i) {
            words.set(i, words.get(i) + " ");
        }
        initGameVar();



//        resultsTable.
        inputCharsListener = this::listenInputChars;

        input.textProperty().addListener(inputCharsListener);

        gameOver.addListener(this::listenGameOver);
    }

    private void initGameVar() {
        gameOver.setValue(false);
        currentWordNum.set(0);
        inputCharsCount.set(0);
        inputErrorsCount.set(0);
        wrongWord.set(false);
        wrongCharPos.set(-1);
        maxLenRightWord.set(0);

        chCnt.setText("0");
        errCnt.setText("0");
    }

    // TODO Lock ctrl+V and selection text
    private void listenInputChars(
            ObservableValue<? extends String> observableWord,
            String oldWord,
            String newWord) {
        System.out.println(oldWord + " -> " + newWord);

        int lastCharPos = newWord.length() - 1;
        String currentWord = words.get(currentWordNum.get());

        if (!wrongWord.get()) {
            if (currentWord.startsWith(newWord)) {
                if (newWord.length() > maxLenRightWord.get()) {
                    inputCharsCount.incrementAndGet();
                    maxLenRightWord.incrementAndGet();

                    chCnt.setText(inputCharsCount.toString());

                    if (currentWord.equals(newWord)) {
                        // TODO Correct exceptions
                        input.clear();

                        maxLenRightWord.set(0);

                        if (currentWordNum.get() == words.size() - 1) {
                            System.out.println(words.get(currentWordNum.get()) + " --> END");
                            gameOver.set(true);
                        } else {
                            System.out.println(words.get(currentWordNum.get()) + " --> " +
                                    words.get(currentWordNum.incrementAndGet()));
                        }
                    }
                }
            } else {
                wrongWord.set(true);
                inputErrorsCount.incrementAndGet();
                wrongCharPos.set(lastCharPos);

                errCnt.setText(inputErrorsCount.toString());
            }
        } else {
            if (currentWord.startsWith(newWord) && newWord.length() == wrongCharPos.get()) {
                wrongWord.set(false);
                wrongCharPos.set(-1);
            }
        }
    }

    private void listenGameOver(
            ObservableValue<? extends Boolean> observableGameOver,
            Boolean oldGameOver,
            Boolean gameOver) {
        if (gameOver) {
            input.textProperty().removeListener(inputCharsListener);
            text.setDisable(true);
        }
    }
}
