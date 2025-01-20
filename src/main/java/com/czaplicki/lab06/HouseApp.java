package com.czaplicki.lab06;

import com.czaplicki.lab06.server.HouseServer;
import com.czaplicki.lab06.service.HouseService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HouseApp extends Application {

    private HouseService houseService;
    private HouseServer houseServer;
    private Thread produceThread;

    private TextArea houseDetails;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("House Application");

        Label portLabel = new Label("House Port:");
        TextField portField = new TextField("7090");

        Label capacityLabel = new Label("Capacity:");
        TextField capacityField = new TextField("100");

        Label currentVolumeLabel = new Label("Initial Volume:");
        TextField currentVolumeField = new TextField("0");

        Label officeHostLabel = new Label("Office Host:");
        TextField officeHostField = new TextField("127.0.0.1");

        Label officePortLabel = new Label("Office Port:");
        TextField officePortField = new TextField("7092");

        Button startButton = new Button("Start House");
        Button stopButton = new Button("Stop House");
        stopButton.setDisable(true);

        houseDetails = new TextArea();
        houseDetails.setEditable(false);

        startButton.setOnAction(e -> {
            try {
                int port = Integer.parseInt(portField.getText());
                int capacity = Integer.parseInt(capacityField.getText());
                int currentVol = Integer.parseInt(currentVolumeField.getText());
                String offHost = officeHostField.getText();
                int offPort = Integer.parseInt(officePortField.getText());

                // Create the service
                houseService = new HouseService(port, capacity, currentVol);
                houseService.setOfficeConnection(offHost, offPort);

                // Start the server
                houseServer = new HouseServer(houseService, this::log);
                houseServer.start();

                // Start a background thread to produce waste regularly
                produceThread = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            break;
                        }
                        // produce random amount
                        houseService.produceWaste((int) (Math.random() * 10) + 1);
                        // if full, order service
                        if (houseService.isFull()) {
                            houseService.orderService();
                        }
                        // update details in UI
                        Platform.runLater(this::updateHouseDetails);
                    }
                });
                produceThread.start();

                updateHouseDetails();
                startButton.setDisable(true);
                stopButton.setDisable(false);

                log("House started on port " + port);

            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid input.");
            }
        });

        stopButton.setOnAction(e -> {
            // Stop produce thread
            if (produceThread != null && produceThread.isAlive()) {
                produceThread.interrupt();
            }
            // Stop house server
            if (houseServer != null) {
                houseServer.stop();
            }
            houseService = null;
            updateHouseDetails();

            startButton.setDisable(false);
            stopButton.setDisable(true);
        });

        VBox layout = new VBox(10,
                portLabel, portField,
                capacityLabel, capacityField,
                currentVolumeLabel, currentVolumeField,
                officeHostLabel, officeHostField,
                officePortLabel, officePortField,
                startButton, stopButton,
                new Label("Logs / Details:"),
                houseDetails
        );
        layout.setPadding(new Insets(10));

        primaryStage.setScene(new Scene(layout, 400, 600));
        primaryStage.show();
    }

    private void updateHouseDetails() {
        if (houseService == null) {
            houseDetails.setText("");
            return;
        }
        houseDetails.setText(String.format(
                "Host: %s\nPort: %d\nCapacity: %d\nCurrent Volume: %d\nOffice: %s:%d\n",
                houseService.getHost(),
                houseService.getPort(),
                houseService.getCapacity(),
                houseService.getCurrentVolume(),
                houseService.getOfficeHost(),
                houseService.getOfficePort()
        ));
    }

    private void log(String message) {
        Platform.runLater(() -> houseDetails.appendText(message + "\n"));
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}