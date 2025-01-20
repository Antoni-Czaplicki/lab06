package com.czaplicki.lab06.service;

import com.czaplicki.lab06.ProtocolConstants;
import com.czaplicki.lab06.SocketUtils;
import com.czaplicki.lab06.interfaces.IHouse;

import java.io.IOException;

/**
 * Encapsulates the "House" business logic.
 */
public class HouseService implements IHouse {

    private final String localHost;
    private final int port;
    private final int capacity;
    private int currentVolume;

    private String officeHost;
    private int officePort;

    public HouseService(int port, int capacity, int currentVolume) {
        this.localHost = SocketUtils.getLocalHost();
        this.port = port;
        this.capacity = capacity;
        this.currentVolume = currentVolume;
    }

    public String getHost() {
        return localHost;
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

    public void setOfficeConnection(String officeHost, int officePort) {
        this.officeHost = officeHost;
        this.officePort = officePort;
    }

    public String getOfficeHost() {
        return officeHost;
    }

    public int getOfficePort() {
        return officePort;
    }

    @Override
    public int getPumpOut(int max) {
        // Called by Tanker. Pump out min(currentVolume, max).
        int pumpedOut = Math.min(currentVolume, max);
        currentVolume -= pumpedOut;
        return pumpedOut;
    }

    public boolean isFull() {
        return currentVolume >= capacity;
    }

    public void produceWaste(int amount) {
        currentVolume = Math.min(currentVolume + amount, capacity);
    }

    public void orderService() {
        if (officeHost == null || officePort <= 0) {
            System.err.println("Office connection not set.");
            return;
        }
        // Send "o:houseHost,housePort" request to Office
        String request = ProtocolConstants.REQUEST_ORDER + localHost + "," + port;
        try {
            String response = SocketUtils.sendRequest(officeHost, officePort, request);
            if ("1".equals(response)) {
                System.out.println("Order accepted by Office.");
            } else {
                System.out.println("Order rejected or no available tankers.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}