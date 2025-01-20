package com.czaplicki.lab06.server;

import com.czaplicki.lab06.ProtocolConstants;
import com.czaplicki.lab06.service.SewagePlantService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public class SewagePlantServer implements Runnable {

    private final SewagePlantService sewageService;
    private final Consumer<String> logConsumer;
    private ServerSocket serverSocket;
    private Thread serverThread;

    public SewagePlantServer(SewagePlantService sewageService, Consumer<String> logConsumer) {
        this.sewageService = sewageService;
        this.logConsumer = logConsumer;
    }

    public void start() {
        serverThread = new Thread(this, "SewagePlantServerThread");
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
                log("Error closing SewagePlantServer: " + e.getMessage());
            }
            log("SewagePlantServer stopped.");
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(sewageService.getPort());
            log("SewagePlantServer listening on port: " + sewageService.getPort());

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
                        log("SewagePlantServer socket closed.");
                        break;
                    }
                    log("Error handling client: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            log("SewagePlantServer error: " + e.getMessage());
        }
    }

    private String handleRequest(String request) {
        if (request == null) return "0";

        // spi:tankerId,volume
        if (request.startsWith(ProtocolConstants.REQUEST_SEWAGE_PUMP_IN)) {
            String data = request.substring(ProtocolConstants.REQUEST_SEWAGE_PUMP_IN.length());
            String[] parts = data.split(",");
            if (parts.length == 2) {
                int tankerId = Integer.parseInt(parts[0]);
                int volume = Integer.parseInt(parts[1]);
                sewageService.setPumpIn(tankerId, volume);
                log("Pumped in from tanker #" + tankerId + ": " + volume + " units.");
                return "1";
            }
        }
        // gs:tankerId => get status
        else if (request.startsWith("gs:")) {
            String data = request.substring(3);
            int tankerId = Integer.parseInt(data);
            int status = sewageService.getStatus(tankerId);
            return String.valueOf(status);
        }
        // spo:tankerId => set payoff
        else if (request.startsWith("spo:")) {
            String data = request.substring(4);
            int tankerId = Integer.parseInt(data);
            sewageService.setPayoff(tankerId);
            log("Payoff completed for tanker #" + tankerId);
            return "1";
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