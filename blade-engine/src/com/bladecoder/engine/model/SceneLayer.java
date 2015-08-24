package com.bladecoder.engine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bladecoder.engine.actions.ModelDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ModelDescription("Scenes can have a list of layers. Actors are added to a specific layer to control the draw order")
public class SceneLayer extends AbstractModel {
	@JsonProperty(required = true, defaultValue = "true")
	@JsonPropertyDescription("Layer visibility")
	private boolean visible = true;

	@JsonProperty(defaultValue = "false")
	@JsonPropertyDescription("True for actor reordering based in y position")
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

	public void orderByZIndex() {
		Collections.sort(actors, new Comparator<BaseActor>() {

			@Override
			public int compare(BaseActor a1, BaseActor a2) {
				if(a1 instanceof InteractiveActor && a2 instanceof InteractiveActor)
					return (int) (((InteractiveActor)a1).getZIndex() - ((InteractiveActor)a2).getZIndex());
				else 
					return 0;
			}
		});
	}

	public void remove(BaseActor actor) {
		actors.remove(actor);
	}
}
