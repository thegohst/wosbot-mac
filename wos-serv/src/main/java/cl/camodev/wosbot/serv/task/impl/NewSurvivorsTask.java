package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.task.DelayedTask;

import java.time.LocalDateTime;

public class NewSurvivorsTask extends DelayedTask {


    public NewSurvivorsTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
        super(profile, tpTask);
    }

    @Override
    protected void execute() {

        DTOImageSearchResult world = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 90);
        if (world.isFound()) {
            emuManager.tapAtPoint(EMULATOR_NUMBER, world.getPoint());
            sleepTask(2000);
        }

        //i need to search for New Survivors Template
        DTOImageSearchResult newSurvivors = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_NEW_SURVIVORS.getTemplate(),  90);
        if (newSurvivors.isFound()) {
            emuManager.tapAtPoint(EMULATOR_NUMBER, newSurvivors.getPoint());
            sleepTask(1000);
            //i need to accept the survivors then check if there's empty spots in the buildings
            DTOImageSearchResult welcomeIn = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_NEW_SURVIVORS_WELCOME_IN.getTemplate(),  90);
            if (welcomeIn.isFound()) {
                emuManager.tapAtPoint(EMULATOR_NUMBER, welcomeIn.getPoint());
                logInfo("Waiting a little before going go reassign survivors");
                sleepTask(10000);

                emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(309,20));
                sleepTask(300);

                //reset scroll (just in case)
                emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(340, 610), new DTOPoint(340, 900));
                sleepTask(200);

                DTOImageSearchResult plusButton=null;
                while((plusButton = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_NEW_SURVIVORS_PLUS_BUTTON.getTemplate(),  90)).isFound()){
                    emuManager.tapAtPoint(EMULATOR_NUMBER,plusButton.getPoint());
                    sleepTask(50);
                }

                //scroll down a little bit an do the same
                emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(340, 900),new DTOPoint(340, 610));
                sleepTask(200);
                while((plusButton = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_NEW_SURVIVORS_PLUS_BUTTON.getTemplate(),  90)).isFound()){
                    emuManager.tapAtPoint(EMULATOR_NUMBER,plusButton.getPoint());
                    sleepTask(50);
                }

                this.reschedule(LocalDateTime.now().plusMinutes(profile.getConfig(EnumConfigurationKey.CITY_ACCEPT_NEW_SURVIVORS_OFFSET_INT,Integer.class)));
            }


        } else {
            this.reschedule(LocalDateTime.now().plusMinutes(profile.getConfig(EnumConfigurationKey.CITY_ACCEPT_NEW_SURVIVORS_OFFSET_INT,Integer.class)));

        }


    }
}
