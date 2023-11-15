package org.Assign;

import java.io.*;
import java.net.Socket;

public class Client {
    private String hostname;
    private int port;
    private String fileName;

    public Client(String hostname, int port, String fileName) {
        this.hostname = hostname;
        this.port = port;
        this.fileName = fileName;
    }

    public void sendFile() throws IOException {
        try (Socket socket = new Socket(hostname, port)) {
            System.out.println("Connected to the server");

            File file = new File(fileName);
            FileInputStream fis = new FileInputStream(file);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            byte[] buffer = new byte[4096];

            while (fis.read(buffer) > 0) {
                dos.write(buffer);
            }

            fis.close();
            dos.close();
        }
    }

    public static void main(String[] args) {
        String hostname = "127.0.0.1";
        int port = 8902;
        String fileName = "src/main/java/org/Assign/client_folder/file_1.txt";

        Client client = new Client(hostname, port, fileName);
        try {
            client.sendFile();
        } catch (IOException ex) {
            System.out.println("Client exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
