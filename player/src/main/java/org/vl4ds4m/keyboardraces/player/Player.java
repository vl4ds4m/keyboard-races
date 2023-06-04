package org.vl4ds4m.keyboardraces.player;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Player {
    private final PlayerData data;
    private final SimpleStringProperty text = new SimpleStringProperty("");
    private final ObservableList<PlayerResult> playersResultsList = FXCollections.observableArrayList();
    private final SimpleBooleanProperty gameActive = new SimpleBooleanProperty(false);

    public Player(String name) {
        data = new PlayerData(name);
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

    public SimpleBooleanProperty getGameStateProperty() {
        return gameActive;
    }

    public void connectToServer(String serverAddress, int serverPort) {
        new Thread(() -> {
            try (Socket socket = new Socket(serverAddress, serverPort);
                 ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream())) {

                Protocol command;
                int playerId = -1;
                int remainTime = -1;

                while ((command = (Protocol) reader.readObject()) != Protocol.STOP) {
                    switch (command) {
                        case TEXT -> {
                            String textObject = (String) reader.readObject();
                            Platform.runLater(() -> text.set(textObject));
                        }

                        case PLAYER_NUM -> playerId = reader.readInt();

                        case START -> Platform.runLater(() -> gameActive.set(true));

                        case TIME -> remainTime = reader.readInt();

                        case DATA -> {
                            writer.reset();
                            writer.writeObject(data);
                            writer.flush();
                        }

                        case DATA_LIST -> {
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
                        }
                    }
                }
                Platform.runLater(() -> gameActive.set(false));
            } catch (IOException | ClassNotFoundException e) {
                data.setConnected(false);
                throw new RuntimeException(e);
            }
        }).start();
    }
}
