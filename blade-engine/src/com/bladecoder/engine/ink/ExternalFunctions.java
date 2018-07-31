package com.bladecoder.engine.ink;

import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.ink.runtime.Story.ExternalFunction;

public class ExternalFunctions {

	private InkManager inkManager;

	public ExternalFunctions() {
	}

	public void bindExternalFunctions(final World w, InkManager ink) throws Exception {

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
					
					w.setModelProp(p, v);
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
					
					return w.getModelProp(p);
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
				
				return w.getInventory().get(actor) != null;
			}
		});
	}
}
