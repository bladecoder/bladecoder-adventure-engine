package com.bladecoder.engine.model;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.actions.SceneActorRef;
import com.bladecoder.engine.assets.AssetConsumer;

public class UIActors implements AssetConsumer, Serializable {
	private ArrayList<InteractiveActor> actors = new ArrayList<InteractiveActor>(0);

	transient private boolean disposed = true;
	private SceneCamera cam;
	private World w;

	public UIActors(World w) {
		this.w = w;
	}

	public void addActor(InteractiveActor a) {
		actors.add(a);
	}

	public InteractiveActor removeActor(String id) {
		for (int i = 0; i < actors.size(); i++) {
			InteractiveActor a = actors.get(i);

			if (a.getId().equals(id)) {
				actors.remove(i);
				return a;
			}
		}

		return null;
	}

	public InteractiveActor get(String actorId) {
		for (InteractiveActor a : actors) {
			if (a.getId().equals(actorId))
				return a;
		}

		return null;
	}

	public List<InteractiveActor> getActors() {
		return actors;
	}

	public void update(float delta) {
		for (InteractiveActor a : actors) {
			a.update(delta);
		}
	}

	public void draw(SpriteBatch batch) {

		batch.setProjectionMatrix(cam.combined);
		batch.begin();

		for (InteractiveActor a : actors) {
			if (a instanceof SpriteActor) {
				if (a.isVisible()) {
					if (((SpriteActor) a).getScale() != 0) {
						((SpriteActor) a).getRenderer().draw(batch, a.getX(), a.getY(), ((SpriteActor) a).getScaleX(),
								((SpriteActor) a).getScaleY(), ((SpriteActor) a).getRot(), ((SpriteActor) a).getTint());
					}
				}
			}
		}
		batch.end();
	}

	// tmp vector to use in getActorAtInput()
	private final Vector3 unprojectTmp = new Vector3();

	public InteractiveActor getActorAtInput(Viewport v) {

		cam.getInputUnProject(v, unprojectTmp);

		for (InteractiveActor uia : actors) {
			if (uia.canInteract() && uia.hit(unprojectTmp.x, unprojectTmp.y))
				return uia;
		}

		return null;
	}

	@Override
	public void loadAssets() {
		for (InteractiveActor a : actors)
			if (a instanceof SpriteActor)
				((AssetConsumer) a).loadAssets();
	}

	@Override
	public void retrieveAssets() {
		for (InteractiveActor a : actors) {
			if (a instanceof SpriteActor)
				((AssetConsumer) a).retrieveAssets();
		}

		cam = new SceneCamera();
		cam.create(w.getWidth(), w.getHeight());

		disposed = false;
	}

	@Override
	public void dispose() {
		for (InteractiveActor a : actors)
			if (a instanceof SpriteActor)
				((Disposable) a).dispose();

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
			Scene sourceScn = w.getScene(actorRef.getSceneId());

			BaseActor actor = sourceScn.getActor(actorRef.getActorId(), false);
			sourceScn.removeActor(actor);
			addActor((InteractiveActor) actor);
		}

		// READ ACTOR STATE.
		// The state must be retrieved after getting actors from his init scene
		// to restore verb cb properly.
		for (int i = 0; i < jsonValueActors.size; i++) {
			JsonValue jsonValueAct = jsonValueActors.get(i);
			actorRef = new SceneActorRef(jsonValueAct.name);

			InteractiveActor actor = actors.get(i);
			actor.read(json, jsonValueAct);
		}
	}
}
