package org.vl4ds4m.keyboardraces.server;

import org.vl4ds4m.keyboardraces.game.PlayerData;
import org.vl4ds4m.keyboardraces.game.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameSession {
    private static final String TEXTS_DIR = "/Texts/";
    private static final List<String> TEXTS = List.of(/*"email-text.txt",*/ "hello.txt", "example.txt");
    private static final int MAX_PLAYERS_COUNT = 3;
    private static final int AWAIT_TIME = 10;
    private static final int COUNTDOWN_TIME = 3;
    private static final int GAME_DURATION_TIME = 30;
    private ScheduledExecutorService gameExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService playersExecutor = Executors.newFixedThreadPool(MAX_PLAYERS_COUNT);
    private final GameHandler gameHandler = new GameHandler();
    private final List<PlayerHandler> handlers = new ArrayList<>();
    private final List<PlayerData> playersDataList = new ArrayList<>();
    private final int textNum = new Random().nextInt(TEXTS.size());
    private volatile boolean gameReady = false;
    private volatile boolean gameStarted = false;
    private volatile boolean gameStopped = false;
    private volatile int remainTime = AWAIT_TIME;

    public synchronized boolean addPlayer(Socket playerSocket) {
        if (!gameReady) {
            PlayerHandler handler = new PlayerHandler(playerSocket, handlers.size());

            playersDataList.add(new PlayerData("Noname"));
            handlers.add(handler);
            playersExecutor.execute(handler);

            if (handlers.size() == 1) {
                gameExecutor.scheduleAtFixedRate(gameHandler, 0, 1, TimeUnit.SECONDS);
            } else if (handlers.size() == MAX_PLAYERS_COUNT) {
                gameExecutor.shutdown();

                gameReady = true;
                remainTime = COUNTDOWN_TIME;

                gameExecutor = Executors.newSingleThreadScheduledExecutor();
                gameExecutor.scheduleAtFixedRate(gameHandler, 0, 1, TimeUnit.SECONDS);
            }

            return true;
        }
        return false;
    }

    private class GameHandler implements Runnable {
        @Override
        public void run() {
            synchronized (GameSession.this) {
                remainTime -= 1;

                if (remainTime < 0) {
                    if (!gameReady) {
                        gameReady = true;
                        remainTime = COUNTDOWN_TIME;
                    } else if (!gameStarted) {
                        gameStarted = true;
                        remainTime = GAME_DURATION_TIME;
                    } else if (!gameStopped) {
                        gameStopped = true;
                        playersExecutor.shutdown();
                        gameExecutor.shutdown();
                    }
                }

                for (var handler : handlers) {
                    synchronized (handler) {
                        handler.notify();
                    }
                }
            }
        }
    }

    private class PlayerHandler implements Runnable {
        private final Socket socket;
        private final int playerNum;

        private PlayerHandler(Socket socket, int playerNum) {
            this.socket = socket;
            this.playerNum = playerNum;
        }

        @Override
        public void run() {
            try (socket;
                 ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream reader = new ObjectInputStream(socket.getInputStream())) {

                initGame(writer);
                System.out.println(this + " INIT");

                synchronized (this) {
                    while (!gameStarted) {
                        updateGame(reader, writer);
                        this.wait();
                    }
                    writer.writeObject(Protocol.START);
                    writer.flush();

                    while (!gameStopped) {
                        updateGame(reader, writer);
                        this.wait();
                    }
                    writer.writeObject(Protocol.STOP);
                    writer.flush();
                }
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                System.out.println(this + " DISCONNECT, message: " + e);
                playersDataList.get(playerNum).setConnected(false);
            }
        }

        private void initGame(ObjectOutputStream writer) throws IOException {
            try (BufferedReader textReader = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(Server.class.getResourceAsStream(TEXTS_DIR + TEXTS.get(textNum)))
            ))) {
                String text = textReader.readLine();
                writer.writeObject(Protocol.TEXT);
                writer.writeObject(text);

                writer.writeObject(Protocol.PLAYER_NUM);
                writer.writeInt(playerNum);

                writer.flush();
            }
        }

        private void updateGame(
                ObjectInputStream reader,
                ObjectOutputStream writer
        ) throws IOException, ClassNotFoundException {

            writer.writeObject(Protocol.DATA);
            writer.flush();

            PlayerData playerData = (PlayerData) reader.readObject();
            playersDataList.set(playerNum, playerData);

            writer.reset();

            writer.writeObject(Protocol.DATA_LIST);
            writer.writeObject(playersDataList);

            writer.writeObject(Protocol.TIME);
            writer.writeInt(remainTime);

            writer.flush();
        }
    }
}
