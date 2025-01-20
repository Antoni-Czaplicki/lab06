package com.czaplicki.lab06.objects;

import com.czaplicki.lab06.SocketUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Office {
    private String host;
    private int port;
    private String sewageHost;
    private int sewagePort;
    private Map<Integer, Tanker> tankerMap = new HashMap<>();
    private int nextTankerId = 1; // Generator unikalnych ID dla cystern

    public Office(int port, String sewageHost, int sewagePort) {
        this.port = port;
        this.sewageHost = sewageHost;
        this.sewagePort = sewagePort;
        this.host = SocketUtils.getLocalHost();
    }

    public String getHost() {
        return host;
    }

    // Rejestracja cysterny
    public int register(String host, int port, int capacity) {
        int tankerId = nextTankerId++;
        Tanker tanker = new Tanker(tankerId, host, port, capacity);
        tankerMap.put(tankerId, tanker);
        System.out.println("Zarejestrowano cysternę nr " + tankerId);
        return tankerId;
    }

    // Ustawienie gotowości cysterny
    public void setReadyToServe(int tankerId) {
        Tanker tanker = tankerMap.get(tankerId);
        if (tanker != null) {
            tanker.setReady(true);
            System.out.println("Cysterna nr " + tankerId + " jest gotowa.");
        } else {
            System.out.println("Nie znaleziono cysterny o ID: " + tankerId);
        }
    }

    // Przypisanie zadania cysternie
    public void assignJob(String houseHost, int housePort) {
        for (Tanker tanker : tankerMap.values()) {
            if (tanker.isReady()) {
                try {
                    String request = "gp:" + tanker.getCapacity(); // Prośba o opróżnienie do pełna
                    String response = SocketUtils.sendRequest(houseHost, housePort, request);
                    int pumpedOut = Integer.parseInt(response);
                    tanker.setCurrentVolume(pumpedOut);
                    tanker.setReady(false); // Cysterna zajęta
                    System.out.println("Zadanie przypisane cysternie nr " + tanker.getId());

                    // Transport do oczyszczalni
                    tanker.deliverToSewagePlant(sewageHost, sewagePort);

                    return; // Zadanie przydzielone, przerywamy
                } catch (IOException e) {
                    System.err.println("Błąd podczas przypisywania zadania: " + e.getMessage());
                }
            }
        }
        System.out.println("Brak dostępnych cystern do przypisania zadania.");
    }
}