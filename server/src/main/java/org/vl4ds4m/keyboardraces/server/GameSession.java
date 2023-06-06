package org.vl4ds4m.keyboardraces.server;

import org.vl4ds4m.keyboardraces.game.PlayerData;
import org.vl4ds4m.keyboardraces.game.ServerCommand;

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
    private final List<PlayerData> playerDataList = new ArrayList<>();
    private final String text;
    private volatile boolean gameReady = false;
    private volatile boolean gameStarted = false;
    private volatile boolean gameStopped = false;
    private volatile int remainTime = AWAIT_TIME;

    public GameSession() {
        int textNum = new Random().nextInt(TEXTS.size());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                GameSession.class.getResourceAsStream(TEXTS_DIR + TEXTS.get(textNum)))))) {
            text = reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized boolean addPlayer(Socket playerSocket) {
        if (!gameReady) {
            PlayerHandler handler = new PlayerHandler(playerSocket, handlers.size());

            //playerDataList.add(new PlayerData("Noname"));
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

                if (remainTime == 0) {
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

                initGame(reader, writer);
                System.out.println(this + " INIT");

                synchronized (this) {
                    while (!gameReady) {
                        updateGame(reader, writer);
                        this.wait();
                    }
                    writer.writeObject(ServerCommand.READY);
                    writer.flush();

                    while (!gameStarted) {
                        updateGame(reader, writer);
                        this.wait();
                    }
                    writer.writeObject(ServerCommand.START);
                    writer.flush();

                    while (!gameStopped) {
                        updateGame(reader, writer);
                        this.wait();
                    }
                    writer.writeObject(ServerCommand.STOP);
                    writer.flush();
                }
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                System.out.println(this + " DISCONNECT, message: " + e);
                synchronized (GameSession.this) {
                    playerDataList.get(playerNum).setConnected(false);
                }
            }
        }

        private void initGame(
                ObjectInputStream reader,
                ObjectOutputStream writer
        ) throws IOException, ClassNotFoundException {

            writer.writeObject(ServerCommand.NEED_NAME);
            writer.flush();

            String playerName = (String) reader.readObject();
            synchronized (GameSession.this) {
                playerDataList.add(new PlayerData(playerName));
            }

            writer.writeObject(ServerCommand.TEXT);
            writer.writeObject(text);

            writer.writeObject(ServerCommand.PLAYER_NUM);
            writer.writeInt(playerNum);

            writer.flush();
        }

        private void updateGame(
                ObjectInputStream reader,
                ObjectOutputStream writer
        ) throws IOException, ClassNotFoundException {

            writer.writeObject(ServerCommand.NEED_COUNTS);
            writer.flush();

            int inputCharsCount = reader.readInt();
            int errorsCount = reader.readInt();
            synchronized (GameSession.this) {
                playerDataList.get(playerNum).setInputCharsCount(inputCharsCount);
                playerDataList.get(playerNum).setErrorsCount(errorsCount);
            }

            writer.writeObject(ServerCommand.DATA_LIST);
            writer.reset();
            writer.writeObject(playerDataList);

            writer.writeObject(ServerCommand.TIME);
            writer.writeInt(remainTime);

            writer.flush();
        }
    }
}
