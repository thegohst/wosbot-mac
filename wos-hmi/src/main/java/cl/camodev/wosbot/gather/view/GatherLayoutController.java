package cl.camodev.wosbot.gather.view;

import cl.camodev.wosbot.common.view.AbstractProfileController;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class GatherLayoutController extends AbstractProfileController {

	@FXML
	private CheckBox checkBoxGatherCoal;

	@FXML
	private CheckBox checkBoxGatherIron;

	@FXML
	private CheckBox checkBoxGatherMeat;

	@FXML
	private CheckBox checkBoxGatherWood;

	@FXML
	private TextField textfieldLevelCoal;

	@FXML
	private TextField textfieldLevelIron;

	@FXML
	private TextField textfieldLevelMeat;
	@FXML
	private TextField textfieldLevelWood;
	@FXML
	private ComboBox<Integer> comboBoxActiveMarchQueue;

	@FXML
	private CheckBox checkBoxGatherSpeedBoost;

	@FXML
	private void initialize() {
		checkBoxMappings.put(checkBoxGatherCoal, EnumConfigurationKey.GATHER_COAL_BOOL);
		checkBoxMappings.put(checkBoxGatherIron, EnumConfigurationKey.GATHER_IRON_BOOL);
		checkBoxMappings.put(checkBoxGatherMeat, EnumConfigurationKey.GATHER_MEAT_BOOL);
		checkBoxMappings.put(checkBoxGatherWood, EnumConfigurationKey.GATHER_WOOD_BOOL);
		checkBoxMappings.put(checkBoxGatherSpeedBoost, EnumConfigurationKey.GATHER_SPEED_BOOL);
		textFieldMappings.put(textfieldLevelCoal, EnumConfigurationKey.GATHER_COAL_LEVEL_INT);
		textFieldMappings.put(textfieldLevelIron, EnumConfigurationKey.GATHER_IRON_LEVEL_INT);
		textFieldMappings.put(textfieldLevelMeat, EnumConfigurationKey.GATHER_MEAT_LEVEL_INT);
		textFieldMappings.put(textfieldLevelWood, EnumConfigurationKey.GATHER_WOOD_LEVEL_INT);

		// Initialize ComboBox with values 1-6
		comboBoxActiveMarchQueue.getItems().addAll(1, 2, 3, 4, 5, 6);
		comboBoxMappings.put(comboBoxActiveMarchQueue, EnumConfigurationKey.GATHER_ACTIVE_MARCH_QUEUE_INT);

		initializeChangeEvents();
	}

}
