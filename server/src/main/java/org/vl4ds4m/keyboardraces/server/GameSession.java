package org.vl4ds4m.keyboardraces.server;

import org.vl4ds4m.keyboardraces.game.GameSettings;
import org.vl4ds4m.keyboardraces.game.GameState;
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                GameSession.class.getResourceAsStream(TEXTS_DIR + TEXTS.get(textNum)))))) {
            text = reader.readLine();
        } catch (IOException e) {
            throw new Exception(e);
        }
    }

    public synchronized boolean addPlayer(Socket playerSocket) {
        if (gameState == GameState.INIT) {
            PlayerHandler handler = new PlayerHandler(playerSocket, handlers.size());

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

                if (gameState == GameState.INIT && handlers.size() == GameSettings.MAX_PLAYERS_COUNT) {
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
                } else if (gameState == GameState.STARTED && playersFinishedGame()) {
                    finishGame();
                }

                for (PlayerHandler handler : handlers) {
                    synchronized (handler) {
                        handler.notify();
                    }
                }
            }
        }

        private void finishGame() {
            gameState = GameState.STOPPED;
            playersExecutor.shutdown();
            gameExecutor.shutdown();
        }

        private boolean playersFinishedGame() {
            boolean playersFinishedGame = true;
            for (PlayerData playerData : playerDataList) {
                if (playerData.connected() && playerData.getInputCharsCount() < text.length()) {
                    playersFinishedGame = false;
                    break;
                }
            }
            return playersFinishedGame;
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
                    while (gameState != GameState.STOPPED) {
                        GameState currentState = gameState;
                        while (currentState == gameState) {
                            updateGame(reader, writer);
                            this.wait();
                        }
                        writer.writeObject(stateToCommand(gameState));
                        writer.flush();
                    }
                }

                System.out.println(this + " EXIT");

            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                System.out.println(this + " DISCONNECT, message: " + e);
                synchronized (GameSession.this) {
                    playerDataList.get(playerNum).setConnected(false);
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
            int finishTime = reader.readInt();
            synchronized (GameSession.this) {
                playerDataList.get(playerNum).setInputCharsCount(inputCharsCount);
                playerDataList.get(playerNum).setErrorsCount(errorsCount);
                playerDataList.get(playerNum).setFinishTime(finishTime);
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
