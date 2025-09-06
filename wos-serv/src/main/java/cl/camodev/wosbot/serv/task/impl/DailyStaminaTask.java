package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.utiles.UtilTime;
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

public class DailyStaminaTask extends DelayedTask {

	private final EmulatorManager emuManager = EmulatorManager.getInstance();
	private final ServLogs servLogs = ServLogs.getServices();
	private final ServScheduler scheduler = ServScheduler.getServices();

	public DailyStaminaTask(DTOProfiles profile, TpDailyTaskEnum tpDailyTask) {
		super(profile, tpDailyTask);
	}

	@Override
	protected void execute() {
		DTOImageSearchResult homeResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(),  90);
		DTOImageSearchResult worldResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(),  90);

		if (homeResult.isFound() || worldResult.isFound()) {
			if (worldResult.isFound()) {
				emuManager.tapAtPoint(EMULATOR_NUMBER, worldResult.getPoint());
				sleepTask(3000);
				servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Going to storehouse");
			}

			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(3, 513), new DTOPoint(26, 588));
			sleepTask(1000);

			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(20, 250), new DTOPoint(200, 280));
			sleepTask(500);

			DTOImageSearchResult researchCenter = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_SHORTCUTS_RESEARCH_CENTER.getTemplate(), 90);

			if (researchCenter.isFound()) {
				{
					emuManager.tapAtRandomPoint(EMULATOR_NUMBER, researchCenter.getPoint(), researchCenter.getPoint());
					sleepTask(2000);
					emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(30, 430), new DTOPoint(50, 470));
					sleepTask(500);

					DTOImageSearchResult chest = null;
					System.out.println("Searching for chest");
					servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Searching for chest");
					for (int i = 0; i < 5; i++) {
						chest = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.STOREHOUSE_CHEST.getTemplate(),  90);

						if (chest.isFound()) {
							// debo obtener la recompensa y verificar caundo es la proxima via ocr
							System.out.println("Chest found, tapping");
							servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Claiming chest");
							emuManager.tapAtRandomPoint(EMULATOR_NUMBER, chest.getPoint(), chest.getPoint());
							sleepTask(500);

							emuManager.tapBackButton(EMULATOR_NUMBER);
							for (int j = 0; j < 5; j++) {
								DTOImageSearchResult stamina = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.STOREHOUSE_STAMINA.getTemplate(),  90);

								if (stamina.isFound()) {
									servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Claiming stamina");
									emuManager.tapAtRandomPoint(EMULATOR_NUMBER, stamina.getPoint(), stamina.getPoint());
									sleepTask(500);
									emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(250, 930), new DTOPoint(450, 950));
									sleepTask(500);
									break;
								} else {
									System.out.println("Stamina not found, sleeping");
									sleepTask(100);
								}
							}

							break;
						} else {
							// debo verrificar solo depues de los 5 intento si exsite la stamina, si no hacer ocr para verificar el proximo reward
							System.out.println("Chest not found, sleeping");
							sleepTask(100);
						}

					}

					if (!chest.isFound()) {
						System.out.println("Chest not found, verifying stamina");
						for (int i = 0; i < 5; i++) {
							DTOImageSearchResult stamina = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.STOREHOUSE_STAMINA.getTemplate(), 90);

							if (stamina.isFound()) {
								servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Claiming stamina");
								emuManager.tapAtRandomPoint(EMULATOR_NUMBER, stamina.getPoint(), stamina.getPoint());
								sleepTask(500);
								emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(250, 930), new DTOPoint(450, 950));
								sleepTask(500);
								break;
							} else {
								System.out.println("Stamina not found, sleeping");
								sleepTask(100);
							}
						}
					}

					this.reschedule(UtilTime.getNextReset());
					scheduler.updateDailyTaskStatus(profile, tpTask, scheduledTime);

				}
			}

		} else {
			emuManager.tapBackButton(EMULATOR_NUMBER);
		}
	}

}
