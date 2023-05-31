package org.vl4ds4m.keyboardraces.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;

public class Server {
    private static final String TEXTS_DIR = "/Texts/";
    private static final List<String> TEXTS = List.of(/*"email-text.txt", "hello.txt",*/ "example.txt");
    private static boolean serverAlive = true;

    public static void main(String[] args) {
        int port = 8888;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Game server has been launched.");
            System.out.println("Address: " + serverSocket.getInetAddress().getHostAddress());
            System.out.println("Port: " + port);

            Thread gameSessionHandler = new Thread(() -> handleGameSession(serverSocket));
            gameSessionHandler.start();

            Scanner scanner = new Scanner(System.in);
            String response;
            System.out.println("Enter 'exit' to close the server.");
            do {
                response = scanner.nextLine();
            } while (!response.equals("exit"));

            serverAlive = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println("Game server has been turned off.");
        }
    }

    private static void handleGameSession(ServerSocket serverSocket) {
        while (serverAlive) {
            try (Socket socket = serverSocket.accept();
                 ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {

                int textNumber = (new Random()).nextInt(TEXTS.size());
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                        Server.class.getResourceAsStream(TEXTS_DIR + TEXTS.get(textNumber)))))) {

                    String text = reader.readLine();
                    outputStream.writeObject(text);
                    outputStream.flush();
                }
            } catch (SocketException e) {
                break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
