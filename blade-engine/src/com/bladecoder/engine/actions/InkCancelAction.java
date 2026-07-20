package com.bladecoder.engine.actions;

import com.bladecoder.engine.ink.InkVerbRunner;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Stops the selected Ink flow.")
public class InkCancelAction implements Action {
	@ActionPropertyDescription("The conversation flow. Empty for the default flow.")
	@ActionProperty(required = false)
	private String flow;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {

		InkVerbRunner inkVerbRunner;

		try {
			if (flow == null) {
				inkVerbRunner = w.getInkManager().getDefaultVerbRunner();
				w.getInkManager().getStory().switchToDefaultFlow();
			} else {
				inkVerbRunner = w.getInkManager().getVerbRunners().get(flow);

				if (inkVerbRunner == null) {
					EngineLogger.debug("Flow not found: " + flow);
					return false;
				}

				w.getInkManager().getStory().switchFlow(flow);
			}

			inkVerbRunner.cancel();
			w.getInkManager().getStory().resetCallstack();

		} catch (Exception e) {
			EngineLogger.error("Error cancelling flow: " + flow == null ? "DEFAULT" : flow);
		}

		return false;
	}
}
