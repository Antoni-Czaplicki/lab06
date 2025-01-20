package com.czaplicki.lab06.objects;

import com.czaplicki.lab06.SocketUtils;

import java.util.HashMap;
import java.util.Map;

public class SewagePlant {
    private int port;
    private String host;
    private Map<Integer, Integer> tankerVolumes = new HashMap<>();

    public SewagePlant(int port) {
        this.port = port;
        this.host = SocketUtils.getLocalHost();
    }

    public void setPumpIn(int number, int volume) {
        tankerVolumes.put(number, tankerVolumes.getOrDefault(number, 0) + volume);
    }

    public int getStatus(int number) {
        return tankerVolumes.getOrDefault(number, 0);
    }

    public void setPayoff(int number) {
        tankerVolumes.put(number, 0);
    }

    public String getHost() {
        return host;
    }
}