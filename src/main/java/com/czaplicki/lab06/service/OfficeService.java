package com.czaplicki.lab06.service;

import com.czaplicki.lab06.ProtocolConstants;
import com.czaplicki.lab06.SocketUtils;
import com.czaplicki.lab06.interfaces.IOffice;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OfficeService implements IOffice {

    private final int port;
    private String localHost;

    private String sewageHost;
    private int sewagePort;

    // Keep track of all registered tankers
    private final Map<Integer, TankerData> tankerMap = new HashMap<>();
    private int nextTankerId = 1;

    public OfficeService(int port, String sewageHost, int sewagePort) {
        this.port = port;
        this.sewageHost = sewageHost;
        this.sewagePort = sewagePort;
        this.localHost = SocketUtils.getLocalHost();
    }

    public int getPort() {
        return port;
    }

    public String getSewageHost() {
        return sewageHost;
    }

    public int getSewagePort() {
        return sewagePort;
    }

    @Override
    public synchronized int register(String tankerHost, String tankerPort) {
        int tankerId = nextTankerId++;
        tankerMap.put(tankerId, new TankerData(tankerId, tankerHost, Integer.parseInt(tankerPort)));
        return tankerId;
    }

    @Override
    public synchronized int order(String houseHost, String housePort) {
        for (TankerData tanker : tankerMap.values()) {
            if (tanker.ready) {
                assignJobToTanker(tanker, houseHost, housePort);
                return 1;  // job assigned
            }
        }
        return 0; // no available tanker
    }

    @Override
    public synchronized void setReadyToServe(int tankerId) {
        TankerData tanker = tankerMap.get(tankerId);
        if (tanker != null) {
            tanker.ready = true;
        }
    }

    public Map<Integer, TankerData> getTankerMap() {
        return tankerMap;
    }

    /**
     * Assign a job to a specific tanker (telling the tanker to handle that house).
     */
    private void assignJobToTanker(TankerData tanker, String houseHost, String housePort) {
        try {
            String request = ProtocolConstants.REQUEST_SET_JOB + houseHost + "," + housePort;
            SocketUtils.sendRequest(tanker.host, tanker.port, request, false);
            // Mark the tanker as not ready
            tanker.ready = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get all tankers volume in the sewage plant.
     */
    public String getAllTankersInSewagePlant() {
        StringBuilder sb = new StringBuilder();
        try {
            for (TankerData tanker : tankerMap.values()) {
                String request = ProtocolConstants.REQUEST_GET_STATUS + tanker.id;
                String response = SocketUtils.sendRequest(sewageHost, sewagePort, request);
                sb.append("ID: ").append(tanker.id).append(" Volume: ").append(response).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * Payoff all tankers.
     */
    public void payoffAll() {
        for (TankerData tanker : tankerMap.values()) {
            try {
                String request = ProtocolConstants.REQUEST_SEWAGE_PAYOFF + tanker.id;
                SocketUtils.sendRequest(sewageHost, sewagePort, request, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper class to store tanker data in Office.
     */
    public static class TankerData {
        public final int id;
        public final String host;
        public final int port;
        public boolean ready;

        public TankerData(int id, String host, int port) {
            this.id = id;
            this.host = host;
            this.port = port;
            this.ready = false;
        }
    }
}