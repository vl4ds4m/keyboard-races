package org.vl4ds4m.keyboardraces.player;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

public class Player {
    private final PlayerData data;
    private String text = "";
    private boolean textReceivedValue = false;
    private List<PlayerData> playersDataList;

    public Player(String name) {
        data = new PlayerData(name);
    }

    public void connectToServer(String serverAddress, int serverPort) {
        new Thread(() -> {
            try (Socket socket = new Socket(serverAddress, serverPort)) {
                ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());
                //ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream());

                text = (String) reader.readObject();
                textReceivedValue = true;
                int playerNum = reader.readInt();

                /*while (true) {
                    writer.writeObject(data);
                    writer.flush();

                    playersDataList = (List<PlayerData>) reader.readObject();
                    data.updateInputValues(playersDataList.get(playerNum));

                    Thread.sleep(1000);
                }*/
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public PlayerData getData() {
        return data;
    }

    public List<PlayerData> getPlayersDataList() {
        return Collections.unmodifiableList(playersDataList);
    }

    public boolean textReceived() {
        return textReceivedValue;
    }

    public String getText() {
        return text;
    }
}
