package org.Assign;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import io.opentelemetry.api.trace.Span;

public class Client {
    private String hostname; // server hostname
    private int port; // server port
    private String fileName; // file to send
    private SecretKey secretKey; // AES key

    public Client(String hostname, int port, String fileName) throws NoSuchAlgorithmException { // constructor
        this.hostname = hostname;
        this.port = port;
        this.fileName = fileName;
    }
    static void setupOpenTelemetry() { // setup OpenTelemetry
        JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
                .setEndpoint("http://localhost:14250") // Jaeger
                .build();

        Sampler sampler = Sampler.alwaysOn();
//        Sampler sampler = Sampler.traceIdRatioBased(0.4);

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(jaegerExporter))
                .setSampler(sampler)
                .build();

        OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();

        System.out.println("OpenTelemetry initialized");
    }


    private byte[] encryptData(byte[] data,SecretKey Key) throws Exception { // encrypt data
        Tracer tracer = GlobalOpenTelemetry.getTracer("ClientTracer");
        Span encryptDataSpan = tracer.spanBuilder("encryptData").startSpan();
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, Key);
            return cipher.doFinal(data);
        } catch (Exception e) {
            encryptDataSpan.recordException(e);
            encryptDataSpan.addEvent("encryptData exception");
            System.out.println("Error encrypting data: " + e.getMessage());
            throw e;
        } finally {
            encryptDataSpan.end();
        }
    }

    public void sendFile() throws Exception { // send file to server

        try (Socket socket = new Socket(hostname, port)) {
            System.out.println("Connected to the server");

            SecretKey secretKey = receiveKey(socket);
            File file = new File(fileName);
            Tracer tracer = GlobalOpenTelemetry.getTracer("ClientTracer");
            Span sendFileSpan = tracer.spanBuilder("sendFile").startSpan();
            try (FileInputStream fis = new FileInputStream(file);
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                dos.writeUTF(file.getName()); // send file name

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) > 0) {
                    byte[] encryptedData = encryptData(Arrays.copyOf(buffer, bytesRead), secretKey);
                    dos.writeInt(encryptedData.length); // send length of encrypted data
                    dos.write(encryptedData); // send encrypted data
                }
                dos.writeInt(0); // send 0 to indicate end of file
            } catch (IOException e) {
                sendFileSpan.recordException(e);
                sendFileSpan.addEvent("sendFile exception");
                System.out.println("Error sending file: " + e.getMessage());
                e.printStackTrace();
            } finally {
                sendFileSpan.end();
            }
        }
        System.out.println("File sent");
    }

    private SecretKey receiveKey(Socket socket) throws IOException { // receive AES key from server
        Tracer tracer = GlobalOpenTelemetry.getTracer("ClientTracer");
        Span receiveKeySpan = tracer.spanBuilder("receiveKey").startSpan();
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream()); // create input stream
            int keyLength = dis.readInt(); // read key length
            byte[] keyBytes = new byte[keyLength]; // create byte array to store key
            dis.readFully(keyBytes); // read key into byte array
            System.out.println("Received key from server");
            return new SecretKeySpec(keyBytes, "AES"); // return AES key
        } catch (IOException e) {
            receiveKeySpan.recordException(e);
            receiveKeySpan.addEvent("receiveKey exception");
            System.out.println("Error receiving key: " + e.getMessage());
            throw e;
        } finally {
            receiveKeySpan.end();
        }
    }

    public static void main(String[] args) {
        setupOpenTelemetry();
        String hostname = "127.0.0.1";
        int port = 8902;
        String fileName = "src/main/java/org/Assign/client_folder/file_1.txt";

        try {
            Client client = new Client(hostname, port, fileName);
            client.sendFile();
            //client close
        } catch (Exception ex) {
            System.out.println("Client exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
