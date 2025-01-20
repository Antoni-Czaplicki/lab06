package com.czaplicki.lab06.server;

import com.czaplicki.lab06.ProtocolConstants;
import com.czaplicki.lab06.service.TankerService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public class TankerServer implements Runnable {

    private final TankerService tankerService;
    private final Consumer<String> logConsumer; // A callback to log messages (e.g. from UI)
    private ServerSocket serverSocket;
    private Thread serverThread;

    public TankerServer(TankerService tankerService, Consumer<String> logConsumer) {
        this.tankerService = tankerService;
        this.logConsumer = logConsumer;
    }

    public void start() {
        serverThread = new Thread(this, "TankerServerThread");
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
                log("Error closing server: " + e.getMessage());
            }
            log("Tanker server stopped.");
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(tankerService.getPort());
            log("Tanker server listening on port: " + tankerService.getPort());

            while (!Thread.currentThread().isInterrupted()) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(
                             new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true))
                {
                    String request = in.readLine();
                    log("Received request: " + request);
                    String response = handleRequest(request);
                    out.println(response);
                } catch (IOException e) {
                    if (serverSocket.isClosed()) {
                        log("Server socket closed.");
                        break;
                    }
                    log("Client handling error: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            log("Tanker server error: " + e.getMessage());
        }
    }

    private String handleRequest(String request) {
        if (request == null) return "0";

        // e.g. "sj:houseHost,housePort"
        if (request.startsWith(ProtocolConstants.REQUEST_SET_JOB)) {
            String data = request.substring(ProtocolConstants.REQUEST_SET_JOB.length());
            String[] parts = data.split(",");
            if (parts.length == 2) {
                String houseHost = parts[0];
                int housePort = Integer.parseInt(parts[1]);
                tankerService.setJob(houseHost, housePort);
                return "1";
            }
        }
        // e.g. "rc:tankerId,sewageHost,sewagePort"
        else if (request.startsWith(ProtocolConstants.REQUEST_REGISTER_CONFIRM)) {
            String data = request.substring(ProtocolConstants.REQUEST_REGISTER_CONFIRM.length());
            String[] parts = data.split(",");
            if (parts.length == 3) {
                int tId = Integer.parseInt(parts[0]);
                String sewageHost = parts[1];
                int sewagePort = Integer.parseInt(parts[2]);
                tankerService.setTankerId(tId);
                tankerService.setSewageConnection(sewageHost, sewagePort);
                return "1";
            }
        }

        log("Unknown request received: " + request);
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