package org.vl4ds4m.keyboardraces.server;

import org.vl4ds4m.keyboardraces.game.GameSettings;
import org.vl4ds4m.keyboardraces.game.GameState;
import org.vl4ds4m.keyboardraces.game.PlayerData;
import org.vl4ds4m.keyboardraces.game.ServerCommand;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameSession {
    private static final String TEXTS_DIR = "/Texts/";
    private static final List<String> TEXTS = List.of("vk.txt", "msk.txt", "Elon.txt", "minecraft.txt", "titanic.txt");
    private final ScheduledExecutorService gameExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService playersExecutor = Executors.newFixedThreadPool(GameSettings.MAX_PLAYERS_COUNT);
    private final List<PlayerHandler> handlers = new ArrayList<>();
    private final List<PlayerData> playerDataList = new ArrayList<>();
    private final String text;
    private GameState gameState = GameState.INIT;
    private volatile int remainTime = GameSettings.AWAIT_TIME;

    public GameSession() throws Exception {
        int textNum = new Random().nextInt(TEXTS.size());
        try (InputStream inputStream = Objects.requireNonNull(GameSession.class
                .getResourceAsStream(TEXTS_DIR + TEXTS.get(textNum)));
             InputStreamReader inputStreamReader =
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(inputStreamReader)
        ) {
            text = reader.readLine();
        } catch (IOException e) {
            throw new Exception(e);
        }
    }

    public synchronized boolean addPlayer(Socket playerSocket) {
        if (handlers.size() < GameSettings.MAX_PLAYERS_COUNT && gameState == GameState.INIT) {
            PlayerHandler handler = new PlayerHandler(playerSocket);
            handlers.add(handler);
            playersExecutor.execute(handler);

            if (handlers.size() == 1) {
                gameExecutor.scheduleAtFixedRate(new GameHandler(), 0, 1, TimeUnit.SECONDS);
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

                if (playersFinishedGame()) {
                    finishGame();
                } else if (gameState == GameState.INIT && handlers.size() == GameSettings.MAX_PLAYERS_COUNT) {
                    gameState = GameState.READY;
                    remainTime = GameSettings.COUNTDOWN_TIME;
                } else if (remainTime == 0) {
                    if (gameState == GameState.INIT) {
                        gameState = GameState.READY;
                        remainTime = GameSettings.COUNTDOWN_TIME;
                    } else if (gameState == GameState.READY) {
                        gameState = GameState.STARTED;
                        remainTime = GameSettings.GAME_DURATION_TIME;
                    } else if (gameState == GameState.STARTED) {
                        finishGame();
                    }
                }

                GameSession.this.notifyAll();
            }
        }

        private void finishGame() {
            gameState = GameState.STOPPED;
            playersExecutor.shutdown();
            gameExecutor.shutdown();
        }

        private boolean playersFinishedGame() {
            boolean playersFinishedGame = true;
            for (PlayerHandler playerHandler : handlers) {
                if (playerHandler.data.connected() && playerHandler.data.getInputCharsCount() < text.length()) {
                    playersFinishedGame = false;
                    break;
                }
            }
            return playersFinishedGame;
        }
    }

    private class PlayerHandler implements Runnable {
        private final Socket socket;
        private PlayerData data = new PlayerData("NewPlayer");

        private PlayerHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            synchronized (GameSession.this) {
                try (socket;
                     ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream reader = new ObjectInputStream(socket.getInputStream())) {

                    System.out.println(this + " INIT");
                    initGame(reader, writer);

                    while (gameState != GameState.STOPPED) {
                        GameState currentState = gameState;

                        if (currentState == GameState.READY) {
                            writer.writeObject(ServerCommand.PLAYER_NUM);
                            writer.writeInt(playerDataList.indexOf(data));
                            writer.flush();
                        }

                        while (currentState == gameState) {
                            if (currentState == GameState.INIT || currentState == GameState.READY) {
                                sendGameData(writer);
                            } else {
                                updateGame(reader, writer);
                            }

                            GameSession.this.wait();
                        }

                        writer.writeObject(stateToCommand(gameState));
                        writer.flush();
                    }

                    System.out.println(this + " EXIT");

                } catch (IOException | ClassNotFoundException | InterruptedException e) {
                    System.out.println(this + " DISCONNECT, message: " + e);

                    if (gameState == GameState.INIT) {
                        handlers.remove(this);
                        playerDataList.remove(data);
                    } else {
                        data.setConnected(false);
                    }
                }
            }
        }

        private ServerCommand stateToCommand(GameState gameState) {
            ServerCommand serverCommand;
            switch (gameState) {
                case READY -> serverCommand = ServerCommand.READY;
                case STARTED -> serverCommand = ServerCommand.START;
                case STOPPED -> serverCommand = ServerCommand.STOP;
                default -> throw new RuntimeException("Invalid game state for stateToCommand function.");
            }
            return serverCommand;
        }

        private void initGame(
                ObjectInputStream reader,
                ObjectOutputStream writer
        ) throws IOException, ClassNotFoundException {

            writer.writeObject(ServerCommand.NEED_NAME);
            writer.flush();

            String playerName = (String) reader.readObject();

            data = new PlayerData(playerName);
            playerDataList.add(data);

            writer.writeObject(ServerCommand.TEXT);
            writer.writeObject(text);

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
            int finishTime = reader.readInt();

            data.setInputCharsCount(inputCharsCount);
            data.setErrorsCount(errorsCount);
            data.setFinishTime(finishTime);

            sendGameData(writer);
        }

        private void sendGameData(ObjectOutputStream writer) throws IOException {
            writer.writeObject(ServerCommand.DATA_LIST);
            writer.reset();
            writer.writeObject(playerDataList);

            writer.writeObject(ServerCommand.TIME);
            writer.writeInt(remainTime);

            writer.flush();
        }
    }
}
