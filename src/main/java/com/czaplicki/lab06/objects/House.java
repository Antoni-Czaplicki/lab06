package com.czaplicki.lab06.objects;

import com.czaplicki.lab06.SocketUtils;

import java.io.IOException;

public class House {
    private String host;
    private int port;
    private int capacity;
    private int currentVolume;
    private String officeHost;
    private int officePort;

    public House(int port, int capacity) {
        this.port = port;
        this.capacity = capacity;
        this.currentVolume = 0; // początkowy stan
        this.host = SocketUtils.getLocalHost();
    }

    public House(int port, int capacity, int currentVolume) {
        this.port = port;
        this.capacity = capacity;
        this.currentVolume = currentVolume;
        this.host = SocketUtils.getLocalHost();
    }

    public House(int port, int capacity, int currentVolume, String officeHost, int officePort) {
        this.port = port;
        this.capacity = capacity;
        this.currentVolume = currentVolume;
        this.host = SocketUtils.getLocalHost();
        this.officeHost = officeHost;
        this.officePort = officePort;
    }

    public int getPumpOut(int max) {
        int pumpedOut = Math.min(currentVolume, max);
        currentVolume -= pumpedOut;
        return pumpedOut;
    }

    public boolean isFull() {
        return currentVolume >= capacity;
    }

    public void orderService(String officeHost, int officePort) {
        String request = "o:" + "127.0.0.1" + "," + port; // Zakładamy lokalny host
        try {
            String response = SocketUtils.sendRequest(officeHost, officePort, request);
            if ("1".equals(response)) {
                System.out.println("Zamówienie przyjęte.");
            } else {
                System.out.println("Zamówienie odrzucone.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void produceWaste(int amount) {
        currentVolume = Math.min(currentVolume + amount, capacity);
    }

    public void setOfficeHost(String officeHost) {
        this.officeHost = officeHost;
    }

    public void setOfficePort(int officePort) {
        this.officePort = officePort;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getCurrentVolume() {
        return currentVolume;
    }

    public String getOfficeHost() {
        return officeHost;
    }

    public int getOfficePort() {
        return officePort;
    }
}