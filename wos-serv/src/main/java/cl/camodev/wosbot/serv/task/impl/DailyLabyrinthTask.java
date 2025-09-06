package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.utiles.UtilTime;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.task.DelayedTask;
import cl.camodev.wosbot.serv.task.EnumStartLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Task responsible for completing daily labyrinth challenges.
 * This task navigates to the labyrinth menu and executes appropriate challenges
 * based on the current day of the week.
 */
public class DailyLabyrinthTask extends DelayedTask {

    // =========================== CONSTANTS ===========================

    private static final Logger logger = LoggerFactory.getLogger(DailyLabyrinthTask.class);

    // Navigation points
    private static final DTOPoint SIDE_MENU_AREA_START = new DTOPoint(3, 513);
    private static final DTOPoint SIDE_MENU_AREA_END = new DTOPoint(26, 588);
    private static final DTOPoint CITY_TAB_BUTTON = new DTOPoint(110, 270);
    private static final DTOPoint SCROLL_START_POINT = new DTOPoint(400, 800);
    private static final DTOPoint SCROLL_END_POINT = new DTOPoint(400, 400);
    private static final DTOPoint SKIP_BUTTON = new DTOPoint(71, 827);
    private static final DTOPoint RESULT_SKIP_BUTTON = new DTOPoint(640, 175);

    // Timing constants
    private static final int MENU_NAVIGATION_DELAY = 1000;
    private static final int TAB_SWITCH_DELAY = 500;
    private static final int SCROLL_DELAY = 1300;
    private static final int LABYRINTH_LOAD_DELAY = 2000;
    private static final int BATTLE_COMPLETION_DELAY = 3000;
    private static final int TEMPLATE_SEARCH_THRESHOLD = 90;

    // =========================== CONSTRUCTOR ===========================

    public DailyLabyrinthTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
        super(profile, tpTask);
    }

    // =========================== TASK OVERRIDES ===========================

    @Override
    public boolean provideDailyMissionProgress() {
        return true;
    }

    @Override
    public EnumStartLocation getRequiredStartLocation() {
        return EnumStartLocation.HOME;
    }

    @Override
    protected void execute() {
        logger.info("Starting Daily Labyrinth Task execution for profile: {}", profile.getName());

        try {
            // Step 1: Navigate to labyrinth menu
            if (!navigateToLabyrinthMenu()) {
                rescheduleOneHourLater("Failed to navigate to labyrinth menu");
                return;
            }

            // Step 2: Execute challenges based on current day
            executeLabyrinthChallenges();

            logger.info("Daily Labyrinth Task completed successfully for profile: {}", profile.getName());
            reschedule(UtilTime.getGameReset());

        } catch (Exception e) {
            logger.error("Error during labyrinth task execution: {}", e.getMessage(), e);
            rescheduleOneHourLater("Unexpected error during execution: " + e.getMessage());
        }
    }

    // =========================== NAVIGATION METHODS ===========================

    /**
     * Opens the side menu, switches to city tab, scrolls down and searches for labyrinth
     * @return true if navigation was successful, false otherwise
     */
    private boolean navigateToLabyrinthMenu() {
        logger.debug("Navigating to labyrinth menu");

        // Open side menu
        tapRandomPoint(SIDE_MENU_AREA_START, SIDE_MENU_AREA_END);
        sleepTask(MENU_NAVIGATION_DELAY);

        // Switch to city tab
        tapPoint(CITY_TAB_BUTTON);
        sleepTask(TAB_SWITCH_DELAY);

        // Scroll down to find labyrinth
        swipe(SCROLL_START_POINT, SCROLL_END_POINT);
        sleepTask(SCROLL_DELAY);

        // Search for labyrinth in menu
        DTOImageSearchResult labyrinthResult = emuManager.searchTemplate(
                EMULATOR_NUMBER,
                EnumTemplates.LEFT_MENU_LABYRINTH_BUTTON.getTemplate(),
                TEMPLATE_SEARCH_THRESHOLD
        );

        if (labyrinthResult.isFound()) {
            tapPoint(labyrinthResult.getPoint());
            sleepTask(LABYRINTH_LOAD_DELAY);
            logInfo("Successfully navigated to labyrinth menu");
            return true;
        } else {
            logWarning("Labyrinth menu item not found");
            return false;
        }
    }

    // =========================== CHALLENGE EXECUTION ===========================

    /**
     * Executes labyrinth challenges based on the current day of the week
     */
    private void executeLabyrinthChallenges() {
        DayOfWeek currentDay = LocalDateTime.now().getDayOfWeek();
        List<Integer> availableDungeons = getAvailableDungeons(currentDay);

        logger.info("Executing challenges for {}, available dungeons: {}", currentDay, availableDungeons);

        boolean anyCompleted = false;
        for (Integer dungeonNumber : availableDungeons) {
            if (executeDungeonChallenge(dungeonNumber)) {
                logger.info("Successfully completed challenge for dungeon {}", dungeonNumber);
                logInfo("Successfully completed challenge for dungeon " + dungeonNumber);
                anyCompleted = true;

            }
        }

        if (!anyCompleted) {
            logWarning("No dungeons were successfully completed for today");
        }
    }

    /**
     * Executes a specific dungeon challenge
     * @param dungeonNumber the dungeon number to challenge
     * @return true if challenge was completed successfully
     */
    private boolean executeDungeonChallenge(int dungeonNumber) {
        logger.debug("Attempting to execute dungeon {} challenge", dungeonNumber);

        DTOImageSearchResult labyrinthResult = emuManager.searchTemplate(
                EMULATOR_NUMBER,
                getDungeonTemplate(dungeonNumber).getTemplate(),
                TEMPLATE_SEARCH_THRESHOLD
        );

        if (!labyrinthResult.isFound()) {
            logger.debug("Dungeon {} not available for today", dungeonNumber);
            logWarning("Dungeon " + dungeonNumber + " not available for today");
            return false;
        }

        tapPoint(labyrinthResult.getPoint());
        sleepTask(TAB_SWITCH_DELAY);

        // Try quick challenge first
        if (attemptQuickChallenge(dungeonNumber)) {
            return true;
        }

        // Try raid challenge
        if (attemptRaidChallenge(dungeonNumber)) {
            return true;
        }

        // Try normal challenge
        return attemptNormalChallenge(dungeonNumber);
    }

    /**
     * Attempts to execute a quick challenge
     */
    private boolean attemptQuickChallenge(int dungeonNumber) {
        tapPoint(new DTOPoint(700, 1200));
        sleepTask(100);
        DTOImageSearchResult quickChallengeResult = emuManager.searchTemplate(
                EMULATOR_NUMBER,
                EnumTemplates.LABYRINTH_QUICK_CHALLENGE.getTemplate(),
                TEMPLATE_SEARCH_THRESHOLD
        );

        if (quickChallengeResult.isFound()) {
            logger.info("Quick Challenge available for dungeon: {}", dungeonNumber);
            logInfo("Quick Challenge available for dungeon: " + dungeonNumber);
            tapPoint(quickChallengeResult.getPoint());
            sleepTask(MENU_NAVIGATION_DELAY);

            // Skip battle animation
            tapPoint(SKIP_BUTTON);
            sleepTask(300);
            tapRandomPoint(SKIP_BUTTON, SKIP_BUTTON, 10, 50);
            tapBackButton();
            return true;
        }
        return false;
    }

    /**
     * Attempts to execute a raid challenge
     */
    private boolean attemptRaidChallenge(int dungeonNumber) {
        DTOImageSearchResult raidResult = emuManager.searchTemplate(
                EMULATOR_NUMBER,
                EnumTemplates.LABYRINTH_RAID_CHALLENGE.getTemplate(),
                TEMPLATE_SEARCH_THRESHOLD
        );

        if (raidResult.isFound()) {
            logger.info("Raid challenge available for dungeon: {}", dungeonNumber);
            logInfo("Raid challenge available for dungeon: " + dungeonNumber);
            tapPoint(raidResult.getPoint());
            sleepTask(400);
            tapRandomPoint(SKIP_BUTTON, SKIP_BUTTON, 10, 50);
            tapBackButton();
            return true;
        }
        return false;
    }

    /**
     * Attempts to execute a normal challenge
     */
    private boolean attemptNormalChallenge(int dungeonNumber) {
        DTOImageSearchResult normalChallengeResult = emuManager.searchTemplate(
                EMULATOR_NUMBER,
                EnumTemplates.LABYRINTH_NORMAL_CHALLENGE.getTemplate(),
                TEMPLATE_SEARCH_THRESHOLD
        );

        if (!normalChallengeResult.isFound()) {
            logger.debug("No normal challenge available for dungeon: {}", dungeonNumber);
            logWarning("No normal challenge available for dungeon: " + dungeonNumber);
            return false;
        }

        tapPoint(normalChallengeResult.getPoint());
        sleepTask(300);

        // Try quick deploy first
        DTOImageSearchResult quickDeployResult = emuManager.searchTemplate(
                EMULATOR_NUMBER,
                EnumTemplates.LABYRINTH_QUICK_DEPLOY.getTemplate(),
                TEMPLATE_SEARCH_THRESHOLD
        );

        if (quickDeployResult.isFound()) {
            logger.info("Quick deploying for dungeon: {}", dungeonNumber);
            logInfo("Quick deploying for dungeon: " + dungeonNumber);
            tapPoint(quickDeployResult.getPoint());
            sleepTask(100);
        }

        // Deploy troops
        DTOImageSearchResult deployResult = emuManager.searchTemplate(
                EMULATOR_NUMBER,
                EnumTemplates.LABYRINTH_DEPLOY.getTemplate(),
                TEMPLATE_SEARCH_THRESHOLD
        );

        if (deployResult.isFound()) {
            logger.info("Deploying troops for dungeon: {}", dungeonNumber);
            logInfo("Deploying troops for dungeon: " + dungeonNumber);
            tapPoint(deployResult.getPoint());
            sleepTask(BATTLE_COMPLETION_DELAY);

            // Skip battle results
            tapRandomPoint(RESULT_SKIP_BUTTON, RESULT_SKIP_BUTTON, 10, 50);
            tapBackButton();
            return true;
        }

        logger.warn("Could not deploy troops for dungeon: {}", dungeonNumber);
        logWarning("Could not deploy troops for dungeon: " + dungeonNumber);
        return false;
    }

    // =========================== UTILITY METHODS ===========================

    /**
     * Returns the list of available dungeons based on the day of the week
     * @param dayOfWeek the current day of the week
     * @return list of available dungeon numbers
     */
    private List<Integer> getAvailableDungeons(DayOfWeek dayOfWeek) {
        List<Integer> dungeons = new ArrayList<>();

        switch (dayOfWeek) {
            case MONDAY, TUESDAY -> dungeons.add(1);
            case WEDNESDAY, THURSDAY -> {
                dungeons.add(2);
                dungeons.add(3);
            }
            case FRIDAY, SATURDAY -> {
                dungeons.add(4);
                dungeons.add(5);
            }
            case SUNDAY -> dungeons.add(6);
        }

        return dungeons;
    }

    /**
     * Returns the appropriate template for each dungeon number
     * @param dungeonNumber the dungeon number (1-6)
     * @return the corresponding template enum
     */
    private EnumTemplates getDungeonTemplate(int dungeonNumber) {
        return switch (dungeonNumber) {
            case 1 -> EnumTemplates.LABYRINTH_DUNGEON_1;
            case 2 -> EnumTemplates.LABYRINTH_DUNGEON_2;
            case 3 -> EnumTemplates.LABYRINTH_DUNGEON_3;
            case 4 -> EnumTemplates.LABYRINTH_DUNGEON_4;
            case 5 -> EnumTemplates.LABYRINTH_DUNGEON_5;
            case 6 -> EnumTemplates.LABYRINTH_DUNGEON_6;
            default -> {
                logger.warn("Invalid dungeon number: {}, using dungeon 1 as fallback", dungeonNumber);
                yield EnumTemplates.LABYRINTH_DUNGEON_1;
            }
        };
    }

    /**
     * Reschedules the task for one hour later with a reason
     * @param reason the reason for rescheduling
     */
    private void rescheduleOneHourLater(String reason) {
        LocalDateTime nextExecution = LocalDateTime.now().plusHours(1);
        logger.warn("Rescheduling task for one hour later. Reason: {}", reason);
        logWarning("Task rescheduled: " + reason);
        this.reschedule(nextExecution);
    }


}
