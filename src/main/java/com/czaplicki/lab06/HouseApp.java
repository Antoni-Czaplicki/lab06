package com.czaplicki.lab06;

import com.czaplicki.lab06.objects.House;
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

public class HouseApp extends Application {

    private House houseModel = null; // Obiekt House
    private TextArea houseDetails; // Pole wyświetlające szczegóły modelu
    private Thread serverThread; // Wątek serwera
    private ServerSocket serverSocket; // Gniazdo serwera, zmienna żeby móc zamknąć serwer
    private Thread updateThread; // Wątek aktualizujący szczegóły modelu

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("House Application");

        // Pola do wpisania parametrów
        Label portFieldLabel = new Label("Port:");
        TextField portField = new TextField("7090");
        portField.setPromptText("Port (np. 7090)");

        Label capacityFieldLabel = new Label("Capacity:");
        TextField capacityField = new TextField("100");
        capacityField.setPromptText("Capacity (np. 100)");

        Label currentVolumeFieldLabel = new Label("Current Volume:");
        TextField currentVolumeField = new TextField("0");
        currentVolumeField.setPromptText("Current Volume (np. 0)");


        Label officeHostFieldLabel = new Label("Office Host:");
        TextField officeHostField = new TextField("127.0.0.1");
        officeHostField.setPromptText("Office Host (np. 127.0.0.1)");

        Label officePortFieldLabel = new Label("Office Port:");
        TextField officePortField = new TextField("7092");
        officePortField.setPromptText("Office Port (np. 7092)");


        // Przycisk Dodaj/Zastąp
        Button addOrReplaceButton = new Button("Dodaj");

        // Przycisk Usuń
        Button removeButton = new Button("Usuń");
        removeButton.setDisable(true); // Początkowo wyłączony

        // Pole do wyświetlania szczegółów obiektu
        houseDetails = new TextArea();
        houseDetails.setEditable(false);
        houseDetails.setPromptText("Szczegóły obiektu House");

        // Obsługa przycisku Dodaj/Zastąp
        addOrReplaceButton.setOnAction(e -> {
            try {
                int port = Integer.parseInt(portField.getText());
                int capacity = Integer.parseInt(capacityField.getText());
                int currentVolume = Integer.parseInt(currentVolumeField.getText());
                String officeHost = officeHostField.getText();
                int officePort = Integer.parseInt(officePortField.getText());

                if (houseModel != null) {
                    stopServer();
                    if (updateThread != null && updateThread.isAlive()) {
                        updateThread.interrupt();
                    }
                }

                houseModel = new House(port, capacity, currentVolume, officeHost, officePort);

                startServer(port);
                updateHouseDetails();
                updateThread = new Thread(() -> {
                    while (houseModel != null && !Thread.currentThread().isInterrupted()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            break;
                        }
                        if (houseModel != null) {
                            houseModel.produceWaste((int) (Math.random() * 10) + 1);
                            if (houseModel.isFull()) {
                                houseModel.orderService(officeHost, officePort);
                            }
                        } else {
                            break;
                        }
                        Platform.runLater(this::updateHouseDetails);
                    }
                });
                updateThread.start();
                addOrReplaceButton.setText("Zastąp");
                removeButton.setDisable(false);
            } catch (NumberFormatException ex) {
                showAlert("Błąd", "Nieprawidłowe dane wejściowe.");
            }
        });

        // Obsługa przycisku Usuń
        removeButton.setOnAction(e -> {
            stopServer();
            houseModel = null;
            updateHouseDetails();
            addOrReplaceButton.setText("Dodaj");
            removeButton.setDisable(true);
        });

        // Układ GUI
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.getChildren().addAll(
                new Label("Parametry House:"),
                portFieldLabel, portField,
                capacityFieldLabel, capacityField,
                currentVolumeFieldLabel, currentVolumeField,
                new Label("Parametry Office:"),
                officeHostFieldLabel, officeHostField,
                officePortFieldLabel, officePortField,
                addOrReplaceButton, removeButton,
                new Label("Szczegóły House:"),
                houseDetails
        );

        Scene scene = new Scene(layout, 600, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Uruchomienie serwera
    private void startServer(int port) {
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("Serwer uruchomiony na porcie: " + port);
                while (!Thread.currentThread().isInterrupted()) {
                    try (Socket clientSocket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                        String request = in.readLine();
                        log("Otrzymano żądanie: " + request);

                        if (request.startsWith("gp:")) {
                            int max = Integer.parseInt(request.substring(3));
                            int pumpedOut = houseModel.getPumpOut(max);
                            log("Opróżniono " + pumpedOut + " jednostek.");
                            out.println(pumpedOut);
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

    // Aktualizacja szczegółów obiektu House
    private void updateHouseDetails() {
        if (houseModel != null) {
            houseDetails.setText(
                    "Host: " + houseModel.getHost() + "\n" +
                            "Port: " + houseModel.getPort() + "\n" +
                            "Capacity: " + houseModel.getCapacity() + "\n" +
                            "Current Volume: " + houseModel.getCurrentVolume() + "\n" +
                            "Office Address: " + houseModel.getOfficeHost() + ":" + houseModel.getOfficePort() + "\n");
        } else {
            houseDetails.setText("");
        }
    }

    // Wyświetlanie logów w polu tekstowym
    private void log(String message) {
        Platform.runLater(() -> houseDetails.appendText(message + "\n"));
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