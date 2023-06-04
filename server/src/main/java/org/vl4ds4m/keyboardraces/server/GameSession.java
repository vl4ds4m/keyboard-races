package org.vl4ds4m.keyboardraces.server;

import org.vl4ds4m.keyboardraces.player.PlayerData;
import org.vl4ds4m.keyboardraces.player.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class GameSession {
    private static final String TEXTS_DIR = "/Texts/";
    private static final List<String> TEXTS = List.of(/*"email-text.txt",*/ "hello.txt", "example.txt");
    private static final int GAME_DURATION = 10;
    private static final int TIMEOUT = 5_000;
    private final List<PlayerData> playersDataList = new ArrayList<>();

    private final List<Socket> playersSockets = new ArrayList<>();
    private final int textNum = new Random().nextInt(TEXTS.size());
    private boolean gameLaunched = false;

    public synchronized boolean gameLaunched() {
        return gameLaunched;
    }

    public synchronized void addPlayer(Socket playerSocket) {
        if (!gameLaunched) {
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
    }

    private void launchGame() {
        gameLaunched = true;

        ExecutorService playersRequestsExecutor = Executors.newFixedThreadPool(3);

        for (int i = 0; i < playersSockets.size(); ++i) {
            playersRequestsExecutor.execute(new PlayerRequestsHandler(playersSockets.get(i), i));
        }
    }

    private class PlayerRequestsHandler implements Runnable {
        private final Socket socket;
        private final int playerNum;
        private final Object lock = new Object();
        private int remainTime = GAME_DURATION;

        private PlayerRequestsHandler(Socket socket, int playerNum) {
            this.socket = socket;
            this.playerNum = playerNum;
        }

        @Override
        public void run() {
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

            try (socket;
                 ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream())) {

                executorService.execute(new PlayerGameInitializer(writer));

                try (ObjectInputStream reader = new ObjectInputStream(socket.getInputStream())) {
                    PlayerGameUpdater playerGameUpdater = new PlayerGameUpdater(reader, writer);
                    executorService.scheduleAtFixedRate(playerGameUpdater, 5, 1, TimeUnit.SECONDS);

                    while (true) {
                        synchronized (lock) {
                            if (remainTime == 0) {
                                executorService.shutdown();
                                break;
                            }
                            lock.wait();
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                playersDataList.get(playerNum).setConnected(false);
                throw new RuntimeException(e);
            }
        }

        private class PlayerGameInitializer implements Runnable {
            private final ObjectOutputStream writer;

            private PlayerGameInitializer(ObjectOutputStream writer) {
                this.writer = writer;
            }

            @Override
            public void run() {
                try (BufferedReader textReader = new BufferedReader(new InputStreamReader(
                        Objects.requireNonNull(Server.class.getResourceAsStream(TEXTS_DIR + TEXTS.get(textNum)))))) {

                    String text = textReader.readLine();
                    writer.writeObject(Protocol.TEXT);
                    writer.writeObject(text);

                    writer.writeObject(Protocol.PLAYER_NUM);
                    writer.writeInt(playerNum);

                    writer.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private class PlayerGameUpdater implements Runnable {
            private final ObjectInputStream reader;
            private final ObjectOutputStream writer;

            private PlayerGameUpdater(ObjectInputStream reader, ObjectOutputStream writer) {
                this.reader = reader;
                this.writer = writer;
            }

            @Override
            public void run() {
                try {
                    PlayerData playerData = (PlayerData) reader.readObject();
                    playersDataList.set(playerNum, playerData);

                    writer.reset();

                    writer.writeObject(Protocol.DATA_LIST);
                    writer.writeObject(playersDataList);

                    writer.writeObject(Protocol.TIME);
                    writer.writeInt(remainTime);
                    --remainTime;

                    writer.flush();

                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                synchronized (lock) {
                    notify();
                }
            }
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
