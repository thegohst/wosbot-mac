package cl.camodev.wosbot.common.view;

import java.util.HashMap;
import java.util.Map;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.profile.model.IProfileChangeObserver;
import cl.camodev.wosbot.profile.model.IProfileLoadListener;
import cl.camodev.wosbot.profile.model.IProfileObserverInjectable;
import cl.camodev.wosbot.profile.model.ProfileAux;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

public abstract class AbstractProfileController implements IProfileLoadListener, IProfileObserverInjectable {

	protected final Map<CheckBox, EnumConfigurationKey> checkBoxMappings = new HashMap<>();
	protected final Map<TextField, EnumConfigurationKey> textFieldMappings = new HashMap<>();
	protected final Map<RadioButton, EnumConfigurationKey> radioButtonMappings = new HashMap<>();
	protected final Map<ComboBox<Integer>, EnumConfigurationKey> comboBoxMappings = new HashMap<>();
	protected IProfileChangeObserver profileObserver;
	protected boolean isLoadingProfile = false;

	@Override
	public void setProfileObserver(IProfileChangeObserver observer) {
		this.profileObserver = observer;
	}
	protected void initializeChangeEvents() {
		checkBoxMappings.forEach(this::setupCheckBoxListener);
		textFieldMappings.forEach(this::setupTextFieldUpdateOnFocusOrEnter);
		radioButtonMappings.forEach(this::setupRadioButtonListener);
		comboBoxMappings.forEach(this::setupComboBoxListener);
	}

	protected void createToggleGroup(RadioButton... radioButtons) {
		ToggleGroup toggleGroup = new ToggleGroup();
		for (RadioButton radioButton : radioButtons) {
			radioButton.setToggleGroup(toggleGroup);
		}
	}

	protected void setupRadioButtonListener(RadioButton radioButton, EnumConfigurationKey configKey) {
		radioButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
			if (!isLoadingProfile) {
				profileObserver.notifyProfileChange(configKey, newVal);
			}
		});
	}

	protected void setupCheckBoxListener(CheckBox checkBox, EnumConfigurationKey configKey) {
		checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
			if (!isLoadingProfile) {
				profileObserver.notifyProfileChange(configKey, newVal);
			}
		});
	}

	protected void setupTextFieldUpdateOnFocusOrEnter(TextField textField, EnumConfigurationKey configKey) {
		textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
			if (!isNowFocused && !isLoadingProfile) {
				updateProfile(textField, configKey);
			}
		});

		textField.setOnAction(event -> {
			if (!isLoadingProfile) {
				updateProfile(textField, configKey);
			}
		});
	}

	protected void setupComboBoxListener(ComboBox<Integer> comboBox, EnumConfigurationKey configKey) {
		comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
			if (!isLoadingProfile && newVal != null) {
				profileObserver.notifyProfileChange(configKey, newVal);
			}
		});
	}

	private void updateProfile(TextField textField, EnumConfigurationKey configKey) {
		String newVal = textField.getText();
		if (isValidPositiveInteger(newVal)) {
			profileObserver.notifyProfileChange(configKey, Integer.valueOf(newVal));
		} else {
			textField.setText(configKey.getDefaultValue());
		}
	}

	private boolean isValidPositiveInteger(String value) {
		if (value == null || value.isEmpty()) {
			return false;
		}
		try {
			int number = Integer.parseInt(value);
			return number >= 0 && number <= 999;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public void onProfileLoad(ProfileAux profile) {
		isLoadingProfile = true;
		try {
			checkBoxMappings.forEach((checkBox, key) -> {
				Boolean value = profile.getConfig(key, Boolean.class);
				checkBox.setSelected(value != null && value);
			});

			textFieldMappings.forEach((textField, key) -> {
				Integer value = profile.getConfig(key, Integer.class);
				textField.setText(value != null ? String.valueOf(value) : key.getDefaultValue());
			});

			radioButtonMappings.forEach((radioButton, key) -> {
				Boolean value = profile.getConfig(key, Boolean.class);
				radioButton.setSelected(value != null && value);
			});			comboBoxMappings.forEach((comboBox, key) -> {
				Integer value = profile.getConfig(key, Integer.class);
				comboBox.setValue(value != null ? value : Integer.valueOf(key.getDefaultValue()));
			});
		} finally {
			isLoadingProfile = false;
		}
	}
}
