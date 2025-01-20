package com.czaplicki.lab06;

import com.czaplicki.lab06.objects.Tanker;
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

public class OfficeApp extends Application {

    private Map<Integer, Tanker> tankerMap = new HashMap<>(); // Numer cysterny -> cysterna
    private int nextTankerId = 1; // Generator numerów cystern
    private String sewageHost = "127.0.0.1"; // Domyślny host oczyszczalni
    private int sewagePort = 7091; // Domyślny port oczyszczalni
    private Thread serverThread;
    private ServerSocket serverSocket;
    private TextArea officeDetails;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Office Application");

        // Pola do wpisania parametrów
        Label portLabel = new Label("Port:");
        TextField portField = new TextField("7092");
        portField.setPromptText("Port (np. 7092)");

        Label sewageHostLabel = new Label("Sewage Host:");
        TextField sewageHostField = new TextField("127.0.0.1");
        sewageHostField.setPromptText("Sewage Host (np. 127.0.0.1");

        Label sewagePortLabel = new Label("Sewage Port:");
        TextField sewagePortField = new TextField("7091");
        sewagePortField.setPromptText("Sewage Port (np. 7091)");

        // Przycisk uruchomienia/zatrzymania serwera
        Button startServerButton = new Button("Uruchom serwer");
        Button stopServerButton = new Button("Zatrzymaj serwer");
        stopServerButton.setDisable(true);

        // Pole do wyświetlania szczegółów
        officeDetails = new TextArea();
        officeDetails.setEditable(false);
        officeDetails.setPromptText("Szczegóły biura");

        // Obsługa przycisku Uruchom serwer
        startServerButton.setOnAction(e -> {
            try {
                int port = Integer.parseInt(portField.getText());
                startServer(port);
                startServerButton.setDisable(true);
                stopServerButton.setDisable(false);
            } catch (NumberFormatException ex) {
                showAlert("Błąd", "Nieprawidłowe dane wejściowe.");
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
                sewageHostLabel, sewageHostField,
                startServerButton, stopServerButton,
                new Label("Szczegóły biura:"),
                officeDetails
        );

        Scene scene = new Scene(layout, 400, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Uruchomienie serwera
    private void startServer(int port) {
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("Serwer biura uruchomiony na porcie: " + serverSocket.getLocalPort());

                while (!Thread.currentThread().isInterrupted()) {
                    try (Socket clientSocket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                        log("Nawiązano połączenie z klientem.");

                        String request = in.readLine();
                        log("Otrzymano żądanie: " + request);

                        if (request.startsWith("r:")) { // Rejestracja cysterny
                            String[] parts = request.substring(2).split(",");
                            String host = parts[0];
                            int portNum = Integer.parseInt(parts[1]);
                            int capacity = Integer.parseInt(parts[2]);
                            int tankerId = registerTanker(host, portNum, capacity);
                            SocketUtils.sendRequest(host, portNum, "rc:" + tankerId + "," + sewageHost + "," + sewagePort);
                            out.println(tankerId);
                        } else if (request.startsWith("o:")) { // Zamówienie usługi
                            String[] parts = request.substring(2).split(",");
                            String houseHost = parts[0];
                            int housePort = Integer.parseInt(parts[1]);
                            int result = orderService(houseHost, housePort);
                            out.println(result);
                        } else if (request.startsWith("sr:")) { // Gotowość cysterny
                            int tankerId = Integer.parseInt(request.substring(3));
                            setTankerReady(tankerId);
                            System.out.println("Gotowość cysterny: " + tankerId);
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

    // Implementacja metod IOffice
    public synchronized int registerTanker(String host, int port, int capacity) {
        Tanker tanker = new Tanker(nextTankerId++, host, port, capacity);
        tankerMap.put(tanker.getId(), tanker);
        log("Zarejestrowano cysternę nr " + tanker.getId() + " (host: " + host + ", port: " + port + ", capacity: " + capacity + ")");
        updateOfficeDetails();
        return tanker.getId();
    }

    public synchronized int orderService(String houseHost, int housePort) {
        for (Tanker tanker : tankerMap.values()) {
            if (tanker.isReady()) {
                assignJobToTanker(tanker, houseHost, housePort);
                return 1;
            }
        }
        log("Brak dostępnych cystern do realizacji zamówienia.");
        return 0;
    }

    public synchronized void setTankerReady(int tankerId) {
        System.out.println(tankerId);
        Tanker tanker = tankerMap.get(tankerId);
        System.out.println(tanker);
        if (tanker != null) {
            tanker.setReady(true);
            log("Cysterna nr " + tankerId + " zgłosiła gotowość.");
            updateOfficeDetails();
        }
    }

    private void assignJobToTanker(Tanker tanker, String houseHost, int housePort) {
        try {
            log("Przypisywanie zadania cysternie nr " + tanker.getId());
            String request = "sj:" + houseHost + "," + housePort;
            SocketUtils.sendRequest(tanker.getHost(), tanker.getPort(), request, false);
            log("Przypisano zadanie cysternie nr " + tanker.getId());
        } catch (IOException e) {
            log("Błąd podczas przypisywania zadania cysternie: " + e.getMessage());
        }
    }

    // Aktualizacja szczegółów biura w GUI
    private void updateOfficeDetails() {
        Platform.runLater(() -> {
            StringBuilder details = new StringBuilder("Status cystern:\n");
            tankerMap.forEach((id, tanker) -> {
                details.append("Cysterna nr ").append(id)
                        .append(": host=").append(tanker.getHost())
                        .append(", port=").append(tanker.getPort())
                        .append(", capacity=").append(tanker.getCapacity())
                        .append(", current volume=").append(tanker.getCurrentVolume())
                        .append(", ready=").append(tanker.isReady() ? "Yes" : "No").append("\n");
            });
            officeDetails.setText(details.toString());
        });
    }

    // Wyświetlanie logów w GUI
    private void log(String message) {
        Platform.runLater(() -> officeDetails.appendText(message + "\n"));
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