package org.vl4ds4m.keyboardraces.client;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.vl4ds4m.keyboardraces.game.GameState;
import org.vl4ds4m.keyboardraces.game.PlayerData;
import org.vl4ds4m.keyboardraces.game.ServerCommand;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

class Player implements Runnable {
    private final PlayerData data;
    private final String serverAddress;
    private final String serverPort;
    private int playerNum = -1;
    private final SimpleStringProperty text = new SimpleStringProperty("");
    private final ObservableList<PlayerData> playerDataList = FXCollections.observableArrayList();
    private final SimpleObjectProperty<GameState> gameStateProperty = new SimpleObjectProperty<>(GameState.INIT);
    private final SimpleStringProperty remainTimeProperty = new SimpleStringProperty();
    private final SimpleStringProperty connectedProperty = new SimpleStringProperty("Connection");

    public Player(String name, String serverAddress, String serverPort) {
        data = new PlayerData(name);
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;

    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getServerPort() {
        return serverPort;
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

    public SimpleObjectProperty<GameState> getGameStateProperty() {
        return gameStateProperty;
    }

    public SimpleStringProperty getRemainTimeProperty() {
        return remainTimeProperty;
    }

    public SimpleStringProperty getConnectedProperty() {
        return connectedProperty;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(serverAddress, Integer.parseInt(serverPort));
             ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream())) {

            ServerCommand command;
            while ((command = (ServerCommand) reader.readObject()) != ServerCommand.STOP) {
                executeCommand(command, reader, writer);
            }
            Platform.runLater(() -> gameStateProperty.set(GameState.STOPPED));

        } catch (NumberFormatException e) {
            Platform.runLater(() -> connectedProperty.set("Порт сервера должен быть числом."));
        } catch (UnknownHostException e) {
            Platform.runLater(() -> connectedProperty.set("Нет такого подключения."));
        } catch (ConnectException e) {
            Platform.runLater(() -> connectedProperty.set("Данный порт недоступен для подключения."));
        } catch (Exception e) {
            Platform.runLater(() -> {
                if (e.getMessage() != null) {
                    connectedProperty.set(e.getMessage());
                } else {
                    connectedProperty.set("Неизвестная ошибка.");
                }
            });
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

            case READY -> Platform.runLater(() -> gameStateProperty.set(GameState.READY));

            case START -> Platform.runLater(() -> gameStateProperty.set(GameState.STARTED));

            case TIME -> {
                String time = String.valueOf(reader.readInt());
                Platform.runLater(() -> remainTimeProperty.set(time));
            }

            case NEED_COUNTS -> {
                synchronized (data) {
                    writer.writeInt(data.getInputCharsCount());
                    writer.writeInt(data.getErrorsCount());
                    writer.writeInt(data.getFinishTime());
                }
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
