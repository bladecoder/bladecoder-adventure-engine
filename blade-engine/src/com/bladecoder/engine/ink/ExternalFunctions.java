package com.bladecoder.engine.ink;

import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.ink.runtime.Story.ExternalFunction;

public class ExternalFunctions {

	private InkManager inkManager;

	public ExternalFunctions() {
	}

	public void bindExternalFunctions(InkManager ink) throws Exception {

		this.inkManager = ink;

		// WARNING: Not use this function, use the set Command instead.
		inkManager.getStory().bindExternalFunction("setModelProp", new ExternalFunction() {

			@Override
			public Object call(Object[] args) throws Exception {

				try {
					World.getInstance().setModelProp(args[0].toString(), args[1].toString());
				} catch (Exception e) {
					EngineLogger.error("Ink setModelProp: " + e.getMessage());
				}

				return null;
			}
		});

		inkManager.getStory().bindExternalFunction("getModelProp", new ExternalFunction() {

			@Override
			public Object call(Object[] args) throws Exception {
				try {
					return World.getInstance().getModelProp(args[0].toString());
				} catch (Exception e) {
					EngineLogger.error("Ink getModelProp: " + e.getMessage(), e);
				}

				return null;
			}
		});

		inkManager.getStory().bindExternalFunction("inInventory", new ExternalFunction() {

			@Override
			public Object call(Object[] args) throws Exception {
				return World.getInstance().getInventory().get(args[0].toString()) != null;
			}
		});
	}
}
