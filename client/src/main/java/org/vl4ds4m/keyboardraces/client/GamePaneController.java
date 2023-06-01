package org.vl4ds4m.keyboardraces.client;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
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

    static void createPlayer(String name, String serverAddress, int serverPort) {
        player = new Player(name);
        player.connectToServer(serverAddress, serverPort);
    }

    private static Player player;
    private List<String> words;
    private final SimpleBooleanProperty gameOver = new SimpleBooleanProperty(false);
    //private static SimpleIntegerProperty inputCharsCount; //private final AtomicInteger inputCharsCount = new AtomicInteger(0);
    //private static SimpleIntegerProperty errorsCount; //private final AtomicInteger inputErrorsCount = new AtomicInteger(0);
    private static int currentWordNum; //private final AtomicInteger currentWordNum = new AtomicInteger(0);
    private static boolean wordWrong; //private final AtomicBoolean wrongWord = new AtomicBoolean(false);
    private static int wrongCharPos; //private final AtomicInteger wrongCharPos = new AtomicInteger(-1);
    private static int maxLenRightWord; //private final AtomicInteger maxLenRightWord = new AtomicInteger(-1);
    private static ChangeListener<String> inputCharsListener;

    /*@FXML
    private Label chCnt;
    @FXML
    private Label errCnt;*/

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
        wordWrong = false; //wrongWord.set(false);
        currentWordNum = 0; //currentWordNum.set(0);
        player.getData().setInputCharsCount(0); //inputCharsCount.set(0);
        player.getData().setErrorsCount(0); //inputErrorsCount.set(0);
        maxLenRightWord = 0; //maxLenRightWord.set(0);
        wrongCharPos = -1; //wrongCharPos.set(-1);

        //chCnt.setText("0");
        //errCnt.setText("0");
    }

    // TODO Lock ctrl+V and selection text
    private void listenInputChars(
            ObservableValue<? extends String> observableWord,
            String oldWord,
            String newWord) {
        System.out.println(oldWord + " -> " + newWord);

        int lastCharPos = newWord.length() - 1;
        String currentWord = words.get(currentWordNum);

        if (!wordWrong) {
            if (currentWord.startsWith(newWord)) {
                if (newWord.length() > maxLenRightWord) {
                    player.getData().setInputCharsCount(player.getData().getInputCharsCount() + 1);
                    ++maxLenRightWord;

                    //chCnt.setText(Integer.toString(inputCharsCount));

                    if (currentWord.equals(newWord)) {
                        // TODO Correct exceptions
                        input.clear();

                        maxLenRightWord = 0;

                        if (currentWordNum == words.size() - 1) {
                            System.out.println(words.get(currentWordNum) + " --> END");

                            gameOver.set(true);
                        } else {
                            System.out.println(words.get(currentWordNum) + " --> " +
                                    words.get(currentWordNum + 1));

                            ++currentWordNum;
                        }
                    }
                }
            } else {
                wordWrong = true;
                player.getData().setErrorsCount(player.getData().getErrorsCount() + 1);
                wrongCharPos = lastCharPos;

                //errCnt.setText(Integer.toString(errorsCount));
            }
        } else {
            if (currentWord.startsWith(newWord) && newWord.length() == wrongCharPos) {
                wordWrong = false;
                wrongCharPos = -1;
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
