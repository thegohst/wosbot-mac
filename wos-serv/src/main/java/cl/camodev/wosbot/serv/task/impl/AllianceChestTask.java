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

public class AllianceChestTask extends DelayedTask {

	public AllianceChestTask(DTOProfiles profile, TpDailyTaskEnum heroRecruitment) {
		super(profile, heroRecruitment);
	}

	@Override
	protected void execute() {

		logInfo("Going to alliance chest");

		// Ir a la sección de cofres de alianza
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(493, 1187), new DTOPoint(561, 1240));
		sleepTask(3000);

		DTOImageSearchResult allianceChestResult = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.ALLIANCE_CHEST_BUTTON.getTemplate(),  90);
		if (!allianceChestResult.isFound()) {

			LocalDateTime nextExecutionTime = LocalDateTime.now().plusMinutes(profile.getConfig(EnumConfigurationKey.ALLIANCE_CHESTS_OFFSET_INT, Integer.class));
			this.reschedule(nextExecutionTime);
			return;
		}

		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, allianceChestResult.getPoint(), allianceChestResult.getPoint());
		sleepTask(500);

		// Abrir el cofre
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(56, 375), new DTOPoint(320, 420));
		sleepTask(500);

		// Buscar el botón de reclamar recompensas
		DTOImageSearchResult claimButton = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.ALLIANCE_CHEST_LOOT_CLAIM_BUTTON.getTemplate(),  90);
		if (claimButton.isFound()) {
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, claimButton.getPoint(), claimButton.getPoint(), 10, 100);
			sleepTask(500);
		}

		// Confirmar la acción
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(410, 375), new DTOPoint(626, 420));
		sleepTask(500);

		// Cerrar la ventana
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(578, 1180), new DTOPoint(641, 1200), 10, 300);

		emuManager.tapBackButton(EMULATOR_NUMBER);
		emuManager.tapBackButton(EMULATOR_NUMBER);


		LocalDateTime nextExecutionTime = LocalDateTime.now().plusMinutes(profile.getConfig(EnumConfigurationKey.ALLIANCE_CHESTS_OFFSET_INT, Integer.class));
		this.reschedule(nextExecutionTime);
	}
}