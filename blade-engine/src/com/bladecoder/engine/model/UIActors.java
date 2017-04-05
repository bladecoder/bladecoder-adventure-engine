package com.bladecoder.engine.model;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.SceneActorRef;
import com.bladecoder.engine.assets.AssetConsumer;

public class UIActors implements AssetConsumer, Serializable   {
	private ArrayList<InteractiveActor>  actors = new ArrayList<InteractiveActor>();
	
	transient private boolean disposed= true;
	
	public void addActor(InteractiveActor a) {
		actors.add(a);
	}
	
	public InteractiveActor removeActor(String id) {	
		for(int i = 0; i < actors.size(); i++) {
			InteractiveActor a = actors.get(i);
			
			if(a.getId().equals(id)) {
				actors.remove(i);
				return a;
			}
		}
		
		return null;
	}
	
	public void update(float delta) {
		for(InteractiveActor a:actors) {
			a.update(delta);
		}
	}
	
	public void draw(SpriteBatch spriteBatch) {

	}
	
	@Override
	public void loadAssets() {
		for (InteractiveActor a : actors)
			a.loadAssets();		
	}
	
	@Override
	public void retrieveAssets() {
		for (InteractiveActor a : actors) {
			a.retrieveAssets();
		}
		
		disposed = false;
	}	
	
	@Override
	public void dispose() {
		for (InteractiveActor a : actors)
			a.dispose();
		
		disposed = true;
	}
	
	public boolean isDisposed() {
		return disposed;
	}

	
	@Override
	public void write(Json json) {
		SceneActorRef actorRef;

		json.writeObjectStart("actors");
		for (InteractiveActor a : actors) {
			actorRef = new SceneActorRef(a.getInitScene(), a.getId());
			json.writeValue(actorRef.toString(), a);
		}
		json.writeObjectEnd();
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		
		actors.clear();
		
		JsonValue jsonValueActors = jsonData.get("actors");
		SceneActorRef actorRef;

		// GET ACTORS FROM HIS INIT SCENE.
		for (int i = 0; i < jsonValueActors.size; i++) {
			JsonValue jsonValueAct = jsonValueActors.get(i);
			actorRef = new SceneActorRef(jsonValueAct.name);
			Scene sourceScn = World.getInstance().getScene(actorRef.getSceneId());

			BaseActor actor = sourceScn.getActor(actorRef.getActorId(), false);
			sourceScn.removeActor(actor);
			addActor((InteractiveActor)actor);
		}
		
		// READ ACTOR STATE. 
		// The state must be retrieved after getting actors from his init scene to restore verb cb properly.
		for (int i = 0; i < jsonValueActors.size; i++) {
			JsonValue jsonValueAct = jsonValueActors.get(i);
			actorRef = new SceneActorRef(jsonValueAct.name);

			InteractiveActor actor = actors.get(i);
			actor.read(json, jsonValueAct);
		}
	}
}
