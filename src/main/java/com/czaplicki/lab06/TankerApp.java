package com.czaplicki.lab06;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TankerApp extends Application {

    private int port; // Port cysterny
    private String officeHost;
    private int officePort;
    private String sewageHost;
    private int sewagePort;
    private int maxCapacity;
    private int currentVolume;
    private Thread serverThread;
    private ServerSocket serverSocket;
    private TextArea tankerDetails;
    private int tankerId;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Tanker Application");

        // Pola do wpisania parametrów
        Label portFieldLabel = new Label("Port:");
        TextField portField = new TextField("7093");
        portField.setPromptText("Port cysterny");

        Label officeHostFieldLabel = new Label("Host Biura:");
        TextField officeHostField = new TextField("127.0.0.1");
        officeHostField.setPromptText("Host Biura");

        Label officePortFieldLabel = new Label("Port Biura:");
        TextField officePortField = new TextField("7092");
        officePortField.setPromptText("Port Biura");


        Label capacityFieldLabel = new Label("Pojemność:");
        TextField capacityField = new TextField("100");
        capacityField.setPromptText("Maksymalna pojemność");

        // Przycisk uruchomienia cysterny
        Button startTankerButton = new Button("Uruchom cysternę");
        Button stopTankerButton = new Button("Zatrzymaj cysternę");
        stopTankerButton.setDisable(true);

        // Pole do wyświetlania szczegółów cysterny
        tankerDetails = new TextArea();
        tankerDetails.setEditable(false);
        tankerDetails.setPromptText("Szczegóły cysterny");

        // Obsługa przycisku Uruchom cysternę
        startTankerButton.setOnAction(e -> {
            try {
                port = Integer.parseInt(portField.getText());
                officeHost = officeHostField.getText();
                officePort = Integer.parseInt(officePortField.getText());
                maxCapacity = Integer.parseInt(capacityField.getText());
                currentVolume = 0;

                startServer();
                registerInOffice();
                setReady();
                startTankerButton.setDisable(true);
                stopTankerButton.setDisable(false);
            } catch (NumberFormatException ex) {
                showAlert("Błąd", "Nieprawidłowe dane wejściowe.");
            }
        });

        // Obsługa przycisku Zatrzymaj cysternę
        stopTankerButton.setOnAction(e -> {
            stopServer();
            startTankerButton.setDisable(false);
            stopTankerButton.setDisable(true);
        });

        // Układ GUI
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.getChildren().addAll(
                new Label("Parametry cysterny:"),
                portField, officeHostField, officePortField, capacityField,
                startTankerButton, stopTankerButton,
                new Label("Szczegóły cysterny:"),
                tankerDetails
        );

        Scene scene = new Scene(layout, 400, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Uruchomienie serwera cysterny
    private void startServer() {
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("Serwer cysterny uruchomiony na porcie: " + port);

                while (!Thread.currentThread().isInterrupted()) {
                    try (Socket clientSocket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                        String request = in.readLine();
                        log("Otrzymano żądanie: " + request);

                        if (request.startsWith("sj:")) { // Zlecenie od Biura
                            String[] parts = request.substring(3).split(",");
                            String houseHost = parts[0];
                            int housePort = Integer.parseInt(parts[1]);
                            setJob(houseHost, housePort);
                            out.println(1);
                        } else if (request.startsWith("rc:")) { // Response confirmation with tanker id and sewage plant details
                            String[] parts = request.substring(3).split(",");
                            int tankerId = Integer.parseInt(parts[0]);
                            sewageHost = parts[1];
                            sewagePort = Integer.parseInt(parts[2]);
                            this.tankerId = tankerId;
                        } else {
                            log("Nieznane żądanie.");
                            out.println(0);
                        }
                    } catch (IOException e) {
                        if (serverSocket.isClosed()) {
                            log("Serwer został zamknięty.");
                            break;
                        }
                        log("Błąd obsługi klienta: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                log("Błąd serwera: " + e.getMessage());
            }
        });
        serverThread.start();
    }

    // Zatrzymanie serwera cysterny
    private void stopServer() {
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                log("Błąd zamykania serwera: " + e.getMessage());
            }
            log("Serwer został zatrzymany.");
        }
    }

    // Rejestracja cysterny w Biurze
    private void registerInOffice() {
        try {
            String request = "r:" + "127.0.0.1" + "," + port + "," + maxCapacity;
            String response = SocketUtils.sendRequest(officeHost, officePort, request);
            log("Zarejestrowano w Biurze jako cysterna nr: " + response);
        } catch (IOException e) {
            log("Błąd rejestracji w Biurze: " + e.getMessage());
        }
    }

    // Ustawienie gotowości cysterny w Biurze
    private void setReady() {
        try {
            System.out.println("sr:" + tankerId);
            String request = "sr:" + tankerId;
            SocketUtils.sendRequest(officeHost, officePort, request);
            log("Zgłoszono gotowość w Biurze.");
        } catch (IOException e) {
            log("Błąd zgłaszania gotowości: " + e.getMessage());
        }
    }

    // Przyjęcie zlecenia od Biura
    public void setJob(String houseHost, int housePort) {
        log("Otrzymano zlecenie dla domu: " + houseHost + ":" + housePort);

        try {
            // Pobranie nieczystości z domu
            String request = "gp:" + maxCapacity;
            String response = SocketUtils.sendRequest(houseHost, housePort, request);
            int pumpedOut = Integer.parseInt(response);
            currentVolume = pumpedOut;
            log("Wypompowano: " + pumpedOut + " jednostek.");

            // Dostarczenie nieczystości do oczyszczalni
            deliverToSewagePlant();
            setReady();
        } catch (IOException e) {
            log("Błąd podczas realizacji zlecenia: " + e.getMessage());
        }
    }

    // Dostarczenie nieczystości do oczyszczalni
    private void deliverToSewagePlant() {
        try {
            String request = "spi:" + tankerId + "," + currentVolume;
            SocketUtils.sendRequest(sewageHost, sewagePort, request);
            log("Dostarczono " + currentVolume + " jednostek do oczyszczalni.");
            currentVolume = 0;
            setReady();
        } catch (IOException e) {
            log("Błąd dostarczania do oczyszczalni: " + e.getMessage());
        }
    }

    // Wyświetlanie logów w GUI
    private void log(String message) {
        Platform.runLater(() -> tankerDetails.appendText(message + "\n"));
    }

    // Wyświetlanie alertów
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}