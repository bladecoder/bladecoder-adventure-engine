package com.bladecoder.engine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class SceneLayer {
	private String name;
	private boolean visible = true;
	private boolean dynamic;
	
	transient private final List<BaseActor> actors = new ArrayList<BaseActor>();
	
	public void update() {
		if(dynamic && visible)
			Collections.sort(actors);
	}
	
	public void draw(SpriteBatch spriteBatch) {
		if(!visible)
			return;
		
		for (BaseActor a : actors) {
			if(a instanceof SpriteActor)
				((SpriteActor)a).draw(spriteBatch);
		}
	}
	
	public void add(BaseActor actor) {
		actors.add(actor);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	public List<BaseActor> getActors() {
		return actors;
	}
}
