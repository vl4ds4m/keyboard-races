package org.vl4ds4m.keyboardraces.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

class GameSession {
    private static final String TEXTS_DIR = "/Texts/";
    private static final List<String> TEXTS = List.of("email-text.txt", "hello.txt", "example.txt");
    private final List<Socket> playersData = new ArrayList<>();
    private int playersNum = 0;
    private final int textNum = (new Random()).nextInt(TEXTS.size());

    public int getPlayersNum() {
        return playersNum;
    }

    public void addPlayer(Socket playerSocket) {
        if (playersNum < 3) {
            ++playersNum;
        new Thread(() -> handlePlayerRequests(playerSocket)).start();
        } else {
            throw new RuntimeException("Players count must be <= 3.");
        }
    }

    private void handlePlayerRequests(Socket socket) {
        try (socket;
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                    Server.class.getResourceAsStream(TEXTS_DIR + TEXTS.get(textNum)))))) {

                String text = reader.readLine();
                outputStream.writeObject(text);
                outputStream.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
