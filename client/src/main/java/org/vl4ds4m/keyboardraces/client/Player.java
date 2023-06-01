package org.vl4ds4m.keyboardraces.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class Player {
    private final String serverAddress;
    private final int serverPort;
    private String text;
    private boolean textReceivedValue = false;

    Player(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    boolean textReceived() {
        return textReceivedValue;
    }

    String getText() {
        return text;
    }

    void connectToServer() {
        new Thread(() -> {
            try (Socket socket = new Socket(serverAddress, serverPort);
                 ObjectInputStream textInputStream = new ObjectInputStream(socket.getInputStream())) {
                getTextFromServer(textInputStream);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    void getTextFromServer(ObjectInputStream textInputStream) throws IOException, ClassNotFoundException {
        text = (String) textInputStream.readObject();
        textReceivedValue = true;
    }
}
