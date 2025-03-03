package org.example;

import java.io.*;
import java.net.Socket;

public class Client {

    //Variables to store connection information and streams;
    private Socket socket;
    private final String serverAddress;
    private final int serverPort;

    private BufferedReader userInputReader;

    private BufferedWriter socketOutputWriter;
    private BufferedReader socketInputReader;

    //Constructor to initialize client with server address and port;
    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    //Method to set up input and output streams to communicate with the server;
    private void setupSocketStreams() throws IOException {

        //Initialize user input reader from console;
        userInputReader = new BufferedReader(new InputStreamReader(System.in));

        //Initialize output stream writer to send messages to the server;
        socketOutputWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        //Initialize input stream reader to receive messages from the server;
        socketInputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    //Method to start client connection to the chat server;
    public void start() {

        //Inform user about connection attempt;
        System.out.println("Connecting...please wait.");

        try {
            //Create a socket connection to the server;
            socket = new Socket(serverAddress, serverPort);

            //Inform about successful connection;
            System.out.println("Connected to server: " + socket.getRemoteSocketAddress());

            //Setup input and output streams;
            setupSocketStreams();
        } catch (Exception e) {
            //Handle connection error and exit;
            System.err.println("Failed to connect to the server: " + e.getMessage());
            return;
        }

        //Start a thread to handle incoming messages from the server;
        Thread messageReceiver = new Thread(new Chat(socketInputReader));
        messageReceiver.start();

        //Handle user input and sent messages from the server;
        try {
            String userInput;
            while (true) {

                // Read user input from the console;
                userInput = userInputReader.readLine();

                //Check /quit command;
                if (userInput == null || userInput.trim().equalsIgnoreCase("/quit")) {
                    // Inform the user and exit;
                    System.out.println("Leaving the chat room...");
                    break;
                }
                //Send the user input to the server;
                socketOutputWriter.write(userInput);
                socketOutputWriter.newLine();
                socketOutputWriter.flush();
            }
        } catch (Exception e) {
            //Handle error during communication;
            System.err.println("Error during chat communication: " + e.getMessage());
        } finally {
            //Close all resources to ensure proper cleanup;
            closeResources();
        }
    }

    //Method to close all open resources of sockets and streams;
    private void closeResources() {
        try {
            if (socket != null && !socket.isClosed()) {
                //Close socket connection;
                socket.close();
            }
            if (userInputReader != null) {
                //Close input reader;
                userInputReader.close();
            }
            if (socketOutputWriter != null) {
                //Close socket output stream writer;
                socketOutputWriter.close();
            }
            if (socketInputReader != null) {
                //Close socket input stream reader;
                socketInputReader.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

    //Main method to run client;
    public static void main(String[] args) {
        try {
            //Create a new client instance with server address and port;
            Client chatClient = new Client("localhost", 8888);

            //Start client and chat connection;
            chatClient.start();
        } catch (Exception e) {
            System.err.println("Error starting client connection: " + e.getMessage());
        }
    }
}

//Inner class to handle incoming messages from the server in a separate thread;
class Chat implements Runnable {

    private final BufferedReader serverInputReader;

    //Constructor to initialize the chat handler with the server input stream reader;
    public Chat(BufferedReader serverInputReader) {
        this.serverInputReader = serverInputReader;
    }

    //Method to continuously receive messages from the server and display them in the console;
    @Override
    public void run() {
        try {
            String serverMessage;
            while ((serverMessage = serverInputReader.readLine())!= null) {
                //Print received message from the server in the console;
                System.out.println(serverMessage);
            }
        } catch (IOException e) {
            //Handle error during message reading;
            System.err.println("Error reading messages from the server: " + e.getMessage());
        }
    }
}
