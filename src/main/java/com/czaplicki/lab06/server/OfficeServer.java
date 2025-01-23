package com.czaplicki.lab06.server;

import com.czaplicki.lab06.ProtocolConstants;
import com.czaplicki.lab06.service.OfficeService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

import static java.lang.System.out;

public class OfficeServer implements Runnable {

    private final OfficeService officeService;
    private final Consumer<String> logConsumer;
    private ServerSocket serverSocket;
    private Thread serverThread;

    public OfficeServer(OfficeService officeService, Consumer<String> logConsumer) {
        this.officeService = officeService;
        this.logConsumer = logConsumer;
    }

    public void start() {
        serverThread = new Thread(this, "OfficeServerThread");
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
                log("Error closing OfficeServer: " + e.getMessage());
            }
            log("OfficeServer stopped.");
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(officeService.getPort());
            log("OfficeServer listening on port: " + officeService.getPort());

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
                        log("OfficeServer socket closed.");
                        break;
                    }
                    log("Error handling client: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            log("OfficeServer error: " + e.getMessage());
        }
    }

    private String handleRequest(String request) {
        if (request == null) return "0";

        // r:host,port,capacity => register tanker
        if (request.startsWith(ProtocolConstants.REQUEST_REGISTER)) {
            String data = request.substring(ProtocolConstants.REQUEST_REGISTER.length());
            String[] parts = data.split(",");
            if (parts.length == 3) {
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                int capacity = Integer.parseInt(parts[2]);

                int tankerId = officeService.registerTanker(host, port, capacity);

                log("Registered tanker # " + tankerId + " at " + host + ":" + port);
                return String.valueOf(tankerId);
            }
        }
        // o:houseHost,housePort => order service
        else if (request.startsWith(ProtocolConstants.REQUEST_ORDER)) {
            String data = request.substring(ProtocolConstants.REQUEST_ORDER.length());
            String[] parts = data.split(",");
            if (parts.length == 2) {
                String houseHost = parts[0];
                int housePort = Integer.parseInt(parts[1]);
                int result = officeService.orderService(houseHost, housePort);
                return String.valueOf(result);
            }
        }
        // sr:tankerId => set tanker ready
        else if (request.startsWith(ProtocolConstants.REQUEST_SET_READY)) {
            String data = request.substring(ProtocolConstants.REQUEST_SET_READY.length());
            int tankerId = Integer.parseInt(data);
            officeService.setReadyToServe(tankerId);
            log("Tanker # " + tankerId + " set to ready.");
            return "1";
        }

        log("Unknown request: " + request);
        return "0";
    }

    private void log(String msg) {
        if (logConsumer != null) {
            logConsumer.accept(msg);
        } else {
            out.println(msg);
        }
    }
}