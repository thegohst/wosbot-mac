package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.utiles.UtilTime;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class LifeEssenceCaringTask extends DelayedTask {


	public LifeEssenceCaringTask(DTOProfiles profile, TpDailyTaskEnum dailyMission) {
		super(profile, dailyMission);
	}

	// i should go to essence tree, check if there's daily attempt available, if not, reschedule till daily reset,
	@Override
	protected void execute() {

		logInfo( "going life essence");
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(1, 509), new DTOPoint(24, 592));
		// asegurarse de esta en el shortcut de ciudad
		sleepTask(2000);
		emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(110, 270));
		sleepTask(1000);

		// hacer swipe hacia abajo
		emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(220, 845), new DTOPoint(220, 94));
		sleepTask(1000);
		DTOImageSearchResult lifeEssenceMenu = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.LIFE_ESSENCE_MENU.getTemplate(),  90);

		if (lifeEssenceMenu.isFound()) {
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, lifeEssenceMenu.getPoint(), lifeEssenceMenu.getPoint());
			sleepTask(3000);
			emuManager.tapBackButton(EMULATOR_NUMBER);
			emuManager.tapBackButton(EMULATOR_NUMBER);
			sleepTask(500);
			servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Going to check if there's daily attempts available");
			emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(670, 100));
			sleepTask(2000);

			DTOImageSearchResult dailyAttempt = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.LIFE_ESSENCE_DAILY_CARING_AVAILABLE.getTemplate(), 90);
			if (dailyAttempt.isFound()) {
				logInfo( "Daily attempt available, proceeding with caring");

				// bebo buscar y scrollear unas 7-8 veces, si no encuentro, reschedule de una hora

				for (int i = 0; i < 8; i++) {
					DTOImageSearchResult caringAvailable = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.LIFE_ESSENCE_DAILY_CARING_GOTO_ISLAND.getTemplate(), 90);
					if (caringAvailable.isFound()) {
						emuManager.tapAtRandomPoint(EMULATOR_NUMBER, caringAvailable.getPoint(), caringAvailable.getPoint());
						sleepTask(5000);
						logInfo( "Caring available, proceeding with caring");
						// buscar el boton de caring, debo buscañpr un par de veces debido al movimiento

						for (int j = 0; j < 3; j++) {
							DTOImageSearchResult caringButton = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.LIFE_ESSENCE_DAILY_CARING_BUTTON.getTemplate(),  90);
							if (caringButton.isFound()) {
								emuManager.tapAtRandomPoint(EMULATOR_NUMBER, caringButton.getPoint(), caringButton.getPoint());
								sleepTask(5000);
								servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Caring done successfully");
								emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(42, 28));
								sleepTask(3000);
								emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(42, 28));
								return;
							}
						}

						return;
					} else {
						servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Caring not found, scrolling down");
						emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(350, 1100), new DTOPoint(350, 670));
						sleepTask(2000);
					}
				}

				this.reschedule(LocalDateTime.now().plusMinutes(profile.getConfig(EnumConfigurationKey.ALLIANCE_LIFE_ESSENCE_OFFSET_INT, Integer.class)));
				logInfo("No caring available after multiple attempts");

				emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(42, 28));
				sleepTask(3000);
				emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(42, 28));

			} else {
				servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "No daily attempts available, rescheduling for next day");
				this.reschedule(UtilTime.getGameReset());
				servScheduler.updateDailyTaskStatus(profile, tpTask, UtilTime.getGameReset());
				emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(42, 28));
				sleepTask(3000);
				emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(42, 28));
			}
		}

	}

}
