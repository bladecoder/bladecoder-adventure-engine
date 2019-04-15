package com.bladecoder.engine.ink;

import com.bladecoder.engine.model.World;
import com.bladecoder.ink.runtime.Story.ExternalFunction;

public class ExternalFunctions {

	private InkManager inkManager;

	public ExternalFunctions() {
	}

	public void bindExternalFunctions(final World w, InkManager ink) throws Exception {

		this.inkManager = ink;

		inkManager.getStory().bindExternalFunction("inInventory", new ExternalFunction() {

			@Override
			public Object call(Object[] args) throws Exception {
				String actor = args[0].toString();

				if (actor.charAt(0) == '>')
					actor = actor.substring(1);

				return w.getInventory().get(actor) != null;
			}
		});
	}
}
