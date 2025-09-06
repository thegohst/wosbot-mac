package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class ChiefOrderTask extends DelayedTask {

	private enum ChiefOrderType {
		//@formatter:off
        DOUBLE_TIME("Chief Order 1"),
        RUSH_JOB("Chief Order 2");
        //@formatter:on

		private final String description;

		private ChiefOrderType(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	private final ChiefOrderType chiefOrderType;

	public ChiefOrderTask(DTOProfiles profile, TpDailyTaskEnum tpTask, ChiefOrderType chiefOrderType) {
		super(profile, tpTask);
		this.chiefOrderType = chiefOrderType;
	}

	@Override
	protected void execute() {
		// Implement the logic for the Chief Order task here
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Executing chief order task");
	}

}
