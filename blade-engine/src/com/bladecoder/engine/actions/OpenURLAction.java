package com.bladecoder.engine.actions;

import com.badlogic.gdx.Gdx;
import com.bladecoder.engine.model.VerbRunner;

@ActionDescription("Open an URL in the default Browser")
public class OpenURLAction implements Action {
	@ActionProperty(required=true)
	@ActionPropertyDescription("The URL")
	private String url;

	@Override
	public boolean run(VerbRunner cb) {
		
		Gdx.net.openURI(url);
		
		return false;
	}
}
