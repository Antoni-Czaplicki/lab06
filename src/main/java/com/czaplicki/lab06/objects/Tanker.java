package com.czaplicki.lab06.objects;

import com.czaplicki.lab06.SocketUtils;

import java.io.IOException;

public class Tanker {
    private int id; // Unikalny numer cysterny (przyznawany przez Biuro)
    private String host; // Host, na którym działa cysterna
    private int port; // Port serwera cysterny
    private boolean isReady; // Czy cysterna jest gotowa do pracy
    private int capacity; // Maksymalna pojemność cysterny
    private int currentVolume; // Aktualna ilość przewożonych nieczystości

    public Tanker(int id, String host, int port, int capacity) {
        this.id = id;
        this.port = port;
        this.capacity = capacity;
        this.currentVolume = 0;
        this.isReady = false;
        this.host = host != null ? host : SocketUtils.getLocalHost();
    }

    // Gettery i settery
    public int getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getCurrentVolume() {
        return currentVolume;
    }

    public void setCurrentVolume(int volume) {
        this.currentVolume = volume;
    }

    // Metoda transportująca ścieki do oczyszczalni
    public void deliverToSewagePlant(String sewageHost, int sewagePort) {
        if (currentVolume > 0) {
            try {
                String request = "spi:" + id + "," + currentVolume;
                String response = SocketUtils.sendRequest(sewageHost, sewagePort, request);
                System.out.println("Cysterna nr " + id + " dostarczyła " + currentVolume + " jednostek do oczyszczalni.");
                currentVolume = 0; // Wyzerowanie zawartości cysterny
            } catch (IOException e) {
                System.err.println("Błąd dostarczania do oczyszczalni: " + e.getMessage());
            }
        } else {
            System.out.println("Cysterna nr " + id + " jest pusta. Nic do dostarczenia.");
        }
    }
}