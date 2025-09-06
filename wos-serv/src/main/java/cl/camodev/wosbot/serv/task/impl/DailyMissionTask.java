package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class DailyMissionTask extends DelayedTask {

	public DailyMissionTask(DTOProfiles profile, TpDailyTaskEnum dailyMission) {
		super(profile, dailyMission);
	}

	@Override
	protected void execute() {

		logInfo("Going to Daily Mission Tab");

		emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(50, 1050));
		sleepTask(2000);

		DTOImageSearchResult result = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.DAILY_MISSION_DAILY_TAB.getTemplate(),  90);

		if (result.isFound()) {
			servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(),
					"Switching to Daily Mission Tab");
			emuManager.tapAtPoint(EMULATOR_NUMBER, result.getPoint());
			sleepTask(500);
		}

		logInfo("Searching for Claim All button");
		result = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.DAILY_MISSION_CLAIMALL_BUTTON.getTemplate(),
				90);

		if (result.isFound()) {
			logInfo("Claiming Daily Mission Reward");
			emuManager.tapAtPoint(EMULATOR_NUMBER, result.getPoint());
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(10, 100), new DTOPoint(600, 120), 20, 50);
		} else {
			logWarning("Claim All button not found, trying to each mission");
			while ((result = emuManager.searchTemplate(EMULATOR_NUMBER,
					EnumTemplates.DAILY_MISSION_CLAIM_BUTTON.getTemplate(), 90)).isFound()) {

				logInfo("Claim button found, tapping on it");

				emuManager.tapAtPoint(EMULATOR_NUMBER, result.getPoint());
				emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(10, 100), new DTOPoint(600, 120), 10, 50);
				sleepTask(500);
			}
		}
		emuManager.tapBackButton(EMULATOR_NUMBER);
		sleepTask(50);

		this.setRecurring(!profile.getConfig(EnumConfigurationKey.DAILY_MISSION_AUTO_SCHEDULE_BOOL,Boolean.class));

		if (recurring){
			LocalDateTime nextSchedule = LocalDateTime.now().plusMinutes(profile.getConfig(EnumConfigurationKey.DAILY_MISSION_OFFSET_INT, Integer.class));
			this.reschedule(nextSchedule);
		}else{
			this.reschedule(LocalDateTime.now().plusMinutes(30));
		}


	}

}
