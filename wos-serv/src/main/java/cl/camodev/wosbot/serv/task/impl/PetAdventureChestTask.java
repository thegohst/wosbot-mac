package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class PetAdventureChestTask extends DelayedTask {

	private final EmulatorManager emuManager = EmulatorManager.getInstance();
	private final ServLogs servLogs = ServLogs.getServices();

	private int attempts = 0;

	public PetAdventureChestTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
		super(profile, tpTask);
	}

	@Override
	protected void execute() {
		if (attempts >= 3) {
			servLogs.appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Menu not found, removing task from scheduler");
			this.setRecurring(false);
			return;
		}

		DTOImageSearchResult homeResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 90);
		DTOImageSearchResult worldResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 90);
		if (homeResult.isFound() || worldResult.isFound()) {
			servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "going pet skills");

			DTOImageSearchResult petsResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_PETS.getTemplate(),  90);
			if (petsResult.isFound()) {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "button pets found, taping");
				emuManager.tapAtRandomPoint(EMULATOR_NUMBER, petsResult.getPoint(), petsResult.getPoint());
				sleepTask(3000);

				DTOImageSearchResult beastCageResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.PETS_BEAST_CAGE.getTemplate(), 90);
				if (beastCageResult.isFound()) {
					emuManager.tapAtPoint(EMULATOR_NUMBER, beastCageResult.getPoint());
					sleepTask(500);
					emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(547, 1150), new DTOPoint(650, 1210));

					for (int i = 0; i < 10; i++) {
						servLogs.appendLog(EnumTpMessageSeverity.DEBUG, taskName, profile.getName(), "Searching completed chest");
						DTOImageSearchResult doneChest = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.PETS_CHEST_COMPLETED.getTemplate(), 90);
						if (doneChest.isFound()) {
							emuManager.tapAtRandomPoint(EMULATOR_NUMBER, doneChest.getPoint(), doneChest.getPoint());
							sleepTask(500);
							emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(270, 735), new DTOPoint(450, 760), 20, 100);

							DTOImageSearchResult share = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.PETS_CHEST_SHARE.getTemplate(),  90);
							if (share.isFound()) {
								servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Sharing chest");
								emuManager.tapAtRandomPoint(EMULATOR_NUMBER, share.getPoint(), share.getPoint());
								sleepTask(500);
							}
							emuManager.tapBackButton(EMULATOR_NUMBER);
							sleepTask(500);
						}
					}

					List<EnumTemplates> chests = List.of(EnumTemplates.PETS_CHEST_RED, EnumTemplates.PETS_CHEST_PURPLE, EnumTemplates.PETS_CHEST_BLUE);

					boolean foundAnyChest; // Variable de control

					do {
						foundAnyChest = false; // Reiniciar en cada iteración

						for (EnumTemplates enumTemplates : chests) {
							for (int attempt = 0; attempt < 5; attempt++) {
								ServLogs.getServices().appendLog(EnumTpMessageSeverity.DEBUG, taskName, profile.getName(), "Searching for " + enumTemplates + " attempt " + attempt);

								DTOImageSearchResult result = emuManager.searchTemplate(EMULATOR_NUMBER, enumTemplates.getTemplate(),  90);
								if (result.isFound()) {
									foundAnyChest = true; // Se encontró un cofre, el bucle se repetirá

									servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Found: " + enumTemplates);

									emuManager.tapAtRandomPoint(EMULATOR_NUMBER, result.getPoint(), result.getPoint());
									sleepTask(500);

									DTOImageSearchResult chestSelect = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.PETS_CHEST_SELECT.getTemplate(),  90);

									if (chestSelect.isFound()) {
										emuManager.tapAtPoint(EMULATOR_NUMBER, chestSelect.getPoint());
										sleepTask(500);

										DTOImageSearchResult chestStart = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.PETS_CHEST_START.getTemplate(),  90);

										if (chestStart.isFound()) {
											emuManager.tapAtPoint(EMULATOR_NUMBER, chestStart.getPoint());
											sleepTask(500);

											emuManager.tapBackButton(EMULATOR_NUMBER);
											sleepTask(500);
											break; // Sale del intento, pero no del ciclo principal
										} else {
											DTOImageSearchResult attempts = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.PETS_CHEST_ATTEMPT.getTemplate(),  90);
											if (attempts.isFound()) {
												servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "No more attempts");
												this.reschedule(UtilTime.getGameReset());
												ServScheduler.getServices().updateDailyTaskStatus(profile, tpTask, UtilTime.getGameReset());
												emuManager.tapBackButton(EMULATOR_NUMBER);
												emuManager.tapBackButton(EMULATOR_NUMBER);
												emuManager.tapBackButton(EMULATOR_NUMBER);
												emuManager.tapBackButton(EMULATOR_NUMBER);
												return;
											}
										}
									}
								}
							}
						}

						if (foundAnyChest) {
							servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "At least one chest was found. Restarting search...");
							sleepTask(500); // Espera 5 segundos antes de repetir
						}

					} while (foundAnyChest); // El bucle se repite hasta que no se encuentren más cofres

					servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "No chests found");
					ServScheduler.getServices().updateDailyTaskStatus(profile, tpTask, LocalDateTime.now().plusHours(2));
					this.reschedule(LocalDateTime.now().plusHours(2));
					emuManager.tapBackButton(EMULATOR_NUMBER);
					emuManager.tapBackButton(EMULATOR_NUMBER);

				}

			} else {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "button pets not found retrying later");
				attempts++;
			}

		} else

		{
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Home not found");
			emuManager.tapBackButton(EMULATOR_NUMBER);

		}

	}

	public Integer extractRemainingAttempts(String ocrText) {
		if (ocrText == null || ocrText.isEmpty()) {
			return null; // Manejo de casos nulos o vacíos
		}

		// Normalizar texto para reducir errores OCR
		String normalizedText = ocrText.replaceAll("[^a-zA-Z0-9: ]", "").trim();

		// Expresión regular para buscar un número después de "attempts"
		Pattern pattern = Pattern.compile("(?i)attempts.*?\\b(\\d+)\\b");
		Matcher matcher = pattern.matcher(normalizedText);

		if (matcher.find()) {
			try {
				return Integer.parseInt(matcher.group(1));
			} catch (NumberFormatException e) {
				return null; // Retorna null si el número no se puede parsear
			}
		}

		return null; // Retorna null si no se encuentra el número
	}

	@Override
	public boolean provideDailyMissionProgress() {return true;}

}
