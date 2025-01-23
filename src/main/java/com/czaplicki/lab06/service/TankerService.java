package com.czaplicki.lab06.service;

import com.czaplicki.lab06.ProtocolConstants;
import com.czaplicki.lab06.interfaces.ITanker;
import com.czaplicki.lab06.SocketUtils;

import java.io.IOException;

public class TankerService implements ITanker {
    private final int port;
    private final int maxCapacity;
    private int currentVolume;
    private int tankerId;
    private String officeHost;
    private int officePort;
    private String sewageHost;
    private int sewagePort;

    public TankerService(int port, int maxCapacity) {
        this.port = port;
        this.maxCapacity = maxCapacity;
        this.currentVolume = 0;
    }

    public int getPort() {
        return port;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public int getCurrentVolume() {
        return currentVolume;
    }

    public int getTankerId() {
        return tankerId;
    }

    public void setTankerId(int tankerId) {
        this.tankerId = tankerId;
    }

    public void setOfficeConnection(String officeHost, int officePort) {
        this.officeHost = officeHost;
        this.officePort = officePort;
    }

    public void setSewageConnection(String sewageHost, int sewagePort) {
        this.sewageHost = sewageHost;
        this.sewagePort = sewagePort;
    }

    /**
     * Register in the Office.
     */
    public void registerInOffice() throws IOException {
        // For local host, you can also do: SocketUtils.getLocalHost()
        String request = ProtocolConstants.REQUEST_REGISTER
                + "127.0.0.1" + "," + port + "," + maxCapacity;
        String response = SocketUtils.sendRequest(officeHost, officePort, request);
        // response is the tankerId
        tankerId = Integer.parseInt(response);
        System.out.println("Registered in office with id: " + tankerId);
    }

    /**
     * Notify Office that the tanker is ready.
     */
    public void setReady() throws IOException {
        String request = ProtocolConstants.REQUEST_SET_READY + tankerId;
        SocketUtils.sendRequest(officeHost, officePort, request);
    }

    /**
     * Gets called when Office instructs this tanker to handle a house.
     */
    @Override
    public void setJob(String houseHost, int housePort) {
        try {
            // Pump out from house
            String request = ProtocolConstants.REQUEST_GET_PUMP_OUT + maxCapacity;
            String response = SocketUtils.sendRequest(houseHost, housePort, request);
            int pumpedOut = Integer.parseInt(response);
            currentVolume = pumpedOut;

            // Deliver to sewage
            deliverToSewagePlant();
            // Then set ready again
            setReady();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deliver the sewage to the sewage plant.
     */
    public void deliverToSewagePlant() throws IOException {
        String request = ProtocolConstants.REQUEST_SEWAGE_PUMP_IN
                + tankerId + "," + currentVolume;
        SocketUtils.sendRequest(sewageHost, sewagePort, request);
        currentVolume = 0;
    }
}