package cl.camodev.wosbot.city.view;

import cl.camodev.wosbot.common.view.AbstractProfileController;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

public class CityEventsLayoutController extends AbstractProfileController {

	@FXML
	private CheckBox checkBoxCrystalLabFC;

	@FXML
	private CheckBox checkBoxDailyVipRewards;

	@FXML
	private CheckBox checkBoxExplorationChest;

	@FXML
	private CheckBox checkBoxHeroRecruitment;

	@FXML
	private CheckBox checkBoxMailRewards;

	@FXML
	private CheckBox checkBoxLifeEssence;

	@FXML
	private CheckBox checkBoxDailyMission, checkboxAutoScheduleDailyMission;

	@FXML
	private CheckBox checkBoxDailyLabyrinth;

	@FXML
	private RadioButton radioButtonStorehouseChest;

	@FXML
	private RadioButton radioButtonStorehouseStamina;

	@FXML
	private CheckBox checkBoxWarAcademyShards;

	@FXML
	private CheckBox checkBoxBuyMonthlyVip;

	@FXML
	private TextField textfieldExplorationOffset;

	@FXML
	private TextField textfieldMailOffset;

	@FXML
	private TextField textfieldLifeEssenceOffset;

	@FXML
	private TextField textfieldDailyMissionOffset;

	@FXML
	private void initialize() {

		checkBoxMappings.put(checkBoxCrystalLabFC, EnumConfigurationKey.BOOL_CRYSTAL_LAB_FC);
		checkBoxMappings.put(checkBoxExplorationChest, EnumConfigurationKey.BOOL_EXPLORATION_CHEST);
		checkBoxMappings.put(checkBoxHeroRecruitment, EnumConfigurationKey.BOOL_HERO_RECRUITMENT);
		checkBoxMappings.put(checkBoxWarAcademyShards, EnumConfigurationKey.BOOL_WAR_ACADEMY_SHARDS);
		checkBoxMappings.put(checkBoxDailyVipRewards, EnumConfigurationKey.BOOL_VIP_POINTS);
		checkBoxMappings.put(checkBoxMailRewards, EnumConfigurationKey.MAIL_REWARDS_BOOL);
		checkBoxMappings.put(checkBoxBuyMonthlyVip, EnumConfigurationKey.VIP_BUY_MONTHLY);
		checkBoxMappings.put(checkBoxLifeEssence, EnumConfigurationKey.LIFE_ESSENCE_BOOL);
		checkBoxMappings.put(checkBoxDailyMission, EnumConfigurationKey.DAILY_MISSION_BOOL);
		checkBoxMappings.put(checkboxAutoScheduleDailyMission, EnumConfigurationKey.DAILY_MISSION_AUTO_SCHEDULE_BOOL);
		checkBoxMappings.put(checkBoxDailyLabyrinth, EnumConfigurationKey.DAILY_LABYRINTH_BOOL);

		textFieldMappings.put(textfieldExplorationOffset, EnumConfigurationKey.INT_EXPLORATION_CHEST_OFFSET);
		textFieldMappings.put(textfieldMailOffset, EnumConfigurationKey.MAIL_REWARDS_OFFSET_INT);
		textFieldMappings.put(textfieldLifeEssenceOffset, EnumConfigurationKey.LIFE_ESSENCE_OFFSET_INT);
		textFieldMappings.put(textfieldDailyMissionOffset, EnumConfigurationKey.DAILY_MISSION_OFFSET_INT);

		radioButtonMappings.put(radioButtonStorehouseChest, EnumConfigurationKey.STOREHOUSE_CHEST_BOOL);
		radioButtonMappings.put(radioButtonStorehouseStamina, EnumConfigurationKey.STOREHOUSE_STAMINA_BOOL);

		createToggleGroup(radioButtonStorehouseChest, radioButtonStorehouseStamina);

		initializeChangeEvents();
	}
}
