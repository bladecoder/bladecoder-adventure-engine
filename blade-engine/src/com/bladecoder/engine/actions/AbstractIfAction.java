package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.VerbRunner;

public abstract class AbstractIfAction extends AbstractControlAction {
	protected void gotoElse(VerbRunner v) {
		int ip = skipControlIdBlock(v.getActions(), v.getIP());

		v.setIP(ip);
	}
}
