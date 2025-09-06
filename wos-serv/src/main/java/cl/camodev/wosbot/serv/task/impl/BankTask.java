package cl.camodev.wosbot.serv.task.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import net.sourceforge.tess4j.TesseractException;

public class BankTask extends DelayedTask {

	private EmulatorManager emuManager = EmulatorManager.getInstance();

	public BankTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
		super(profile, tpTask);
	}

	@Override
	protected void execute() {
		int attempt = 0;
		int bankDelay = profile.getConfig(EnumConfigurationKey.INT_BANK_DELAY, Integer.class);

		while (attempt < 5) {
			// Check if we are on the home screen
			DTOImageSearchResult homeResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(),  90);
			DTOImageSearchResult worldResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 90);

			if (homeResult.isFound() || worldResult.isFound()) {
				if (navigateToBank()) {
					handleBankOperations(bankDelay);
					return;
				}
				return;
			} else {
				// If home screen is not found, log warning and go back
				logWarning("Home not found");
				EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
				sleepTask(2000);
			}
			attempt++;
		}

		// If menu is not found after 5 attempts, cancel the task
		if (attempt >= 5) {
			logWarning("Menu not found, removing task from scheduler");
			this.setRecurring(false);
		}
	}

	/**
	 * Navigates to the bank section in the game
	 *
	 * @return true if navigation was successful, false otherwise
	 */
	private boolean navigateToBank() {
		// Search for the Deals button
		DTOImageSearchResult dealsResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.HOME_DEALS_BUTTON.getTemplate(),  90);
		if (!dealsResult.isFound()) {
			logWarning("Deals button not found");
			return false;
		}

		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, dealsResult.getPoint(), dealsResult.getPoint());
		sleepTask(2000);
		emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(630, 143), new DTOPoint(2, 128));
		sleepTask(200);
		emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(630, 143), new DTOPoint(2, 128));
		sleepTask(200);
		// Search for the bank option within events
		DTOImageSearchResult bankResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.EVENTS_DEALS_BANK.getTemplate(), 90);
		if (!bankResult.isFound()) {
			logWarning("Bank option not found");
			return false;
		}

		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, bankResult.getPoint(), bankResult.getPoint());
		sleepTask(1000);

		logInfo("Successfully navigated to bank");
		return true;
	}

	/**
	 * Handles bank operations: check for ready deposits, withdraw if available, make
	 * new deposit
	 *
	 * @param bankDelay the delay configuration for bank deposits
	 */
	private void handleBankOperations(int bankDelay) {
		// STEP 1: Check if there's a deposit ready to withdraw
		DTOImageSearchResult withdrawAvailableResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.EVENTS_DEALS_BANK_WITHDRAW.getTemplate(),  90);

		if (withdrawAvailableResult.isFound()) {
			// Deposit is ready - withdraw it
			logInfo("Deposit ready for withdrawal");
			withdrawDeposit();

			// STEP 2: After withdrawal, make a new deposit
			makeNewDeposit(bankDelay);

		} else {
			// STEP 3: No deposit ready - check remaining time and reschedule
			logInfo("No deposit ready for withdrawal, checking remaining time");
			checkRemainingTimeAndReschedule();
		}

		// Navigate back
		emuManager.tapBackButton(EMULATOR_NUMBER);
	}

	/**
	 * Withdraws the ready deposit
	 */
	private void withdrawDeposit() {
		DTOImageSearchResult withdrawResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.EVENTS_DEALS_BANK_WITHDRAW.getTemplate(),  90);
		if (withdrawResult.isFound()) {
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, withdrawResult.getPoint(), withdrawResult.getPoint());
			sleepTask(1000);
			// Tap close/back button after withdrawal
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(670, 40), new DTOPoint(670, 40), 15, 100);
			sleepTask(1000);
			logInfo("Deposit successfully withdrawn");
		}
	}

	/**
	 * Makes a new deposit based on the configured delay
	 *
	 * @param bankDelay the delay configuration (1, 7, 15, 30 representing different deposit durations and search areas)
	 */
	private void makeNewDeposit(int bankDelay) {
		// Define search areas for the deposit template based on bankDelay values
		DTOPoint searchTopLeft;
		DTOPoint searchBottomRight;
		String depositType;
		int depositDays;

		switch (bankDelay) {
			case 1:
				// 1-day deposit area (top-left)
				searchTopLeft = new DTOPoint(50, 580);
				searchBottomRight = new DTOPoint(320 , 920 );
				depositType = "1-day";
				depositDays = 1;
				break;
			case 7:
				// 7-day deposit area (top-right)
				searchTopLeft = new DTOPoint(380, 580);
				searchBottomRight = new DTOPoint(670 , 920);
				depositType = "7-day";
				depositDays = 7;
				break;
			case 15:
				// 15-day deposit area (bottom-left)
				searchTopLeft = new DTOPoint(50, 900);
				searchBottomRight = new DTOPoint(340, 1250 );
				depositType = "15-day";
				depositDays = 15;
				break;
			case 30:
				// 30-day deposit area (bottom-right)
				searchTopLeft = new DTOPoint(380, 900);
				searchBottomRight = new DTOPoint(660 , 1250 );
				depositType = "30-day";
				depositDays = 30;
				break;
			default:
				logWarning("Invalid bank delay configuration: " + bankDelay + ". Valid values are 1, 7, 15, 30. Using 1-day deposit as fallback");
				searchTopLeft = new DTOPoint(50, 580);
				searchBottomRight = new DTOPoint(320 , 920 );
				depositType = "1-day (fallback)";
				depositDays = 1;
				break;
		}

		// Search for the deposit template in the specific area determined by bankDelay
		DTOImageSearchResult depositAvailableResult = emuManager.searchTemplate(
				EMULATOR_NUMBER,
				EnumTemplates.EVENTS_DEALS_BANK_DEPOSIT.getTemplate(),
				searchTopLeft,
				searchBottomRight,
				90
		);

		if (depositAvailableResult.isFound()) {
			// Tap on the found deposit option
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, depositAvailableResult.getPoint(), depositAvailableResult.getPoint());
			logInfo("Selected " + depositType + " deposit at: " + depositAvailableResult.getPoint());

			sleepTask(2000);
			// Confirm deposit
			emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(168, 762), new DTOPoint(477, 760));
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(410, 877), new DTOPoint(589, 919));

			// Schedule next check based on deposit duration
			LocalDateTime nextCheck = LocalDateTime.now().plusDays(depositDays);
			this.reschedule(nextCheck);

			logInfo("New " + depositType + " deposit created, next check scheduled for: " + nextCheck);
		} else {
			logWarning(depositType + " deposit option not available in search area [" +
					searchTopLeft.getX() + "," + searchTopLeft.getY() + " to " +
					searchBottomRight.getX() + "," + searchBottomRight.getY() + "]");
		}
	}

	/**
	 * Checks if there's an active deposit, reads remaining time, or makes a new deposit if none exists
	 */
	private void checkRemainingTimeAndReschedule() {
		// First, check if there's an active deposit using the template
		DTOImageSearchResult activeDepositResult = emuManager.searchTemplate(
				EMULATOR_NUMBER,
				EnumTemplates.EVENTS_DEALS_BANK_INDEPOSIT.getTemplate(),
				 90
		);

		if (activeDepositResult.isFound()) {
			// There's an active deposit - read the remaining time
			logInfo("Active deposit found, reading remaining time");
			emuManager.tapAtPoint(EMULATOR_NUMBER, activeDepositResult.getPoint());
			sleepTask(200);

			// Try OCR up to 5 times before fallback
			boolean ocrSuccess = false;
			String timeLeft = null;
			int maxOcrAttempts = 5;

			for (int attempt = 1; attempt <= maxOcrAttempts; attempt++) {
				try {
					logInfo("OCR attempt " + attempt + " of " + maxOcrAttempts);
					timeLeft = emuManager.ocrRegionText(EMULATOR_NUMBER, new DTOPoint(220, 770), new DTOPoint(490, 810));

					// Try to parse the time to validate it's a valid format
					LocalDateTime nextBank = parseAndAddToNow(timeLeft);
					this.reschedule(nextBank);

					logInfo("Deposit not ready, rescheduled for: " + nextBank + " (remaining: " + timeLeft + ")");
					ocrSuccess = true;
					break; // Success, exit the retry loop

				} catch (IOException | TesseractException e) {
					logWarning("OCR attempt " + attempt + " failed: " + e.getMessage());
					if (attempt < maxOcrAttempts) {
						// Wait a bit before retrying
						sleepTask(1000);
					}
				} catch (IllegalArgumentException e) {
					logWarning("OCR attempt " + attempt + " - invalid time format: " + e.getMessage() + " (text: '" + timeLeft + "')");
					if (attempt < maxOcrAttempts) {
						// Wait a bit before retrying
						sleepTask(1000);
					}
				}
			}

			// If all OCR attempts failed, use fallback
			if (!ocrSuccess) {
				logError("All " + maxOcrAttempts + " OCR attempts failed, using fallback schedule");
				LocalDateTime fallbackTime = LocalDateTime.now().plusHours(1);
				this.reschedule(fallbackTime);
				logWarning("Using fallback schedule: " + fallbackTime);
			}

		} else {
			// No active deposit found - make a new deposit
			logInfo("No active deposit found, creating new deposit");

			int bankDelay = profile.getConfig(EnumConfigurationKey.INT_BANK_DELAY, Integer.class);
			makeNewDeposit(bankDelay);
		}
	}

	public LocalDateTime parseAndAddToNow(String text) {
		// Regular expression to match the input format [n]d HH:mm:ss' o 'HH:mm:ss
		Pattern pattern = Pattern.compile("(?i).*?(?:(\\d+)\\s*d\\D*)?(\\d{1,2}:\\d{2}:\\d{2}).*", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(text.trim());

		if (!matcher.matches()) {
			throw new IllegalArgumentException("Input does not match the expected format. Expected format: [n]d HH:mm:ss' o 'HH:mm:ss");
		}

		String daysStr = matcher.group(1);
		String timeStr = matcher.group(2);

		int daysToAdd = (daysStr != null) ? Integer.parseInt(daysStr) : 0;

		// parser for time part
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm:ss");
		LocalTime timePart = LocalTime.parse(timeStr, timeFormatter);


		return LocalDateTime.now()
				.plusDays(daysToAdd)
				.plusHours(timePart.getHour())
				.plusMinutes(timePart.getMinute())
				.plusSeconds(timePart.getSecond());
	}
}
