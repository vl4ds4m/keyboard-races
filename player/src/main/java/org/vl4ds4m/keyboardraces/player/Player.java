package org.vl4ds4m.keyboardraces.player;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

public class Player {
    private final PlayerData data;
    private final SimpleStringProperty text = new SimpleStringProperty("");
    private final ObservableList<PlayerData> playersDataList =
            FXCollections.observableArrayList(Collections.nCopies(3, null));
    private int playerNum;

    public Player(String name) {
        data = new PlayerData(name);
    }

    public void connectToServer(String serverAddress, int serverPort) {
        new Thread(() -> {
            try (Socket socket = new Socket(serverAddress, serverPort);
                 ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream())) {

                String textObject = (String) reader.readObject();
                playerNum = reader.readInt();

                Platform.runLater(() -> text.setValue(textObject));

                while (true) {
                    writer.reset();
                    writer.writeObject(data);
                    writer.flush();

                    List<PlayerData> newList = (List<PlayerData>) reader.readObject();

                    Platform.runLater(() -> {
                        for (int i = 0; i < newList.size(); ++i) {
                            playersDataList.set(i, newList.get(i));
                        }
                    });

                    Thread.sleep(1000);
                }
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public PlayerData getData() {
        return data;
    }

    public ObservableList<PlayerData> getPlayersDataList() {
        return playersDataList;
    }


    public SimpleStringProperty getText() {
        return text;
    }
}
