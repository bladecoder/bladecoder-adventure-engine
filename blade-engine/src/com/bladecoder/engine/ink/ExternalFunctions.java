package com.bladecoder.engine.ink;

import com.bladecoder.engine.model.World;
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
				
				World.getInstance().setModelProp(args[0].toString(), args[1].toString());

				return null;
			}
		});
		
		inkManager.getStory().bindExternalFunction("getModelProp", new ExternalFunction() {

			@Override
			public Object call(Object[] args) throws Exception {
				
				return World.getInstance().getModelProp(args[0].toString());
			}
		});
	}
}
