package org.vl4ds4m.keyboardraces.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class Server {
    private static final int TIMEOUT = 3_000;
    private static final int DEFAULT_PORT = 5619;

    public static void main(String[] args) {
        if (args.length == 1) {
            try {
                int port = Integer.parseInt(args[0]);
                launch(port);
            } catch (NumberFormatException e) {
                System.out.println("Invalid argument.");
            }
        } else if (args.length == 0) {
            launch(DEFAULT_PORT);
        } else {
            System.out.println("Invalid arguments count.");
        }
    }

    private static void launch(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Game server has been launched.");
            System.out.println("Address: " + serverSocket.getInetAddress().getHostAddress());
            System.out.println("Port: " + port);

            Thread playersAcceptor = new Thread(new PlayersAcceptor(serverSocket));
            playersAcceptor.start();

            System.out.println("Enter 'exit' to close the server.");
            Scanner scanner = new Scanner(System.in);

            while (scanner.hasNextLine()) {
                if (scanner.nextLine().equals("exit")) {
                    break;
                }
            }
            playersAcceptor.interrupt();
            System.out.println("Game server has been turned off.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record PlayersAcceptor(ServerSocket serverSocket) implements Runnable {
        private static final Object LOCK = new Object();

        @Override
        public void run() {
            try {
                GameSession gameSession = null;

                while (!Thread.currentThread().isInterrupted()) {
                    Socket playerSocket = serverSocket.accept();

                    synchronized (LOCK) {
                        if (gameSession == null || gameSession.gameLaunched()) {
                            gameSession = new GameSession();
                            new Thread(new TimeoutGameLauncher(gameSession)).start();
                        }

                        gameSession.addPlayer(playerSocket);

                        if (gameSession.playersCount() == 3) {
                            gameSession.launchGame();
                        }
                    }
                }
            } catch (SocketException e) {
                System.out.println("ServerSocket has been closed.");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private record TimeoutGameLauncher(GameSession gameSession) implements Runnable {
            @Override
            public void run() {
                try {
                    Thread.sleep(TIMEOUT);
                    synchronized (LOCK) {
                        if (!gameSession.gameLaunched()) {
                            gameSession.launchGame();
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
