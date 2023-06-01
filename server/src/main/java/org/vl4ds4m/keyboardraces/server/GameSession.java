package org.vl4ds4m.keyboardraces.server;

import org.vl4ds4m.keyboardraces.player.PlayerData;

import java.io.*;
import java.net.Socket;
import java.util.*;

class GameSession {
    private static final String TEXTS_DIR = "/Texts/";
    private static final List<String> TEXTS = List.of("email-text.txt", "hello.txt", "example.txt");
    private final List<PlayerData> playersDataList =
            new ArrayList<>(Collections.nCopies(3, new PlayerData("noname")));
    private int playersCount = 0;
    private final int textNum = (new Random()).nextInt(TEXTS.size());

    public void addPlayer(Socket playerSocket) {
        if (playersCount < 3) {
            int playerNum = playersCount;
            new Thread(() -> handlePlayerRequests(playerSocket, playerNum)).start();
            ++playersCount;
        } else {
            throw new RuntimeException("Players count must be <= 3.");
        }
    }

    private void handlePlayerRequests(Socket socket, int playerNum) {
        try (socket) {
            ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream());

            try (BufferedReader textReader = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(Server.class.getResourceAsStream(TEXTS_DIR + TEXTS.get(textNum)))))) {

                String text = textReader.readLine();
                writer.writeObject(text);
                writer.writeInt(playerNum);
                writer.flush();

                ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());

                while (true) {
                    PlayerData playerData = (PlayerData) reader.readObject();
                    playersDataList.set(playerNum, playerData);

                    writer.reset();
                    writer.writeObject(playersDataList);
                    writer.flush();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public int getPlayersCount() {
        return playersCount;
    }
}
