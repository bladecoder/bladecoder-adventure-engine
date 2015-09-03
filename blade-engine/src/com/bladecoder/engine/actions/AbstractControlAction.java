package com.bladecoder.engine.actions;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public abstract class AbstractControlAction extends AbstractAction {
	@JsonProperty
	protected String caID;

	public String getControlActionId() {
		return caID;
	}

	public void setControlActionId(String caID) {
		this.caID = caID;
	}

	protected int skipControlIdBlock(List<AbstractAction> actions, int ip) {
		final String caID = getControlActionId();

		do {
			ip++;
		} while (!(actions.get(ip) instanceof AbstractControlAction)
				|| !((AbstractControlAction) actions.get(ip)).getControlActionId().equals(caID));

		return ip;
	}
}
