package org.Assign;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;

public class Server {
    private int port;

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                ForkJoinPool.commonPool().execute(new serverThread(socket));
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static class serverThread extends RecursiveAction {
        private Socket socket;

        public serverThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        protected void compute() {
            try {
                saveFile(socket);
            } catch (IOException e) {
                System.out.println("Error saving file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void saveFile(Socket socket) throws IOException {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            FileOutputStream fos = new FileOutputStream("received_file");
            byte[] buffer = new byte[4096];

            int filesize = 15000; // Assuming a file size, in actual scenario this should be dynamic
            int read;
            int totalRead = 0;
            int remaining = filesize;

            while ((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
                totalRead += read;
                remaining -= read;
                System.out.println("read " + totalRead + " bytes.");
                fos.write(buffer, 0, read);
            }

            fos.close();
            dis.close();
        }
    }

    public static void main(String[] args) {
        // OpenTelemetry
        JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
                .setEndpoint("http://localhost:14250").build()
                .build();

        OpenTelemetrySdk.builder()
                .setTracerProvider(
                        SdkTracerProvider.builder()
                                .addSpanProcessor(SimpleSpanProcessor.create(jaegerExporter))
                                .build())
                .buildAndRegisterGlobal();

        Tracer tracer = GlobalOpenTelemetry.getTracer("my-tracer");


        int port = 8902;
        Server server = new Server(port);
        server.start();
    }
}
