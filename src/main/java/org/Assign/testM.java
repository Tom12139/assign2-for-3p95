package org.Assign;

//Cosc 3p95
//Assignment 2
//Shijie Tong
//7081201

public class testM { // test multiple clients
    public static void main(String[] args) throws Exception {
        Client.setupOpenTelemetry();
        String hostname = "127.0.0.1";
        Client client1 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_1.txt");
        client1.sendFile();
        Client client2 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_2.txt");
        client2.sendFile();
        Client client3 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_3.txt");
        client3.sendFile();
        Client client4 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_4.txt");
        client4.sendFile();
        Client client5 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_5.txt");
        client5.sendFile();
        Client client6 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_6.txt");
        client6.sendFile();
        Client client7 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_7.txt");
        client7.sendFile();
        Client client8 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_8.txt");
        client8.sendFile();
        Client client9 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_9.txt");
        client9.sendFile();
        Client client10 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_10.txt");
        client10.sendFile();
        Client client11 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_11.txt");
        client11.sendFile();
        Client client12 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_12.txt");
        client12.sendFile();
        Client client13 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_13.txt");
        client13.sendFile();
        Client client14 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_14.txt");
        client14.sendFile();
        Client client15 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_15.txt");
        client15.sendFile();
        Client client16 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_16.txt");
        client16.sendFile();
        Client client17 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_17.txt");
        client17.sendFile();
        Client client18 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_18.txt");
        client18.sendFile();
        Client client19 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_19.txt");
        client19.sendFile();
        Client client20 = new Client(hostname, 8902, "src/main/java/org/Assign/client_folder/file_20.txt");
        client20.sendFile();
    }
}
