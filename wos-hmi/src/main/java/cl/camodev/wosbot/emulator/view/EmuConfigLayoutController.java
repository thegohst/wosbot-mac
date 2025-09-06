package cl.camodev.wosbot.emulator.view;

import java.io.File;
import java.util.HashMap;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.emulator.EmulatorType;
import cl.camodev.wosbot.emulator.model.EmulatorAux;
import cl.camodev.wosbot.serv.impl.ServConfig;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import cl.camodev.wosbot.emulator.AdbDeviceService;
import java.util.Optional;

public class EmuConfigLayoutController {

	@FXML
	private TableView<EmulatorAux> tableviewEmulators;

	@FXML
	private TableColumn<EmulatorAux, Boolean> tableColumnActive;

	@FXML
	private TableColumn<EmulatorAux, String> tableColumnEmulatorName;

	@FXML
	private TableColumn<EmulatorAux, String> tableColumnEmulatorPath;

	@FXML
	private TableColumn<EmulatorAux, Void> tableColumnEmulatorAction;

	@FXML
	private TextField textfieldMaxConcurrentInstances;

	@FXML
	private TextField textfieldMaxIdleTime;

	private final FileChooser fileChooser = new FileChooser();

	// Lista fija de emuladores que se derivan del enum
	private final ObservableList<EmulatorAux> emulatorList = FXCollections.observableArrayList();

	public void initialize() {
		// Se obtiene la configuración global
		HashMap<String, String> globalConfig = ServConfig.getServices().getGlobalConfig();
		// Se recupera el emulador activo en la configuración
		String currentEmulator = globalConfig.get(EnumConfigurationKey.CURRENT_EMULATOR_STRING.name());

		// Se llena la lista recorriendo los valores del enum
		for (EmulatorType type : EmulatorType.values()) {
			String defaultPath = globalConfig.getOrDefault(type.getConfigKey(), "");
			EmulatorAux emulator = new EmulatorAux(type, defaultPath);
			emulator.setActive(type.name().equals(currentEmulator));
			emulatorList.add(emulator);
		}

		// Configurar columna de nombre (lectura únicamente)
		tableColumnEmulatorName.setCellValueFactory(new PropertyValueFactory<>("name"));
		// Configurar columna que muestra la ruta
		tableColumnEmulatorPath.setCellValueFactory(new PropertyValueFactory<>("path"));

		// Configurar la columna de selección con RadioButton para elegir el emulador activo
		tableColumnActive.setCellValueFactory(cellData -> cellData.getValue().activeProperty());

		final ToggleGroup toggleGroup = new ToggleGroup();
		tableColumnActive.setCellFactory(column -> new TableCell<EmulatorAux, Boolean>() {
			private final RadioButton radioButton = new RadioButton();
			{
				radioButton.setToggleGroup(toggleGroup);
				radioButton.setOnAction(event -> {
					EmulatorAux selected = getTableView().getItems().get(getIndex());
					// Desactiva la bandera activa en todos
					for (EmulatorAux e : emulatorList) {
						e.setActive(false);
					}
					selected.setActive(true);
					tableviewEmulators.refresh();
				});
			}

			@Override
			protected void updateItem(Boolean item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
				} else {
					radioButton.setSelected(item != null && item);
					setGraphic(radioButton);
				}
			}
		});

		// Configurar la columna de acción para actualizar la ruta
		tableColumnEmulatorAction.setCellFactory(col -> new TableCell<EmulatorAux, Void>() {
			private final Button btn = new Button("...");

			{
				btn.setOnAction(event -> {
					EmulatorAux emulator = getTableView().getItems().get(getIndex());
					
					// For Android Studio emulators, use ADB-based selection
					if (emulator.getEmulatorType() == EmulatorType.ANDROID_STUDIO) {
						selectEmulatorFromAdb(emulator);
					} else {
						// For other emulators, use file chooser
						File selectedFile = openFileChooser("Select " + emulator.getEmulatorType().getExecutableName());
						if (selectedFile != null) {
							// Verifica que el archivo seleccionado coincida con el esperado
							if (!selectedFile.getName().equalsIgnoreCase(emulator.getEmulatorType().getExecutableName())) {
								showError("File not valid, please select: " + emulator.getEmulatorType().getExecutableName());
								return;
							}
							emulator.setPath(selectedFile.getParent());
							tableviewEmulators.refresh();
						}
					}
				});
			}

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(empty ? null : btn);
			}
		});

		// Asignar la lista fija al TableView
		tableviewEmulators.setItems(emulatorList);

		textfieldMaxConcurrentInstances.setText(globalConfig.getOrDefault(EnumConfigurationKey.MAX_RUNNING_EMULATORS_INT.name(), "1"));
		textfieldMaxIdleTime.setText(globalConfig.getOrDefault(EnumConfigurationKey.MAX_IDLE_TIME_INT.name(), "15"));
	}

	// Guarda la configuración, recorriendo la lista para extraer la ruta y determinar el emulador activo
	@FXML
	private void handleSaveConfiguration() {
		String activeEmulatorName = null;
		for (EmulatorAux emulator : emulatorList) {
			if (emulator.isActive()) {
				activeEmulatorName = emulator.getEmulatorType().name();
				break;
			}
		}
		if (activeEmulatorName == null) {
			showError("Missing active emulator. Please select one.");
			return;
		}

		// Guarda el número máximo de instancias
		String maxInstances = textfieldMaxConcurrentInstances.getText();
		if (maxInstances.isEmpty()) {
			showError("Max instances cannot be empty.");
			return;
		}

		// Guarda el tiempo máximo de inactividad
		String maxIdleTime = textfieldMaxIdleTime.getText();
		if (maxIdleTime.isEmpty()) {
			showError("Max idle time cannot be empty.");
			return;
		}
		// Guarda la configuración usando la clave definida en cada valor del enum
		for (EmulatorAux emulator : emulatorList) {
			ServScheduler.getServices().saveEmulatorPath(emulator.getEmulatorType().getConfigKey(), emulator.getPath());
		}

		ServScheduler.getServices().saveEmulatorPath(EnumConfigurationKey.MAX_IDLE_TIME_INT.name(), maxIdleTime);
		ServScheduler.getServices().saveEmulatorPath(EnumConfigurationKey.MAX_RUNNING_EMULATORS_INT.name(), maxInstances);
		ServScheduler.getServices().saveEmulatorPath(EnumConfigurationKey.CURRENT_EMULATOR_STRING.name(), activeEmulatorName);
		showInfo("Config saved successfully");
	}

	private File openFileChooser(String title) {
		fileChooser.setTitle(title);
		fileChooser.getExtensionFilters().clear();
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Executable Files", "*.exe"));
		return fileChooser.showOpenDialog(null);
	}

	private void showError(String message) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	private void showInfo(String message) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Éxito");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}
	
	private void selectEmulatorFromAdb(EmulatorAux emulator) {
		Optional<AdbDeviceService.AdbDevice> selectedDevice = EmulatorSelectionDialog.showAndWait(null);
		
		if (selectedDevice.isPresent()) {
			AdbDeviceService.AdbDevice device = selectedDevice.get();
			
			// For Android Studio emulators, save the ADB device ID
			if (emulator.getEmulatorType() == EmulatorType.ANDROID_STUDIO) {
				emulator.setPath("adb:" + device.getDeviceId());
				tableviewEmulators.refresh();
				showInfo("Android Studio emulator connected: " + device.getName());
			} else if (device.getType() == emulator.getEmulatorType()) {
				// Matching emulator type detected
				emulator.setPath("adb:" + device.getDeviceId());
				tableviewEmulators.refresh();
				showInfo("Emulator connected: " + device.getName());
			} else {
				showError("Selected device does not match the expected emulator type: " + emulator.getEmulatorType().getDisplayName());
			}
		}
	}
}
