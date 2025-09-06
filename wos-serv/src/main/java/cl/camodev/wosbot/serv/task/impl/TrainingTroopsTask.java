package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.task.DelayedTask;
import cl.camodev.wosbot.serv.task.EnumStartLocation;
import net.sourceforge.tess4j.TesseractException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Task responsible for managing troop training operations in the game.
 * Handles both normal training and promotion-priority training modes.
 * Supports Infantry, Lancer, and Marksman troop types.
 */
public class TrainingTroopsTask extends DelayedTask {

    // ===============================
    // CONSTANTS AND FIELDS
    // ===============================

    private final TroopType troopType;

    // ===============================
    // CONSTRUCTOR
    // ===============================

    public TrainingTroopsTask(DTOProfiles profile, TpDailyTaskEnum heroRecruitment, TroopType troopType) {
        super(profile, heroRecruitment);
        this.troopType = troopType;
    }

    // ===============================
    // MAIN EXECUTION METHODS
    // ===============================

    /**
     * Parse time string and add it to the current LocalDateTime
     * @param baseTime Base time to add to (unused but kept for method signature compatibility)
     * @param timeString Time string in format "[n]d HH:mm:ss"
     * @return LocalDateTime with the parsed time added
     */
    public static LocalDateTime addTimeToLocalDateTime(LocalDateTime baseTime, String timeString) {
        Pattern pattern = Pattern.compile("(?i).*?(?:(\\d+)\\s*d\\s*)?(\\d{1,2}:\\d{2}:\\d{2}).*", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(timeString.trim());

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Time string does not match expected format [n]d HH:mm:ss: " + timeString);
        }

        String daysStr = matcher.group(1);   // Optional days component
        String timeStr = matcher.group(2);   // Required time component

        int daysToAdd = (daysStr != null) ? Integer.parseInt(daysStr) : 0;

        // Parse time component
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm:ss");
        LocalTime timePart = LocalTime.parse(timeStr, timeFormatter);

        return LocalDateTime.now()
                .plusDays(daysToAdd)
                .plusHours(timePart.getHour())
                .plusMinutes(timePart.getMinute())
                .plusSeconds(timePart.getSecond());
    }

    @Override
    protected void execute() {
        logInfo("Starting training execution for troop type: " + troopType);

        // Navigate to troops interface
        ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(),
                "Navigating to training interface for " + troopType);

        // Tap on troops menu
        emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(3, 513), new DTOPoint(26, 588));
        sleepTask(1000);

        // Tap on training tab
        emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(110, 270));
        sleepTask(500);

        // Search for the specific troop type
        DTOImageSearchResult troopsResult = emuManager.searchTemplate(EMULATOR_NUMBER, troopType.getTemplate(), 90);

        if (troopsResult.isFound()) {
            handleTroopStatusCheck(troopsResult);
        } else {
            logInfo("Troop type " + troopType + " not found, ending execution");
        }
    }



    /**
     * Handle checking the status of troops and determine next action
     * @param troopsResult The search result for the troop type
     */
    private void handleTroopStatusCheck(DTOImageSearchResult troopsResult) {
        // Attempt multiple times to read troop status (OCR can be unreliable)
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                logInfo("Attempting to read troop status - Attempt " + attempt);

                DTOPoint[] points = getTroopsPoints(troopType);
                String rawText = emuManager.ocrRegionText(EMULATOR_NUMBER, points[0], points[1]);

                if (handleTroopStatus(rawText, troopsResult)) {
                    return; // Status handled successfully, exit
                }

                // If none of the status conditions were met, try manual attempt
                logInfo("Manual attempt - tapping on troop and reopening interface");

                // Tap on the troop first
                EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER,
                    troopsResult.getPoint(), troopsResult.getPoint());
                sleepTask(2000);

                // Navigate back to home/training camp area
                tapRandomPoint(new DTOPoint(310, 650), new DTOPoint(450, 730), 10, 100);
                sleepTask(500);

                // Reopen the troops interface completely
                reopenTroopsInterface();

            } catch (Exception e) {
                logInfo("Error during training check attempt " + attempt + ": " + e.getMessage());
                if (attempt == 5) {
                    logInfo("All attempts failed, rescheduling in 1 hour");
                    reschedule(LocalDateTime.now().plusHours(1));
                }
            }
        }
    }

    /**
     * Reopen the troops interface after a manual attempt
     * This ensures we're in the correct state to read troop status
     */
    private void reopenTroopsInterface() {
        logInfo("Reopening troops interface for status refresh");

        ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(),
                "Manual attempt - reopening training interface for " + troopType);

        // Tap on troops menu
        emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(3, 513), new DTOPoint(26, 588));
        sleepTask(1000);

        // Tap on training tab
        emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(110, 270));
        sleepTask(500);

        logInfo("Troops interface reopened successfully");
    }

    /**
     * Process the troop status and take appropriate action
     * @param statusText The OCR text containing troop status
     * @param troopsResult The search result for the troop type
     * @return true if status was handled, false if should retry
     */
    private boolean handleTroopStatus(String statusText, DTOImageSearchResult troopsResult) {
        logInfo("Processing troop status: " + statusText);

        // Check if troops are currently upgrading
        if (statusText.contains("Upgrading")) {
            logInfo("Troops are upgrading, rescheduling in 1 hour");
            reschedule(LocalDateTime.now().plusHours(1));
            return true;
        }

        // Check if training is completed or troops are idle
        if (statusText.contains("Completed") || statusText.contains("Idle")) {
            logInfo("Training completed or troops idle, starting new training process");

            // Tap on troop to enter training interface
           emuManager.tapAtRandomPoint(EMULATOR_NUMBER,
                troopsResult.getPoint(), troopsResult.getPoint());
            sleepTask(2000);

            // Execute the training process
            processTraining();
            sleepTask(1000);

            // Extract and schedule next training time
            scheduleNextTraining();
            return true;
        }

        // Parse remaining training time and reschedule
        try {
            LocalDateTime nextTrainingTime = parseTime(statusText);
            logInfo("Training in progress, rescheduling for: " + nextTrainingTime);
            reschedule(nextTrainingTime);
            return true;
        } catch (Exception e) {
            logInfo("Could not parse training time from: " + statusText);
            return false; // Retry reading status
        }
    }

    // ===============================
    // TRAINING PROCESS METHODS
    // ===============================

    /**
     * Extract next training completion time and schedule the task accordingly
     */
    private void scheduleNextTraining() {
        ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(),
            "Extracting next training schedule time");

        Optional<LocalDateTime> optionalNextTime = extractNextTime();

        if (optionalNextTime.isPresent()) {
            LocalDateTime nextSchedule = optionalNextTime.get();
            logInfo("Next training scheduled for: " + nextSchedule);
            reschedule(nextSchedule);
        } else {
            // Fallback: reschedule in 1 hour if next time cannot be extracted
            LocalDateTime fallback = LocalDateTime.now().plusHours(1);
            logInfo("Could not extract next training time, rescheduling in 1 hour as fallback");
            reschedule(fallback);
        }
    }

    /**
     * Main training process orchestrator
     * Decides between promotion-priority training or normal training
     */
    public void processTraining() {
        logInfo("Starting training process");

        // Navigate to training camp area
        if (!navigateToTrainingCamp()) {
            logInfo("Failed to navigate to training camp, aborting training");
            return;
        }

        // Enter training interface
        enterTrainingInterface();

        // Check training mode configuration and execute accordingly
        boolean prioritizePromotion = profile.getConfig(
            EnumConfigurationKey.TRAIN_PRIORITIZE_PROMOTION_BOOL, Boolean.class);
        logInfo("Training mode - Prioritize promotion: " + prioritizePromotion);

        if (prioritizePromotion) {
            handlePromotionPriorityTraining();
        } else {
            handleNormalTraining();
        }
    }

    /**
     * Navigate to the training camp area
     * @return true if navigation successful, false otherwise
     */
    private boolean navigateToTrainingCamp() {
        logInfo("Navigating to training camp");

        // Tap on training camp area
        tapRandomPoint(new DTOPoint(310, 650), new DTOPoint(450, 730), 10, 100);

        // Verify we're in the correct location by searching for training template
        DTOImageSearchResult trainingResult = EmulatorManager.getInstance()
            .searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_CAMP_TRAIN.getTemplate(), 90);

        boolean found = trainingResult.isFound();
        logInfo("Training camp template found: " + found);

        if (found) {
            tapPoint(trainingResult.getPoint());
            sleepTask(500);
        }

        return found;
    }

    // ===============================
    // PROMOTION PRIORITY TRAINING
    // ===============================

    /**
     * Enter the training interface from the training camp
     */
    private void enterTrainingInterface() {
        logInfo("Entering training interface");

        // Tap on training interface area
        tapRandomPoint(new DTOPoint(222, 157), new DTOPoint(504, 231), 10, 100);
        sleepTask(500);
    }

    /**
     * Handles training logic when promotion priority is enabled
     * Attempts to find and promote lower-tier troops before training new ones
     */
    private void handlePromotionPriorityTraining() {
        logInfo("Starting promotion-priority training logic");

        // Reset troop list position to beginning
        resetTroopListPosition();

        // Find the maximum available troop level
        int maxLevel = findMaxAvailableTroopLevel();
        logInfo("Maximum available troop level found: " + maxLevel);

        if (maxLevel == -1) {
            logInfo("No troop levels detected, falling back to normal training");
            executeNormalTraining();
            return;
        }

        // Reset position again and attempt promotions
        swipe( new DTOPoint(73, 785),new DTOPoint(690, 785));
        sleepTask(100);
        swipe( new DTOPoint(73, 785),new DTOPoint(690, 785));
        sleepTask(400);
        boolean promotionExecuted = attemptTroopPromotions(maxLevel);

        if (!promotionExecuted) {
            ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(),
                "No promotions available, executing normal training");
            executeNormalTraining();
        }
    }

    /**
     * Reset the troop list to the beginning position by swiping
     */
    private void resetTroopListPosition() {
        logInfo("Resetting troop list to beginning position");

        // Swipe from right to left to go to beginning of troop list
        swipe(new DTOPoint(690, 785), new DTOPoint(73, 785));
        sleepTask(100);
        swipe(new DTOPoint(690, 785), new DTOPoint(73, 785));
        sleepTask(100);
    }

    /**
     * Find the maximum available troop level by iterating through all templates
     * @return The highest level found, or -1 if none found
     */
    private int findMaxAvailableTroopLevel() {
        List<EnumTemplates> templates = getTroopsTemplates(troopType);
        logInfo("Searching for maximum troop level among " + templates.size() + " templates");

        int maxLevel = -1;

        // Iterate through templates from highest to lowest tier
        for (EnumTemplates template : templates) {
            logInfo("Checking template: " + template.name());

            DTOImageSearchResult troop = emuManager.searchTemplate(EMULATOR_NUMBER, template.getTemplate(), 98);

            if (troop.isFound()) {
                int level = extractLevelFromTemplateName(template.name());
                if (level > 0) {
                    ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(),
                        "Found troop template: " + template.name() + " (Level " + level + ")");
                    maxLevel = level;
                    break; // Found highest available level
                }
            } else {
                // Swipe to search for next troop type
                swipe(new DTOPoint(490, 773), new DTOPoint(530, 773));
                sleepTask(400);
            }
        }

        return maxLevel;
    }

    /**
     * Extract numeric level from template name
     * @param templateName The template name (e.g., "TRAINING_INFANTRY_T6")
     * @return The extracted level number, or -1 if not found
     */
    private int extractLevelFromTemplateName(String templateName) {
        try {
            String levelStr = templateName.replaceAll("[^0-9]", "");

            if (!levelStr.isEmpty()) {
                int level = Integer.parseInt(levelStr);
                logInfo("Extracted level " + level + " from template: " + templateName);
                return level;
            } else {
                logInfo("No numeric level found in template name: " + templateName);
                return -1;
            }
        } catch (Exception e) {
            logInfo("Error extracting level from template " + templateName + ": " + e.getMessage());
            return -1;
        }
    }

    /**
     * Attempt to find and execute troop promotions for levels below maxLevel
     * @param maxLevel The maximum level available for promotion target
     * @return true if a promotion was executed, false otherwise
     */
    private boolean attemptTroopPromotions(int maxLevel) {
        List<EnumTemplates> templates = getTroopsTemplates(troopType);
        logInfo("Attempting to find promotable troops (levels below " + maxLevel + ")");

        // Search from highest to lowest level to prioritize higher-level promotions
        for (int i = templates.size() - 1; i >= 0; i--) {
            EnumTemplates template = templates.get(i);
            int templateLevel = extractLevelFromTemplateName(template.name());

            // Only consider troops with levels lower than maximum available
            if (templateLevel > 0 && templateLevel <= maxLevel) {
                if (attemptSingleTroopPromotion(template)) {
                    return true; // Promotion successful
                }
            }
        }

        logInfo("No promotable troops found");
        return false;
    }

    /**
     * Attempt to promote a single troop type
     * @param template The troop template to attempt promotion for
     * @return true if promotion was successful, false otherwise
     */
    private boolean attemptSingleTroopPromotion(EnumTemplates template) {
        logInfo("Attempting promotion for: " + template.name());

        DTOImageSearchResult troop = emuManager.searchTemplate(EMULATOR_NUMBER, template.getTemplate(), 98);

        if (troop.isFound()) {
            // Tap on troop to check promotion availability
            tapPoint(troop.getPoint());
            sleepTask(500);

            DTOImageSearchResult promoteButton = emuManager.searchTemplate(EMULATOR_NUMBER,
                EnumTemplates.TRAINING_TROOP_PROMOTE.getTemplate(), 90);

            if (promoteButton.isFound()) {
                return executePromotion(template, promoteButton);
            } else {
                logInfo("No promotion button found for: " + template.name());
                navigateToNextTroop();
                return false;
            }
        } else {
            navigateToNextTroop();
            return false;
        }
    }

    /**
     * Execute the promotion process for a troop
     * @param template The troop template being promoted
     * @param promoteButton The promote button search result
     * @return true if promotion executed successfully
     */
    private boolean executePromotion(EnumTemplates template, DTOImageSearchResult promoteButton) {
        ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(),
            "Executing promotion for: " + template.name());

        // Tap promote button
        emuManager.tapAtRandomPoint(EMULATOR_NUMBER, promoteButton.getPoint(), promoteButton.getPoint());
        sleepTask(500);

        // Confirm promotion
        tapPoint(new DTOPoint(523, 900));
        sleepTask(500);

        logInfo("Promotion completed for: " + template.name());
        return true;
    }

    // ===============================
    // NORMAL TRAINING METHODS
    // ===============================

    /**
     * Navigate to the next troop in the list
     */
    private void navigateToNextTroop() {
        swipe(new DTOPoint(530, 773), new DTOPoint(490, 773));
        sleepTask(300);
    }

    /**
     * Handle normal training when promotion priority is disabled
     */
    private void handleNormalTraining() {
        logInfo("Executing normal training mode");
        executeNormalTraining();
    }

    // ===============================
    // TIME PARSING AND EXTRACTION
    // ===============================

    /**
     * Execute normal training by finding and tapping the training button
     */
    private void executeNormalTraining() {
        DTOImageSearchResult trainingButtonResult = EmulatorManager.getInstance()
            .searchTemplate(EMULATOR_NUMBER, EnumTemplates.TRAINING_TRAIN_BUTTON.getTemplate(), 90);

        if (trainingButtonResult.isFound()) {
            ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(),
                "Normal training button found, executing training");

            EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER,
                trainingButtonResult.getPoint(), trainingButtonResult.getPoint());
            sleepTask(500);
        } else {
            logInfo("Training button not found - no training available");
        }
    }

    /**
     * Extract the next training completion time from the UI
     * @return Optional containing the next training time, or empty if extraction failed
     */
    private Optional<LocalDateTime> extractNextTime() {
        try {
            // OCR region containing training completion time
            String text = EmulatorManager.getInstance().ocrRegionText(EMULATOR_NUMBER,
                new DTOPoint(410, 997), new DTOPoint(586, 1048));

            LocalDateTime nextTime = addTimeToLocalDateTime(LocalDateTime.now(), text);
            logInfo("Successfully extracted next training time: " + nextTime);
            return Optional.of(nextTime);

        } catch (IOException | TesseractException e) {
            ServLogs.getServices().appendLog(EnumTpMessageSeverity.ERROR, taskName, profile.getName(),
                "OCR error while extracting training time: " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            ServLogs.getServices().appendLog(EnumTpMessageSeverity.ERROR, taskName, profile.getName(),
                "Unexpected error extracting training time: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parse training time from OCR text
     * @param input The OCR text containing time information
     * @return LocalDateTime when training will complete
     */
    public LocalDateTime parseTime(String input) {
        return addTimeToLocalDateTime(LocalDateTime.now(), input);
    }

    // ===============================
    // CONFIGURATION AND UTILITY METHODS
    // ===============================

    /**
     * Get the list of troop templates for a specific troop type
     * @param type The troop type to get templates for
     * @return List of templates ordered from highest to lowest tier
     */
    public List<EnumTemplates> getTroopsTemplates(TroopType type) {
        List<EnumTemplates> templates = new ArrayList<>();
        return switch (type) {
            case INFANTRY -> {
                templates.add(EnumTemplates.TRAINING_INFANTRY_T11);
                templates.add(EnumTemplates.TRAINING_INFANTRY_T10);
                templates.add(EnumTemplates.TRAINING_INFANTRY_T9);
                templates.add(EnumTemplates.TRAINING_INFANTRY_T8);
                templates.add(EnumTemplates.TRAINING_INFANTRY_T7);
                templates.add(EnumTemplates.TRAINING_INFANTRY_T6);
                templates.add(EnumTemplates.TRAINING_INFANTRY_T5);
                templates.add(EnumTemplates.TRAINING_INFANTRY_T4);
                templates.add(EnumTemplates.TRAINING_INFANTRY_T3);
                templates.add(EnumTemplates.TRAINING_INFANTRY_T2);
                templates.add(EnumTemplates.TRAINING_INFANTRY_T1);
                yield templates;
            }
            case LANCER -> {
                templates.add(EnumTemplates.TRAINING_LANCER_T11);
                templates.add(EnumTemplates.TRAINING_LANCER_T10);
                templates.add(EnumTemplates.TRAINING_LANCER_T9);
                templates.add(EnumTemplates.TRAINING_LANCER_T8);
                templates.add(EnumTemplates.TRAINING_LANCER_T7);
                templates.add(EnumTemplates.TRAINING_LANCER_T6);
                templates.add(EnumTemplates.TRAINING_LANCER_T5);
                templates.add(EnumTemplates.TRAINING_LANCER_T4);
                templates.add(EnumTemplates.TRAINING_LANCER_T3);
                templates.add(EnumTemplates.TRAINING_LANCER_T2);
                templates.add(EnumTemplates.TRAINING_LANCER_T1);
                yield templates;
            }
            case MARKSMAN -> {
                templates.add(EnumTemplates.TRAINING_MARKSMAN_T11);
                templates.add(EnumTemplates.TRAINING_MARKSMAN_T10);
                templates.add(EnumTemplates.TRAINING_MARKSMAN_T9);
                templates.add(EnumTemplates.TRAINING_MARKSMAN_T8);
                templates.add(EnumTemplates.TRAINING_MARKSMAN_T7);
                templates.add(EnumTemplates.TRAINING_MARKSMAN_T6);
                templates.add(EnumTemplates.TRAINING_MARKSMAN_T5);
                templates.add(EnumTemplates.TRAINING_MARKSMAN_T4);
                templates.add(EnumTemplates.TRAINING_MARKSMAN_T3);
                templates.add(EnumTemplates.TRAINING_MARKSMAN_T2);
                templates.add(EnumTemplates.TRAINING_MARKSMAN_T1);
                yield templates;
            }
        };
    }

    /**
     * Get the screen coordinates for OCR reading of troop status
     * @param type The troop type to get coordinates for
     * @return Array of DTOPoint containing top-left and bottom-right coordinates
     */
    public DTOPoint[] getTroopsPoints(TroopType type) {
        return switch (type) {
            case INFANTRY -> new DTOPoint[]{new DTOPoint(142, 557), new DTOPoint(340, 592)};
            case LANCER -> new DTOPoint[]{new DTOPoint(142, 628), new DTOPoint(340, 663)};
            case MARKSMAN -> new DTOPoint[]{new DTOPoint(142, 705), new DTOPoint(340, 737)};
        };
    }

    // ===============================
    // TASK FRAMEWORK OVERRIDES
    // ===============================

    @Override
    protected Object getDistinctKey() {
        return troopType;
    }

    @Override
    public boolean provideDailyMissionProgress() {
        return true;
    }

    @Override
    protected EnumStartLocation getRequiredStartLocation() {
        return EnumStartLocation.HOME;
    }

    // ===============================
    // INNER CLASSES
    // ===============================

    /**
     * Enumeration of available troop types for training
     */
    public enum TroopType {
        INFANTRY(EnumTemplates.GAME_HOME_SHORTCUTS_INFANTRY),
        LANCER(EnumTemplates.GAME_HOME_SHORTCUTS_LANCER),
        MARKSMAN(EnumTemplates.GAME_HOME_SHORTCUTS_MARKSMAN);

        private final EnumTemplates template;

        TroopType(EnumTemplates template) {
            this.template = template;
        }

        public String getTemplate() {
            return template.getTemplate();
        }
    }

}
