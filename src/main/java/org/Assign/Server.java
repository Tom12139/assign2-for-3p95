package org.Assign;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

import static java.lang.Thread.sleep;

public class Server {
    private int port; // server port
    private SecretKey secretKey; // AES key
    public Server(int port) throws NoSuchAlgorithmException {
        this.port = port;
        this.secretKey = generateKey(); // generate AES key
    }
    private static SecretKey generateKey() throws NoSuchAlgorithmException { // generate AES key
        Tracer tracer = GlobalOpenTelemetry.getTracer("ServerTracer");
        Span generateKeySpan = tracer.spanBuilder("generateKey").startSpan();
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128); // 128 bits AES key
            System.out.println("Generated key: " + keyGen.toString() + "\n");
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            generateKeySpan.recordException(e);
            generateKeySpan.addEvent("generateKey exception");
            System.out.println("Error generating key: " + e.getMessage());
            throw e;
        } finally {
            generateKeySpan.end();
        }
    }

    public void start() { // start server
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept(); // accept new client
                System.out.println("New client connected");

                ForkJoinPool.commonPool().execute(new serverThread(socket,secretKey)); // create a new thread to handle the client
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static class serverThread extends RecursiveAction { // thread to handle client
        private Socket socket; // client socket
        private SecretKey secretKey; // AES key

        public serverThread(Socket socket,SecretKey key) {
            this.socket = socket;
            this.secretKey = key;
        }


        private byte[] decryptData(byte[] data,SecretKey key) throws Exception { // decrypt data
            Tracer tracer = GlobalOpenTelemetry.getTracer("ServerTracer");
            Span decryptSpan = tracer.spanBuilder("decryptData").startSpan();
            try {
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, key);
                return cipher.doFinal(data);
            } catch (Exception e) {
                decryptSpan.recordException(e);
                decryptSpan.addEvent("decryptData exception");
                System.out.println("Error decrypting data: " + e.getMessage());
                throw e;
            } finally {
                decryptSpan.end();
            }
        }

        @Override
        protected void compute() { // handle client
            Tracer tracer = GlobalOpenTelemetry.getTracer("ServerTracer");
            Span serverSpan = tracer.spanBuilder("serverThread").startSpan();

            try {
                //send key to client
                sendKey(socket);
                saveFile(socket);
            } catch (IOException e) {
                System.out.println("Error saving file: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                serverSpan.recordException(e);
                serverSpan.addEvent("serverThread exception");
                System.out.println("Error saving file: " + e.getMessage());
            } finally {
                serverSpan.end();
            }
        }
        private void sendKey(Socket socket) throws Exception { // send AES key to client
            Tracer tracer = GlobalOpenTelemetry.getTracer("ServerTracer");
            Span sendKeySpan = tracer.spanBuilder("sendKey").startSpan();
            try {
                byte[] keyBytes = secretKey.getEncoded(); // get key bytes
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); // create output stream
                dos.writeInt(keyBytes.length); // send key length
                dos.write(keyBytes);
            } catch (Exception e) {
                sendKeySpan.recordException(e);
                sendKeySpan.addEvent("sendKey exception");
                System.out.println("Error sending key: " + e.getMessage());
                throw e;
            } finally {
                sendKeySpan.end();
            }
        }
        private void deliberateDelay() throws InterruptedException {
            sleep(1000);
        }
        private void saveFile(Socket socket) throws Exception { // save file from client

            DataInputStream dis = new DataInputStream(socket.getInputStream());

            String fileName = dis.readUTF(); // read file name

            ByteArrayOutputStream decryptedOutputStream = new ByteArrayOutputStream();

            // create a span
            Tracer tracer = GlobalOpenTelemetry.getTracer("ServerTracer");
            Span fileSaveSpan = tracer.spanBuilder("saveFile").startSpan();
//
//            if(new Random().nextDouble() < 0.3){
//                deliberateDelay();
//            }

            try (Scope scope = fileSaveSpan.makeCurrent()) {
                while (true) {
                    int length;
                    try {
                        length = dis.readInt(); // read length of incoming message
                    } catch (EOFException e) {
                        break; // if EOF, end loop
                    }

                    if (length <= 0) break; // if length is negative or zero, end loop

                    byte[] encryptedData = new byte[length];
                    dis.readFully(encryptedData); // read encrypted data

                    byte[] decryptedData = decryptData(encryptedData,secretKey); // decrypt data
                    decryptedOutputStream.write(decryptedData); // write to output stream

                    // record event
                    fileSaveSpan.addEvent("Read chunk", Attributes.of(AttributeKey.longKey("bytesRead"), (long) length));
                    System.out.println("Read chunk");
                }
            } catch (IOException e) {
                fileSaveSpan.recordException(e);
                System.out.println("Error saving file: " + e.getMessage());
                e.printStackTrace();
            } finally {
                fileSaveSpan.end(); // end span

                // let's save the file
                try (FileOutputStream fos = new FileOutputStream( fileName)) {
                    decryptedOutputStream.writeTo(fos);
                }
                decryptedOutputStream.close();
                dis.close();
            }
        }

    }



    private static void setupOpenTelemetry() { // setup OpenTelemetry
        JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
                .setEndpoint("http://localhost:14250") // Jaeger
                .build();

        Sampler sampler = Sampler.alwaysOn();   // always sample
//        Sampler sampler = Sampler.traceIdRatioBased(0.4); // sample 40% of traces

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(jaegerExporter))
                .setSampler(sampler)
                .build();

        OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();

        System.out.println("OpenTelemetry initialized");
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        setupOpenTelemetry();


        int port = 8902;
        Server server = new Server(port);
        server.start();
    }

}
