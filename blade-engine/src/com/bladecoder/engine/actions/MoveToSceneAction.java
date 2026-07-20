package com.bladecoder.engine.actions;

import com.badlogic.gdx.utils.Disposable;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Move the actor to the selected scene")
public class MoveToSceneAction implements Action {
	@ActionProperty(required = true)
	@ActionPropertyDescription("The selected actor")
	private SceneActorRef actor;

	@ActionPropertyDescription("The target scene. The current scene if empty.")
	@ActionProperty(type = Type.SCENE)
	private String scene;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		final Scene s = actor.getScene(w);

		final String actorId = actor.getActorId();

		if (actorId == null) {
			// if called in a scene verb and no actor is specified, we do nothing
			EngineLogger.error(getClass() + ": No actor specified");
			return false;
		}

		BaseActor a = s.getActor(actorId, false);

		if (a == null) {
			EngineLogger.error(getClass() + "- Actor not found: " + actorId + " in scene: " + s.getId());
			return false;
		}

		s.removeActor(a);

		Scene ts = null;

		if (scene == null)
			ts = w.getCurrentScene();
		else
			ts = w.getScene(scene);

		// Dispose if s is the current scene or a cached scene and the target is not the
		// current scene or a cache scene
		if ((s == w.getCurrentScene() || w.getCachedScene(ts.getId()) != null)
				&& !(ts == w.getCurrentScene() || w.getCachedScene(ts.getId()) != null) && a instanceof Disposable) {
			((Disposable) a).dispose();
		}

		// We must load assets when the target scene is the current scene or when
		// the scene is cached.
		if ((ts == w.getCurrentScene() || w.getCachedScene(ts.getId()) != null)
				&& !(s == w.getCurrentScene() || w.getCachedScene(s.getId()) != null) && a instanceof AssetConsumer) {
			((AssetConsumer) a).loadAssets();
			EngineAssetManager.getInstance().finishLoading();
			((AssetConsumer) a).retrieveAssets();
		}

		ts.addActor(a);

		return false;
	}

}
