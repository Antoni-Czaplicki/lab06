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
import java.util.HashMap;
import java.util.Map;

public class SewagePlantApp extends Application {

    private Map<Integer, Integer> tankerVolumes = new HashMap<>(); // Mapa cystern: numer -> suma przywiezionych ścieków
    private Thread serverThread; // Wątek serwera
    private ServerSocket serverSocket; // Gniazdo serwera
    private TextArea sewageDetails; // Pole do wyświetlania szczegółów

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Sewage Plant Application");

        // Pola do wpisania parametrów
        Label portLabel = new Label("Port:");
        TextField portField = new TextField("7091");
        portField.setPromptText("Port (np. 7091)");

        // Przycisk uruchomienia/zatrzymania serwera
        Button startServerButton = new Button("Uruchom serwer");
        Button stopServerButton = new Button("Zatrzymaj serwer");
        stopServerButton.setDisable(true);

        // Pole do wyświetlania szczegółów
        sewageDetails = new TextArea();
        sewageDetails.setEditable(false);
        sewageDetails.setPromptText("Szczegóły oczyszczalni");

        // Obsługa przycisku Uruchom serwer
        startServerButton.setOnAction(e -> {
            try {
                int port = Integer.parseInt(portField.getText());
                startServer(port);
                startServerButton.setDisable(true);
                stopServerButton.setDisable(false);
            } catch (NumberFormatException ex) {
                showAlert("Błąd", "Nieprawidłowy numer portu.");
            }
        });

        // Obsługa przycisku Zatrzymaj serwer
        stopServerButton.setOnAction(e -> {
            stopServer();
            startServerButton.setDisable(false);
            stopServerButton.setDisable(true);
        });

        // Układ GUI
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.getChildren().addAll(
                portLabel, portField,
                startServerButton, stopServerButton,
                new Label("Szczegóły oczyszczalni:"),
                sewageDetails
        );

        Scene scene = new Scene(layout, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Uruchomienie serwera
    private void startServer(int port) {
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("Serwer oczyszczalni uruchomiony na porcie: " + port);

                while (!Thread.currentThread().isInterrupted()) {
                    try (Socket clientSocket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                        String request = in.readLine();
                        log("Otrzymano żądanie: " + request);

                        if (request.startsWith("spi:")) {
                            String[] parts = request.substring(4).split(",");
                            int tankerNumber = Integer.parseInt(parts[0]);
                            int volume = Integer.parseInt(parts[1]);
                            setPumpIn(tankerNumber, volume);
                            out.println(1);
                        } else if (request.startsWith("gs:")) {
                            int tankerNumber = Integer.parseInt(request.substring(3));
                            int status = getStatus(tankerNumber);
                            out.println(status);
                        } else if (request.startsWith("spo:")) {
                            int tankerNumber = Integer.parseInt(request.substring(4));
                            setPayoff(tankerNumber);
                            out.println(1);
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

    // Zatrzymanie serwera
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

    // Implementacja metod ISewagePlant
    public synchronized void setPumpIn(int number, int volume) {
        tankerVolumes.put(number, tankerVolumes.getOrDefault(number, 0) + volume);
        log("Przepompowano od cysterny nr " + number + ": " + volume + " jednostek.");
        updateSewageDetails();
    }

    public synchronized int getStatus(int number) {
        return tankerVolumes.getOrDefault(number, 0);
    }

    public synchronized void setPayoff(int number) {
        tankerVolumes.put(number, 0);
        log("Rozliczono cysternę nr " + number + ".");
        updateSewageDetails();
    }

    // Aktualizacja szczegółów w GUI
    private void updateSewageDetails() {
        Platform.runLater(() -> {
            StringBuilder details = new StringBuilder("Status cystern:\n");
            tankerVolumes.forEach((number, volume) -> {
                details.append("Cysterna nr ").append(number).append(": ").append(volume).append(" jednostek\n");
            });
            sewageDetails.setText(details.toString());
        });
    }

    // Wyświetlanie logów w GUI
    private void log(String message) {
        Platform.runLater(() -> sewageDetails.appendText(message + "\n"));
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