package cl.camodev.wosbot.intel.view;

import cl.camodev.wosbot.common.view.AbstractProfileController;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

public class IntelLayoutController extends AbstractProfileController {

	@FXML
	private CheckBox checkBoxBeast;

	@FXML
	private CheckBox checkBoxFireBeast;

	@FXML
	private CheckBox checkBoxIntel;

	@FXML
	private CheckBox checkBoxJourney;

	@FXML
	private CheckBox checkBoxSurvivors;

	@FXML
	private CheckBox checkBoxFireCrystalEra;

	@FXML
	private void initialize() {
		checkBoxMappings.put(checkBoxIntel, EnumConfigurationKey.INTEL_BOOL);
		checkBoxMappings.put(checkBoxFireBeast, EnumConfigurationKey.INTEL_FIRE_BEAST_BOOL);
		checkBoxMappings.put(checkBoxBeast, EnumConfigurationKey.INTEL_BEASTS_BOOL);
		checkBoxMappings.put(checkBoxJourney, EnumConfigurationKey.INTEL_EXPLORATION_BOOL);
		checkBoxMappings.put(checkBoxSurvivors, EnumConfigurationKey.INTEL_CAMP_BOOL);
		checkBoxMappings.put(checkBoxFireCrystalEra, EnumConfigurationKey.INTEL_FC_ERA_BOOL);
		initializeChangeEvents();

	}

}
