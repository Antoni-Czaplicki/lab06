package com.czaplicki.lab06.service;

import com.czaplicki.lab06.SocketUtils;
import com.czaplicki.lab06.interfaces.ISewagePlant;

import java.util.HashMap;
import java.util.Map;

public class SewagePlantService implements ISewagePlant {
    private final int port;
    private final String localHost;
    private final Map<Integer, Integer> tankerVolumes = new HashMap<>();

    public SewagePlantService(int port) {
        this.port = port;
        this.localHost = SocketUtils.getLocalHost();
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return localHost;
    }

    @Override
    public synchronized void setPumpIn(int number, int volume) {
        tankerVolumes.put(number, tankerVolumes.getOrDefault(number, 0) + volume);
    }

    @Override
    public synchronized int getStatus(int number) {
        return tankerVolumes.getOrDefault(number, 0);
    }

    @Override
    public synchronized void setPayoff(int number) {
        tankerVolumes.put(number, 0);
    }

    public synchronized Map<Integer, Integer> getAllVolumes() {
        return new HashMap<>(tankerVolumes);
    }
}