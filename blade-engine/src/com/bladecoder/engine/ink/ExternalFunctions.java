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
					String p = args[0].toString();
					
					if(p.charAt(0) == '>')
						p = p.substring(1);
					
					String v = args[1].toString();
					
					if(v.charAt(0) == '>')
						v = v.substring(1);
					
					World.getInstance().setModelProp(p, v);
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
					String p = args[0].toString();
					
					if(p.charAt(0) == '>')
						p = p.substring(1);
					
					return World.getInstance().getModelProp(p);
				} catch (Exception e) {
					EngineLogger.error("Ink getModelProp: " + e.getMessage(), e);
				}

				return null;
			}
		});

		inkManager.getStory().bindExternalFunction("inInventory", new ExternalFunction() {

			@Override
			public Object call(Object[] args) throws Exception {
				String actor = args[0].toString();
				
				if(actor.charAt(0) == '>')
					actor = actor.substring(1);
				
				return World.getInstance().getInventory().get(actor) != null;
			}
		});
	}
}
