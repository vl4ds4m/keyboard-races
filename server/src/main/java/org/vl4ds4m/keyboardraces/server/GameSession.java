package org.vl4ds4m.keyboardraces.server;

import org.vl4ds4m.keyboardraces.player.PlayerData;

import java.io.*;
import java.net.Socket;
import java.util.*;

class GameSession {
    private static final String TEXTS_DIR = "/Texts/";
    private static final List<String> TEXTS = List.of(/*"email-text.txt",*/ "hello.txt", "example.txt");
    private static final int TIMEOUT = 5_000;
    private final List<PlayerData> playersDataList = new ArrayList<>();

    private final List<Socket> playersSockets = new ArrayList<>();
    private final int textNum = new Random().nextInt(TEXTS.size());
    private boolean gameLaunched = false;

    public synchronized boolean gameLaunched() {
        return gameLaunched;
    }

    public synchronized void addPlayer(Socket playerSocket) {
        if (playersSockets.size() == 0) {
            new Thread(new TimeoutGameLauncher()).start();
        }

        if (playersSockets.size() < 3) {
            playersSockets.add(playerSocket);
            playersDataList.add(null);
        } else {
            throw new RuntimeException("Players count must be <= 3.");
        }

        if (playersSockets.size() == 3) {
            launchGame();
        }
    }

    private void launchGame() {
        gameLaunched = true;
        for (int i = 0; i < playersSockets.size(); ++i) {
            int playerNum = i;
            new Thread(
                    () -> handlePlayerRequests(playersSockets.get(playerNum), playerNum)
            ).start();
        }
    }

    private void handlePlayerRequests(Socket socket, int playerNum) {
        try (socket;
             ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream())) {

            try (BufferedReader textReader = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(Server.class.getResourceAsStream(TEXTS_DIR + TEXTS.get(textNum)))))) {

                String text = textReader.readLine();
                writer.writeObject(text);
                writer.writeInt(playerNum);
                writer.flush();
            }

            try (ObjectInputStream reader = new ObjectInputStream(socket.getInputStream())) {
                while (true) {
                    PlayerData playerData = (PlayerData) reader.readObject();
                    playersDataList.set(playerNum, playerData);

                    writer.reset();
                    writer.writeObject(playersDataList);
                    writer.flush();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            playersDataList.get(playerNum).setConnected(false);
            throw new RuntimeException(e);
        }
    }

    private class TimeoutGameLauncher implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(TIMEOUT);
                synchronized (GameSession.this) {
                    if (!gameLaunched && playersSockets.size() < 3) {
                        launchGame();
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
