package com.bladecoder.engine.actions;

import com.badlogic.gdx.Gdx;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Open an URL in the default Browser")
public class OpenURLAction implements Action {
	@ActionProperty(required=true)
	@ActionPropertyDescription("The URL")
	private String url;
	
	@Override
	public void init(World w) {
	}

	@Override
	public boolean run(VerbRunner cb) {
		
		Gdx.net.openURI(url);
		
		return false;
	}
}
