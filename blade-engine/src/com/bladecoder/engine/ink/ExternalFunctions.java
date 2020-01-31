package com.bladecoder.engine.ink;

import com.bladecoder.engine.actions.SceneActorRef;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Inventory;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.ink.runtime.Story;
import com.bladecoder.ink.runtime.Story.ExternalFunction0;
import com.bladecoder.ink.runtime.Story.ExternalFunction1;
import com.bladecoder.ink.runtime.Story.ExternalFunction2;

public class ExternalFunctions {

	public static void bindExternalFunctions(final World w, Story story) throws Exception {

		story.bindExternalFunction("inInventory", new ExternalFunction1<String, Boolean>() {

			@Override
			public Boolean call(String actor) throws Exception {
				if (actor.charAt(0) == '>')
					actor = actor.substring(1);

				return w.getInventory().get(actor) != null;
			}
		});

		story.bindExternalFunction("getActorState", new ExternalFunction1<String, String>() {

			@Override
			public String call(String act) throws Exception {
				SceneActorRef actor = new SceneActorRef(act);
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

		story.bindExternalFunction("getSceneState", new ExternalFunction1<String, String>() {

			@Override
			public String call(String scene) throws Exception {
				final Scene s = w.getScene(scene);

				if (s == null) {
					EngineLogger.error("getSceneState - Scene not found: " + scene);
					return "";
				}

				return s.getState() == null ? "" : s.getState();
			}
		});

		story.bindExternalFunction("getPlayer", new ExternalFunction0<String>() {

			@Override
			public String call() throws Exception {
				return w.getCurrentScene().getPlayer().getId();
			}
		});

		story.bindExternalFunction("inInventory2", new ExternalFunction2<String, String, Boolean>() {

			@Override
			public Boolean call(String actor, String inventory) throws Exception {
				if (actor.charAt(0) == '>')
					actor = actor.substring(1);

				Inventory inv = w.getInventories().get(inventory);

				if (inv == null) {
					EngineLogger.debug("InkExternalFunction::inInventory2: Inventory not found: " + inventory);

					return false;
				}

				return inv.get(actor) != null;
			}
		});

		story.bindExternalFunction("getCurrentScene", new ExternalFunction0<String>() {

			@Override
			public String call() throws Exception {
				return w.getCurrentScene().getId();
			}
		});

		story.bindExternalFunction("isDebug", new ExternalFunction0<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return EngineLogger.debugMode();
			}
		});

		story.bindExternalFunction("getProperty", new ExternalFunction1<String, String>() {

			@Override
			public String call(String p) throws Exception {
				String v = w.getCustomProperty(p);
				return v == null ? "" : v;
			}
		});

		story.bindExternalFunction("isVisible", new ExternalFunction1<String, Boolean>() {

			@Override
			public Boolean call(String act) throws Exception {
				SceneActorRef actor = new SceneActorRef(act);
				final Scene s = actor.getScene(w);

				String actorId = actor.getActorId();

				InteractiveActor a = (InteractiveActor) s.getActor(actorId, true);

				if (a == null) {
					EngineLogger.error("getActorState - Actor not found: " + actorId);
					return false;
				}

				return a.isVisible();
			}
		});
	}
}
