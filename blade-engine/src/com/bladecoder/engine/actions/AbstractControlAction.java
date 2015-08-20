package com.bladecoder.engine.actions;

import java.util.List;

public abstract class AbstractControlAction implements Action {
	public abstract String getControlActionID();

	protected int skipControlIdBlock(List<Action> actions, int ip) {
		final String caID = getControlActionID();

		do {
			ip++;
		} while (!(actions.get(ip) instanceof AbstractControlAction)
				|| !((AbstractControlAction) actions.get(ip)).getControlActionID().equals(caID));

		return ip;
	}
}
