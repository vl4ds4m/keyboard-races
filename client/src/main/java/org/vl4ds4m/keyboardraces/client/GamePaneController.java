package org.vl4ds4m.keyboardraces.client;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class GamePaneController {
    static Player player;
    @FXML
    private Label text;
    @FXML
    private TextField input;

    static void createPlayer(String serverAddress, int serverPort) {
        player = new Player(serverAddress, serverPort);
    }

    static void connectToServer() {
        player.connectToServer();
    }

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
            writeTextManually(player.getText());
        } else {
            System.out.println("Text hasn't been received yet!");
        }
    }

    private List<String> words;
    private final SimpleBooleanProperty gameOver = new SimpleBooleanProperty(false);
    private final AtomicInteger currentWordNum = new AtomicInteger(0);
    private final AtomicInteger inputCharsCount = new AtomicInteger(0);
    private final AtomicInteger inputErrorsCount = new AtomicInteger(0);
    private final AtomicBoolean wrongWord = new AtomicBoolean(false);
    private final AtomicInteger wrongCharPos = new AtomicInteger(-1);
    private final AtomicInteger maxLenRightWord = new AtomicInteger(-1);
    private ChangeListener<String> inputWordsListener;
    private ChangeListener<String> inputCharsListener;

    @FXML
    private Label chCnt;
    @FXML
    private Label errCnt;

    private void writeTextManually(String originalText) {
        words = new ArrayList<>(List.of(originalText.split(" ")));
        for (int i = 0; i < words.size() - 1; ++i) {
            words.set(i, words.get(i) + " ");
        }
        gameOver.setValue(false);
        currentWordNum.set(0);
        inputCharsCount.set(0);
        inputErrorsCount.set(0);
        wrongWord.set(false);
        wrongCharPos.set(-1);
        maxLenRightWord.set(0);

        chCnt.setText("0");
        errCnt.setText("0");

        inputWordsListener = makeInputWordsListener();
        inputCharsListener = makeInputCharsListener();

        input.textProperty().addListener(inputWordsListener);
        input.textProperty().addListener(inputCharsListener);
        gameOver.addListener(makeGameOverListener());
    }

    private ChangeListener<String> makeInputWordsListener() {
        return (observableWord, oldWord, newWord) -> {
            boolean wordEntered = currentWordNum.get() <= words.size() - 1 &&
                    newWord.equals(words.get(currentWordNum.get()));
            boolean lastWordEntered = currentWordNum.get() == words.size() - 1 &&
                    newWord.equals(words.get(currentWordNum.get()));
            if (wordEntered) {
                input.clear(); // TODO Correct exceptions

                wrongWord.set(false);
                wrongCharPos.set(-1);
                maxLenRightWord.set(0);

                if (lastWordEntered) {
                    System.out.println(words.get(currentWordNum.get()) + " -> END");
                    gameOver.set(true);
                } else {
                    System.out.println(words.get(currentWordNum.get()) + " -> " +
                            words.get(currentWordNum.incrementAndGet()));
                }
            }
        };
    }

    private ChangeListener<String> makeInputCharsListener() { // TODO Lock ctrl+V and selection text
        return (observableWord, oldWord, newWord) -> {
            System.out.println(oldWord + " -> " + newWord);

            int lastCharPos = newWord.length() - 1;
            String currentWord = words.get(currentWordNum.get());

            if (!wrongWord.get()) {
                if (currentWord.startsWith(newWord)) {
                    if (newWord.length() > maxLenRightWord.get()) {
                        inputCharsCount.incrementAndGet();
                        maxLenRightWord.incrementAndGet();

                        chCnt.setText(inputCharsCount.toString());
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
        };
    }

    private ChangeListener<Boolean> makeGameOverListener() {
        return (observableGameOver, oldGameOver, gameOver) -> {
            if (gameOver) {
                input.textProperty().removeListener(inputWordsListener);
                input.textProperty().removeListener(inputCharsListener);
                text.setDisable(true);
            }
        };
    }
}
