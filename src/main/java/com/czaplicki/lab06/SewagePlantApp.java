package com.czaplicki.lab06;

import com.czaplicki.lab06.server.SewagePlantServer;
import com.czaplicki.lab06.service.SewagePlantService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SewagePlantApp extends Application {

    private SewagePlantService sewageService;
    private SewagePlantServer sewageServer;
    private TextArea sewageDetails;
    private Thread updateThread;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Sewage Plant Application");

        Label portLabel = new Label("Sewage Plant Port:");
        TextField portField = new TextField("7091");

        Button startButton = new Button("Start SewagePlant");
        Button stopButton = new Button("Stop SewagePlant");
        stopButton.setDisable(true);

        sewageDetails = new TextArea();
        sewageDetails.setEditable(false);

        startButton.setOnAction(e -> {
            try {
                int port = Integer.parseInt(portField.getText());
                sewageService = new SewagePlantService(port);
                sewageServer = new SewagePlantServer(sewageService, this::log);
                sewageServer.start();

                // Periodically update the status in the TextArea
                updateThread = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            break;
                        }
                        Platform.runLater(this::updateSewageDetails);
                    }
                });
                updateThread.start();

                startButton.setDisable(true);
                stopButton.setDisable(false);

                log("Sewage Plant started on port " + port);
            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid port number.");
            }
        });

        stopButton.setOnAction(e -> {
            // stop server
            if (sewageServer != null) {
                sewageServer.stop();
            }
            // stop update thread
            if (updateThread != null && updateThread.isAlive()) {
                updateThread.interrupt();
            }
            sewageService = null;
            updateSewageDetails();

            startButton.setDisable(false);
            stopButton.setDisable(true);
        });

        VBox root = new VBox(10,
                portLabel, portField,
                startButton, stopButton,
                new Label("Logs / Details:"),
                sewageDetails
        );
        root.setPadding(new Insets(10));

        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();
    }

    private void updateSewageDetails() {
        if (sewageService == null) {
            sewageDetails.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder("Tanker volumes:\n");
        sewageService.getAllVolumes().forEach((tankerId, volume) -> {
            sb.append("Tanker #").append(tankerId).append(": ").append(volume).append(" units\n");
        });
        sewageDetails.setText(sb.toString());
    }

    private void log(String message) {
        Platform.runLater(() -> sewageDetails.appendText(message + "\n"));
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