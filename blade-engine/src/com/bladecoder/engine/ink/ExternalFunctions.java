package com.bladecoder.engine.ink;

import com.bladecoder.engine.actions.SceneActorRef;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;
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

		inkManager.getStory().bindExternalFunction("getActorState", new ExternalFunction() {

			@Override
			public Object call(Object[] args) throws Exception {
				SceneActorRef actor = new SceneActorRef(args[0].toString());
				final Scene s = actor.getScene(w);

				String actorId = actor.getActorId();

				InteractiveActor a = (InteractiveActor) s.getActor(actorId, true);

				if (a == null) {
					EngineLogger.error("getActorState - Actor not found: " + actorId);
					return "";
				}

				return a.getState() == null ? "" : a.getState();
			}
		});

		inkManager.getStory().bindExternalFunction("getSceneState", new ExternalFunction() {

			@Override
			public Object call(Object[] args) throws Exception {
				String scene = args[0].toString();
				final Scene s = w.getScene(scene);

				if (s == null) {
					EngineLogger.error("getSceneState - Scene not found: " + scene);
					return "";
				}

				return s.getState() == null ? "" : s.getState();
			}
		});
	}
}
