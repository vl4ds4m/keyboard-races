package org.vl4ds4m.keyboardraces.server;

import org.vl4ds4m.keyboardraces.player.PlayerData;

import java.io.*;
import java.net.Socket;
import java.util.*;

class GameSession {
    private static final String TEXTS_DIR = "/Texts/";
    private static final List<String> TEXTS = List.of("email-text.txt", "hello.txt", "example.txt");
    private final List<PlayerData> playersDataList = new ArrayList<>(Collections.nCopies(3, null));
    private int playersCount = 0;
    private final int textNum = (new Random()).nextInt(TEXTS.size());

    public synchronized void addPlayer(Socket playerSocket) {
        if (playersCount < 3) {
            new Thread(() -> handlePlayerRequests(playerSocket, playersCount)).start();
            ++playersCount;
        } else {
            throw new RuntimeException("Players count must be <= 3.");
        }
    }

    private void handlePlayerRequests(Socket socket, int playerNum) {
        try (socket) {
            //ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream());

            try (BufferedReader textReader = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(Server.class.getResourceAsStream(TEXTS_DIR + TEXTS.get(textNum)))))) {

                String text = textReader.readLine();
                writer.writeObject(text);
                writer.writeInt(playerNum);
                writer.flush();

                /*while (true) {
                    PlayerData playerData = (PlayerData) reader.readObject();
                    playersDataList.set(playerNum, playerData);

                    writer.writeObject(playersDataList);
                    writer.flush();
                }*/
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getPlayersCount() {
        return playersCount;
    }
}
