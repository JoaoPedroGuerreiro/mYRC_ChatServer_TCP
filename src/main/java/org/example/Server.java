package org.example;

import java.io.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class Server {
    public static final String RESET = "\033[0m";  // Reset color
    public static final String RED = "\033[31m";    // Red color
    public static final String GREEN = "\033[32m";  // Green color
    public static final String YELLOW = "\033[33m"; // Yellow color
    public static final String BLUE = "\033[34m";   // Blue color
    public static final String MAGENTA = "\033[35m"; // Magenta color
    public static final String CYAN = "\033[36m";    // Cyan color
    public static final String WHITE = "\033[37m";   // White color

    private final int PORT;
    private final List<ServerWorker> workers = new CopyOnWriteArrayList<>();

    //Constructor to initialize the server with a specified Port.
    public Server(int port) {
        this.PORT = port;
    }

    //Method to start the server and accept incoming connections.
    public void startServer() {

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Connecting to port " + PORT);
            System.out.println("Server connected to port " + PORT);

            while (true) {
                System.out.println("Waiting for a client to connect...");
                Socket clientSocket = serverSocket.accept(); //Accept incoming client connections.
                System.out.println("New Client Connected");
                ServerWorker worker = new ServerWorker(clientSocket);
                workers.add(worker);
                new Thread(worker).start(); // Start the worker in a new thread.
            }
        } catch (IOException e) {
            System.err.println("Server error : " + e.getMessage());
        }
    }

    //Method to send a message to all the connected clients except the sender.
    private void sendToAll(String message, ServerWorker sender) {
        for (ServerWorker worker : workers) {
            if (worker.getNickName() != null && worker != sender) {
                worker.send(sender.getNickName() + ": " + sender.colorString + message + RESET);
            }
        }
    }

    //Method to send a private message to a specific client
    private void whisper(String message, ServerWorker sender, String recipient) {
        for (ServerWorker worker : workers) {
            if (worker.getNickName() != null && worker.getNickName().equals(recipient)) {
                worker.send(sender.getNickName() + " [whisper]: " + sender.colorString + message + RESET);
                return;
            }
        }
        sender.send("User '" + recipient + "'offline or not found ");
    }

    //Method to remove a worker from the list when disconnected
    public void removeWorker(ServerWorker worker) {
        workers.remove(worker);
    }

    //Inner class to handle each client connection
    private class ServerWorker implements Runnable {

        private final Socket socket;
        private BufferedReader inputReader;
        private BufferedWriter outputWriter;
        private String nickName;
        private String color = "default";
        private String colorString = RESET;

        //Constructor to initialize the worker with a client socket.
        public ServerWorker(Socket socket) {
            this.socket = socket;
        }

        //Getter for the worker name
        public String getNickName() {
            return nickName;
        }

        //Method to send a message to a client.
        public void send(String message) {

            try {
                outputWriter.write(message);
                outputWriter.newLine();
                outputWriter.flush();

            } catch (IOException e) {
                System.err.println("Error sending message to " + nickName + ": " + e.getMessage());
            }
        }

        //Method to handle the command to change the name of a user.
        private void handleChangeNickname(String[] parts) {
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                send("Invalid Nickname. Use: /newNick <new_nickname> to change the current Nickname");
                return;
            }
            String newNickname = parts[1].trim();
            for (ServerWorker worker : workers) {
                if (worker.getNickName() != null && worker.getNickName().equals(newNickname)) {
                    send("Nickname already in use, choose another.");
                    return;
                }
            }
            this.nickName = newNickname;
            send("Nickname changed to: " + nickName);
        }

        //Method to handle /whisper command.
        private void handleWhisperCommand(String[] parts) {
            if (parts.length < 3) {
                send("How to use: /whisper <nickname> <message>");
                return;
            }
            String recipient = parts[1].trim();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 2; i < parts.length; i++) {
                stringBuilder.append(parts[i]).append(" ");
            }
            String message = stringBuilder.toString().trim();
            whisper(message, this, recipient);
        }

        //Method to handle the color command
        private void handleColorCommand(String[] parts) {
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                send("Invalid color.  Use /color <color_name> ");
                return;
            }
            this.color = parts[1].trim();
            switch (color.toLowerCase()) {
                case "red":
                    colorString = RED;
                    break;
                case "green":
                    colorString = GREEN;
                    break;
                case "blue":
                    colorString = BLUE;
                    break;
                case "yellow":
                    colorString = YELLOW;
                    break;
                case "magenta":
                    colorString = MAGENTA;
                    break;
                case "cyan":
                    colorString = CYAN;
                    break;
                case "white":
                    colorString = WHITE;
                    break;
                default:
                    colorString = RESET;
                    color = "Default";
                    break;
            }
            send("Color changed to: " + color);
        }
    //Method to handle commands.
        private void handleCommand(String command) {

            String[] parts = command.split(" ", 3);
            if (command.startsWith("/name")){
                handleChangeNickname(parts);

            } else if (command.startsWith("/whisper")){
                handleWhisperCommand(parts);
            } else if(command.startsWith("/color")){
                handleColorCommand(parts);
            } else {
                send("Invalid Command" + command);
            }
        }

        //Run method implemented from Runnable to handle client communication.
        @Override
        public void run() {
            try {
                inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                send("Welcome to mYRC! To set your nickname use the command /name <your_nickname> enjoy the chat");
                send("There's also command to set the color of your text using: /color <color_name> .");
                send("If you want to change your nickname just use /name <your_new_nickname> to get a new nickname.");

                String message;
                while((message = inputReader.readLine())!= null){
                    if (message.startsWith("/")){
                        handleCommand(message);
                    } else if (this.nickName != null){
                        sendToAll(message, this);
                    } else {
                        send("You need to set your nickname first using: /name <your_nickname>");
                    }
                }
            } catch (IOException e){
                System.err.println("Client disconnected: " + e.getMessage());
            } finally {
                try {
                    if (inputReader != null) inputReader.close();
                    if (outputWriter != null) outputWriter.close();
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing the client socket: " + e.getMessage());
                }
                removeWorker(this);
                if (this.nickName != null) {
                    sendToAll(this.nickName + "has left the server.", null);
                }
            }
        }
    }
    //Main method to run/start the Server.
    public static void main(String[] args) {
        Server chatServer = new Server(8888);
        chatServer.startServer();
    }
}
