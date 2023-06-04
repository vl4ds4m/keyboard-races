package org.vl4ds4m.keyboardraces.player;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Player {
    private final PlayerData data;
    private final SimpleStringProperty text = new SimpleStringProperty("");
    private final ObservableList<PlayerResult> playersResultsList = FXCollections.observableArrayList();

    public Player(String name) {
        data = new PlayerData(name);
    }

    public void connectToServer(String serverAddress, int serverPort) {
        new Thread(() -> {
            try (Socket socket = new Socket(serverAddress, serverPort);
                 ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream())) {

                String textObject = (String) reader.readObject();
                int playerId = reader.readInt();

                Platform.runLater(() -> text.setValue(textObject));

                while (true) {
                    writer.reset();
                    writer.writeObject(data);
                    writer.flush();

                    List<?> newDataList = (List<?>) reader.readObject();

                    List<PlayerResult> newResultList = new ArrayList<>();
                    for (int i = 0; i < newDataList.size(); ++i) {
                        newResultList.add(new PlayerResult((PlayerData) newDataList.get(i)));
                        if (i == playerId) {
                            newResultList.get(i).setCurrentPlayer(true);
                        }
                    }
                    newResultList.sort(PlayerResult.COMPARATOR);

                    Platform.runLater(() -> {
                        for (int i = 0; i < newResultList.size(); ++i) {
                            if (i == playersResultsList.size()) {
                                playersResultsList.add(newResultList.get(i));
                            } else {
                                playersResultsList.set(i, newResultList.get(i));
                            }
                        }
                    });

                    Thread.sleep(1000);
                }
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                data.setConnected(false);
                throw new RuntimeException(e);
            }
        }).start();
    }

    public PlayerData getData() {
        return data;
    }

    public ObservableList<PlayerResult> getPlayersResultsList() {
        return playersResultsList;
    }


    public SimpleStringProperty getText() {
        return text;
    }
}
