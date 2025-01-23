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
    public synchronized int register(String tankerHost, String tankerPortString) {
        // For backward compatibility, but in practice we might parse capacity or more parameters.
        // Here we’ll assume we can parse: register(tankerHost, port, capacity).
        return -1; // not used in the new approach, left for interface compliance
    }

    public synchronized int registerTanker(String host, int tankerPort, int capacity) {
        int tankerId = nextTankerId++;
        tankerMap.put(tankerId, new TankerData(tankerId, host, tankerPort, capacity));
        return tankerId;
    }

    @Override
    public synchronized int order(String houseHost, String housePortString) {
        // Not used in new approach, left for interface compliance
        return -1;
    }

    public synchronized int orderService(String houseHost, int housePort) {
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
    private void assignJobToTanker(TankerData tanker, String houseHost, int housePort) {
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
     * Helper class to store tanker data in Office.
     */
    public static class TankerData {
        public final int id;
        public final String host;
        public final int port;
        public final int capacity;
        public boolean ready;

        public TankerData(int id, String host, int port, int capacity) {
            this.id = id;
            this.host = host;
            this.port = port;
            this.capacity = capacity;
            this.ready = false;
        }
    }
}