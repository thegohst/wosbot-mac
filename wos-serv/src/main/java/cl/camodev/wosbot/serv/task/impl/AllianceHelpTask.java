package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class AllianceHelpTask extends DelayedTask {

	public AllianceHelpTask(DTOProfiles profile, TpDailyTaskEnum tpDailyTask) {
		super(profile, tpDailyTask);
	}

	@Override
	protected void execute() {

		logInfo("Going to alliance help");

		// Ir a la sección de cofres de alianza
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(493, 1187), new DTOPoint(561, 1240));
		sleepTask(3000);

		DTOImageSearchResult allianceChestResult = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.ALLIANCE_HELP_BUTTON.getTemplate(),  90);
		if (!allianceChestResult.isFound()) {
			logWarning("Alliance help button not found");
			rescheduleTask();
			return;
		}
		logInfo("Alliance help button found");
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, allianceChestResult.getPoint(), allianceChestResult.getPoint());

		sleepTask(500);

		DTOImageSearchResult allianceHelpResult = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.ALLIANCE_HELP_REQUESTS.getTemplate(),  90);
		if (allianceHelpResult.isFound()) {
			logInfo("Helping alliance members");
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, allianceHelpResult.getPoint(), allianceHelpResult.getPoint());

		} else {
			logWarning("Alliance help requests not found");
		}
		emuManager.tapBackButton(EMULATOR_NUMBER);
		sleepTask(100);
		emuManager.tapBackButton(EMULATOR_NUMBER);

		rescheduleTask();
	}

	/**
	 * Obtiene el tiempo de reprogramación y actualiza la tarea.
	 */
	private void rescheduleTask() {

		LocalDateTime nextExecutionTime = LocalDateTime.now().plusMinutes(profile.getConfig(EnumConfigurationKey.ALLIANCE_HELP_REQUESTS_OFFSET_INT, Integer.class));
		this.reschedule(nextExecutionTime);
	}

	@Override
	public boolean provideDailyMissionProgress() {return true;}

}
