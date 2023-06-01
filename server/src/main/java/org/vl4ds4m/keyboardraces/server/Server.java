package org.vl4ds4m.keyboardraces.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class Server {
    public static void main(String[] args) {
        int port = 8888;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Game server has been launched.");
            System.out.println("Address: " + serverSocket.getInetAddress().getHostAddress());
            System.out.println("Port: " + port);

            // This thread creates new gameSessions
            Thread playerReceiver = new Thread(() -> {
                try {
                    GameSession gameSession = null;
                    int gameSessionNum = 0;
                    int playersNum = 0;
                    while (!Thread.currentThread().isInterrupted()) {
                        Socket playerSocket = serverSocket.accept();

                        if (gameSession == null || gameSession.getPlayersNum() == 3) {
                            gameSession = new GameSession();
                            ++gameSessionNum;
                        }
                        gameSession.addPlayer(playerSocket);
                        ++playersNum;
                        System.out.println("New player: " + playersNum + ", gameSession: " + gameSessionNum);
                    }
                } catch (SocketException ignored) {
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            playerReceiver.start();

            Scanner scanner = new Scanner(System.in);
            String response;
            System.out.println("Enter 'exit' to close the server.");
            do {
                response = scanner.nextLine();
            } while (!response.equals("exit"));

            playerReceiver.interrupt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println("Game server has been turned off.");
        }
    }
}
