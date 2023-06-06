package org.vl4ds4m.keyboardraces.client;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.vl4ds4m.keyboardraces.game.PlayerData;
import org.vl4ds4m.keyboardraces.game.ServerCommand;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

class Player implements Runnable {
    private final PlayerData data;
    private final String serverAddress;
    private final int serverPort;
    private int playerNum = -1;
    private final SimpleStringProperty text = new SimpleStringProperty("");
    private final ObservableList<PlayerData> playerDataList = FXCollections.observableArrayList();
    private final SimpleBooleanProperty gameReadyProperty = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty gameStartProperty = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty gameStopProperty = new SimpleBooleanProperty(false);
    private final SimpleStringProperty remainTimeProperty = new SimpleStringProperty();

    public Player(String name, String serverAddress, int serverPort) {
        data = new PlayerData(name);
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;

    }

    public PlayerData getData() {
        return data;
    }

    public ObservableList<PlayerData> getPlayerDataList() {
        return playerDataList;
    }

    public SimpleStringProperty getText() {
        return text;
    }

    public SimpleBooleanProperty getGameReadyProperty() {
        return gameReadyProperty;
    }

    public SimpleBooleanProperty getGameStartProperty() {
        return gameStartProperty;
    }

    public SimpleBooleanProperty getGameStopProperty() {
        return gameStopProperty;
    }

    public SimpleStringProperty getRemainTimeProperty() {
        return remainTimeProperty;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream())) {

            ServerCommand command;
            while ((command = (ServerCommand) reader.readObject()) != ServerCommand.STOP) {
                executeCommand(command, reader, writer);
            }
            Platform.runLater(() -> gameStopProperty.set(true));

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void executeCommand(
            ServerCommand command,
            ObjectInputStream reader,
            ObjectOutputStream writer
    ) throws IOException, ClassNotFoundException {

        switch (command) {
            case TEXT -> {
                String textObject = (String) reader.readObject();
                Platform.runLater(() -> text.set(textObject));
            }

            case NEED_NAME -> {
                writer.writeObject(data.getName());
                writer.flush();
            }

            case PLAYER_NUM -> playerNum = reader.readInt();

            case READY -> Platform.runLater(() -> gameReadyProperty.set(true));

            case START -> Platform.runLater(() -> gameStartProperty.set(true));

            case TIME -> {
                String time = String.valueOf(reader.readInt());
                Platform.runLater(() -> remainTimeProperty.set(time));
            }

            case NEED_COUNTS -> {
                writer.writeInt(data.getInputCharsCount());
                writer.writeInt(data.getErrorsCount());
                writer.flush();
            }

            case DATA_LIST -> {
                List<PlayerData> newPlayerDataList = (List<PlayerData>) reader.readObject();

                for (int i = 0; i < newPlayerDataList.size(); ++i) {
                    if (i == playerNum) {
                        newPlayerDataList.get(i).setCurrentPlayer(true);
                    }
                }

                newPlayerDataList.sort(PlayerData.RATE_COMP);

                Platform.runLater(() -> {
                    for (int i = 0; i < newPlayerDataList.size(); ++i) {
                        if (i == playerDataList.size()) {
                            playerDataList.add(newPlayerDataList.get(i));
                        } else {
                            playerDataList.set(i, newPlayerDataList.get(i));
                        }
                    }
                });
            }
        }
    }
}
