package com.czaplicki.lab06;

import com.czaplicki.lab06.server.TankerServer;
import com.czaplicki.lab06.service.TankerService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class TankerApp extends Application {

    private TankerService tankerService;
    private TankerServer tankerServer;
    private TextArea tankerDetails;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Tanker Application");

        Label portLabel = new Label("Tanker Port:");
        TextField portField = new TextField("7093");

        Label officeHostLabel = new Label("Office Host:");
        TextField officeHostField = new TextField("127.0.0.1");

        Label officePortLabel = new Label("Office Port:");
        TextField officePortField = new TextField("7092");

        Label capacityLabel = new Label("Tanker Capacity:");
        TextField capacityField = new TextField("100");

        Button startButton = new Button("Start Tanker");
        Button stopButton = new Button("Stop Tanker");
        stopButton.setDisable(true);

        tankerDetails = new TextArea();
        tankerDetails.setEditable(false);

        startButton.setOnAction(e -> {
            try {
                int port = Integer.parseInt(portField.getText());
                int capacity = Integer.parseInt(capacityField.getText());
                String officeHost = officeHostField.getText();
                int officePort = Integer.parseInt(officePortField.getText());

                // Create the service
                tankerService = new TankerService(port, capacity);
                tankerService.setOfficeConnection(officeHost, officePort);

                // Start server
                tankerServer = new TankerServer(tankerService, this::log);
                tankerServer.start();

                // Register and set ready
                tankerService.registerInOffice();
                tankerService.setReady();

                startButton.setDisable(true);
                stopButton.setDisable(false);

                log("Tanker started and registered.");
            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid input.");
            } catch (IOException ex) {
                showAlert("Error", "Failed to register or set ready: " + ex.getMessage());
            }
        });

        stopButton.setOnAction(e -> {
            if (tankerServer != null) {
                tankerServer.stop();
            }
            startButton.setDisable(false);
            stopButton.setDisable(true);
        });

        VBox root = new VBox(10, portLabel, portField,
                officeHostLabel, officeHostField,
                officePortLabel, officePortField,
                capacityLabel, capacityField,
                startButton, stopButton,
                new Label("Logs:"), tankerDetails
        );
        root.setPadding(new Insets(10));

        primaryStage.setScene(new Scene(root, 400, 600));
        primaryStage.show();
    }

    private void log(String message) {
        Platform.runLater(() -> tankerDetails.appendText(message + "\n"));
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