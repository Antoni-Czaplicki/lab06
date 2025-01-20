package com.czaplicki.lab06.server;

import com.czaplicki.lab06.ProtocolConstants;
import com.czaplicki.lab06.service.HouseService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Listens for TCP requests sent to the House (e.g. "gp:xxx").
 */
public class HouseServer implements Runnable {

    private final HouseService houseService;
    private final Consumer<String> logConsumer;
    private ServerSocket serverSocket;
    private Thread serverThread;

    public HouseServer(HouseService houseService, Consumer<String> logConsumer) {
        this.houseService = houseService;
        this.logConsumer = logConsumer;
    }

    public void start() {
        serverThread = new Thread(this, "HouseServerThread");
        serverThread.start();
    }

    public void stop() {
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                log("Error closing HouseServer: " + e.getMessage());
            }
            log("HouseServer stopped.");
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(houseService.getPort());
            log("HouseServer listening on port: " + houseService.getPort());

            while (!Thread.currentThread().isInterrupted()) {
                try (Socket client = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                     PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

                    String request = in.readLine();
                    log("Received request: " + request);

                    String response = handleRequest(request);
                    out.println(response);

                } catch (IOException e) {
                    if (serverSocket.isClosed()) {
                        log("HouseServer socket closed.");
                        break;
                    }
                    log("Error handling client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            log("HouseServer error: " + e.getMessage());
        }
    }

    private String handleRequest(String request) {
        if (request == null) return "0";

        // e.g. "gp:50"
        if (request.startsWith(ProtocolConstants.REQUEST_GET_PUMP_OUT)) {
            String data = request.substring(ProtocolConstants.REQUEST_GET_PUMP_OUT.length());
            int max = Integer.parseInt(data);
            int pumped = houseService.getPumpOut(max);
            log("Pumped out: " + pumped + " units.");
            return String.valueOf(pumped);
        }

        log("Unknown request: " + request);
        return "0";
    }

    private void log(String msg) {
        if (logConsumer != null) {
            logConsumer.accept(msg);
        } else {
            System.out.println(msg);
        }
    }
}