package com.czaplicki.lab06;

import com.czaplicki.lab06.server.OfficeServer;
import com.czaplicki.lab06.service.OfficeService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class OfficeApp extends Application {

    private OfficeService officeService;
    private OfficeServer officeServer;

    private TextArea officeDetails;
    private Thread updateThread;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Office Application");

        Label portLabel = new Label("Office Port:");
        TextField portField = new TextField("7092");

        Label sewageHostLabel = new Label("Sewage Host:");
        TextField sewageHostField = new TextField("127.0.0.1");

        Label sewagePortLabel = new Label("Sewage Port:");
        TextField sewagePortField = new TextField("7091");

        Button startButton = new Button("Start Office");
        Button stopButton = new Button("Stop Office");
        stopButton.setDisable(true);

        Button payOffButton = new Button("Payoff All");
        payOffButton.setOnAction(e -> {
            if (officeService != null) {
                officeService.payoffAll();
                updateOfficeDetails();
            }
        });

        Button getTankersInSewagePlantButton = new Button("Get Tankers in Sewage Plant");
        getTankersInSewagePlantButton.setOnAction(e -> {
            if (officeService != null) {
                log(officeService.getAllTankersInSewagePlant());
            }
        });

        officeDetails = new TextArea();
        officeDetails.setEditable(false);

        startButton.setOnAction(e -> {
            try {
                int port = Integer.parseInt(portField.getText());
                String sHost = sewageHostField.getText();
                int sPort = Integer.parseInt(sewagePortField.getText());

                officeService = new OfficeService(port, sHost, sPort);
                officeServer = new OfficeServer(officeService, this::log);
                officeServer.start();

                // Start a background thread to update the UI with tanker statuses
                updateThread = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            break;
                        }
                        Platform.runLater(this::updateOfficeDetails);
                    }
                });
                updateThread.start();

                startButton.setDisable(true);
                stopButton.setDisable(false);

                log("Office started on port " + port);
            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid input.");
            }
        });

        stopButton.setOnAction(e -> {
            // stop server
            if (officeServer != null) {
                officeServer.stop();
            }
            // stop update thread
            if (updateThread != null && updateThread.isAlive()) {
                updateThread.interrupt();
            }
            officeService = null;
            updateOfficeDetails();

            startButton.setDisable(false);
            stopButton.setDisable(true);
        });

        VBox layout = new VBox(10,
                portLabel, portField,
                sewageHostLabel, sewageHostField,
                sewagePortLabel, sewagePortField,
                startButton, stopButton,
                getTankersInSewagePlantButton, payOffButton,
                new Label("Logs / Details:"),
                officeDetails
        );
        layout.setPadding(new Insets(10));

        primaryStage.setScene(new Scene(layout, 450, 500));
        primaryStage.show();
    }

    private void updateOfficeDetails() {
        if (officeService == null) {
            officeDetails.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Tanker status:\n");
        officeService.getTankerMap().forEach((id, data) -> {
            sb.append("Tanker #").append(id)
                    .append(" [host=").append(data.host)
                    .append(", port=").append(data.port)
                    .append(", ready=").append(data.ready)
                    .append("]\n");
        });
        officeDetails.setText(sb.toString());
    }

    private void log(String message) {
        Platform.runLater(() -> officeDetails.appendText(message + "\n"));
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