package org.vl4ds4m.keyboardraces.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class Server {
    private static final int TIMEOUT = 1_000;

    public static void main(String[] args) {
        int port = 8888;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Game server has been launched.");
            System.out.println("Address: " + serverSocket.getInetAddress().getHostAddress());
            System.out.println("Port: " + port);

            Thread playerAcceptor = new Thread(() -> {
                try {
                    GameSession gameSession = new GameSession();

                    while (!Thread.currentThread().isInterrupted()) {
                        Socket playerSocket = serverSocket.accept();

                        synchronized (gameSession) {
                            gameSession.addPlayer(playerSocket);
                            if (gameSession.playersCount() == 1) {
                                new Thread(new TimeoutGameLauncher(gameSession)).start();
                            } else if (gameSession.playersCount() == 3) {
                                gameSession.launchGame();
                                gameSession = new GameSession();
                            }
                        }
                    }
                } catch (SocketException e) {
                    System.out.println("ServerSocket has been closed.");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            playerAcceptor.start();

            System.out.println("Enter 'exit' to close the server.");
            Scanner scanner = new Scanner(System.in);

            while (!"exit".equals(scanner.nextLine())) ;

            playerAcceptor.interrupt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println("Game server has been turned off.");
        }
    }

    private record TimeoutGameLauncher(GameSession gameSession) implements Runnable {
        @Override
            public void run() {
                try {
                    Thread.sleep(TIMEOUT);
                    synchronized (gameSession) {
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
