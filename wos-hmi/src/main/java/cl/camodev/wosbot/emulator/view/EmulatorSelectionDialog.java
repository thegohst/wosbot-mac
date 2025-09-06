package cl.camodev.wosbot.emulator.view;

import cl.camodev.wosbot.emulator.AdbDeviceService;
import cl.camodev.wosbot.emulator.AdbDeviceService.AdbDevice;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

/**
 * Dialog for selecting an emulator/device from ADB connected devices
 */
public class EmulatorSelectionDialog extends Dialog<AdbDevice> {
    
    private ListView<AdbDevice> deviceListView;
    private Button refreshButton;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    
    public EmulatorSelectionDialog(Stage parent) {
        if (parent != null && parent.getScene() != null) {
            initOwner(parent);
        }
        setTitle("Select Android Device/Emulator");
        setHeaderText("Choose from available Android devices and emulators connected via ADB");
        
        setupDialogPane();
        setupButtons();
        
        // Load devices initially
        refreshDevices();
    }
    
    private void setupDialogPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Status section
        HBox statusBox = new HBox(10);
        statusLabel = new Label("Loading devices...");
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(20, 20);
        statusBox.getChildren().addAll(statusLabel, progressIndicator);
        
        // Device list
        deviceListView = new ListView<>();
        deviceListView.setPrefHeight(200);
        deviceListView.setCellFactory(listView -> new DeviceListCell());
        
        // Refresh button
        refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshDevices());
        
        // Instructions
        Label instructions = new Label(
            "• Make sure ADB is enabled and devices are connected\n" +
            "• For Android Studio emulators, start them from Android Studio first\n" +
            "• Physical devices need USB debugging enabled"
        );
        instructions.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        
        content.getChildren().addAll(statusBox, deviceListView, refreshButton, instructions);
        getDialogPane().setContent(content);
        getDialogPane().setPrefWidth(500);
    }
    
    private void setupButtons() {
        ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        getDialogPane().getButtonTypes().addAll(selectButtonType, cancelButtonType);
        
        // Enable/disable select button based on selection
        Node selectButton = getDialogPane().lookupButton(selectButtonType);
        selectButton.setDisable(true);
        
        deviceListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> selectButton.setDisable(newVal == null)
        );
        
        // Set result converter
        setResultConverter(buttonType -> {
            if (buttonType == selectButtonType) {
                return deviceListView.getSelectionModel().getSelectedItem();
            }
            return null;
        });
    }
    
    private void refreshDevices() {
        progressIndicator.setVisible(true);
        statusLabel.setText("Checking ADB connection...");
        refreshButton.setDisable(true);
        
        Task<List<AdbDevice>> loadDevicesTask = new Task<List<AdbDevice>>() {
            @Override
            protected List<AdbDevice> call() throws Exception {
                // Check if ADB is available
                if (!AdbDeviceService.isAdbAvailable()) {
                    throw new RuntimeException("ADB is not available. Please ensure Android SDK is installed and ADB is in PATH.");
                }
                
                // Start ADB server if needed
                AdbDeviceService.startAdbServer();
                
                // Give a moment for server to start
                Thread.sleep(1000);
                
                // Get connected devices
                return AdbDeviceService.getConnectedDevices();
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    List<AdbDevice> devices = getValue();
                    ObservableList<AdbDevice> deviceList = FXCollections.observableArrayList(devices);
                    deviceListView.setItems(deviceList);
                    
                    progressIndicator.setVisible(false);
                    refreshButton.setDisable(false);
                    
                    if (devices.isEmpty()) {
                        statusLabel.setText("No devices found. Make sure emulators are running and ADB is connected.");
                    } else {
                        statusLabel.setText(String.format("Found %d device(s)", devices.size()));
                    }
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    refreshButton.setDisable(false);
                    
                    Throwable exception = getException();
                    statusLabel.setText("Error: " + exception.getMessage());
                    
                    // Show error dialog
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("ADB Error");
                    alert.setHeaderText("Failed to load devices");
                    alert.setContentText(exception.getMessage());
                    alert.showAndWait();
                });
            }
        };
        
        Thread loadThread = new Thread(loadDevicesTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }
    
    /**
     * Custom list cell for displaying device information
     */
    private static class DeviceListCell extends ListCell<AdbDevice> {
        @Override
        protected void updateItem(AdbDevice device, boolean empty) {
            super.updateItem(device, empty);
            
            if (empty || device == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox content = new VBox(2);
                
                Label nameLabel = new Label(device.getName());
                nameLabel.setStyle("-fx-font-weight: bold;");
                
                Label typeLabel = new Label(
                    device.isEmulator() ? "Emulator" : "Physical Device"
                );
                typeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
                
                if (device.getType() != null) {
                    Label emulatorTypeLabel = new Label("Type: " + device.getType().getDisplayName());
                    emulatorTypeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: blue;");
                    content.getChildren().add(emulatorTypeLabel);
                }
                
                content.getChildren().addAll(nameLabel, typeLabel);
                setGraphic(content);
            }
        }
    }
    
    /**
     * Show the dialog and return the selected device
     */
    public static Optional<AdbDevice> showAndWait(Stage parent) {
        EmulatorSelectionDialog dialog = new EmulatorSelectionDialog(parent);
        return dialog.showAndWait();
    }
}
