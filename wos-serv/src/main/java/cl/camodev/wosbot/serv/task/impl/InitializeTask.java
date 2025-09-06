package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ex.ProfileInReconnectStateException;
import cl.camodev.wosbot.ex.StopExecutionException;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class InitializeTask extends DelayedTask {
	boolean isStarted = false;

	public InitializeTask(DTOProfiles profile, TpDailyTaskEnum tpDailyTask) {
		super(profile, tpDailyTask);
	}

	@Override
	protected void execute() {
		this.setRecurring(false);
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Checking emulator status");
		
		// Phase 1: Wait for emulator to be running
		while (!isStarted) {
			if (EmulatorManager.getInstance().isRunning(EMULATOR_NUMBER)) {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Emulator detected, checking readiness...");
				isStarted = true;
			} else {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Emulator not found, trying to start it");
				EmulatorManager.getInstance().launchEmulator(EMULATOR_NUMBER);
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Waiting 10 seconds before checking again");
				sleepTask(10000);
			}
		}
		
		// Phase 2: Wait for emulator to be fully ready and responsive
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Waiting for emulator to be fully ready...");
		boolean emulatorReady = false;
		int readinessAttempts = 0;
		final int MAX_READINESS_ATTEMPTS = 30; // 5 minutes max wait
		
		while (!emulatorReady && readinessAttempts < MAX_READINESS_ATTEMPTS) {
			if (EmulatorManager.getInstance().isEmulatorReady(EMULATOR_NUMBER)) {
				emulatorReady = true;
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Emulator is fully ready and responsive!");
			} else {
				readinessAttempts++;
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), 
					String.format("Emulator not ready yet (attempt %d/%d), waiting 10 seconds...", readinessAttempts, MAX_READINESS_ATTEMPTS));
				sleepTask(10000);
			}
		}
		
		if (!emulatorReady) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.ERROR, taskName, profile.getName(), 
				"Emulator did not become ready after maximum wait time. Restarting emulator...");
			EmulatorManager.getInstance().closeEmulator(EMULATOR_NUMBER);
			isStarted = false;
			this.setRecurring(true);
			return;
		}
		
		// Phase 3: Additional 30-second grace period
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), 
			"Emulator ready! Waiting additional 30 seconds for full stability...");
		sleepTask(30000);

		// Phase 4: Check if game is installed and launch it
		if (!EmulatorManager.getInstance().isWhiteoutSurvivalInstalled(EMULATOR_NUMBER)) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.ERROR, taskName, profile.getName(), "Whiteout Survival not installed, stopping queue");
			throw new StopExecutionException("Game not installed");
		} else {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), 
				"Whiteout Survival detected! Launching game now...");
			EmulatorManager.getInstance().launchApp(EMULATOR_NUMBER, EmulatorManager.WHITEOUT_PACKAGE);
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), 
				"Game launch command sent. Waiting 10 seconds for game to start...");
			sleepTask(10000);

			final int MAX_ATTEMPTS = 10;
			final int WAIT_TIME = 5000;

			boolean homeScreen = false;
			int attempts = 0;
			while (attempts <= MAX_ATTEMPTS) {
				DTOImageSearchResult home = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 90);
				DTOImageSearchResult world = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 90);

				if (home.isFound() || world.isFound()) {
					homeScreen = true;
					ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "home screen found");
					break;
				}

				DTOImageSearchResult reconnect = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_RECONNECT.getTemplate(), 90);
				if (reconnect.isFound()) {
					throw new ProfileInReconnectStateException("Profile " + profile.getName() + " is in reconnect state, cannot execute task: " + taskName);
				}

				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "screen not found, esperando 5 segundos antes de volver a intentar");
				EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
				sleepTask(WAIT_TIME);
				attempts++;
			}

			if (!homeScreen) {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.ERROR, taskName, profile.getName(), "screen no encontrado tras varios intentos, reiniciando emulador");
				EmulatorManager.getInstance().closeEmulator(EMULATOR_NUMBER);
				isStarted = false;
				this.setRecurring(true);
			}

		}
	}


}