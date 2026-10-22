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
    private static final java.util.concurrent.atomic.AtomicLong traceIds=new java.util.concurrent.atomic.AtomicLong();
    /** Bind without changing argument/result types or the runtime's default lookahead contract. */
    public static <R> void bind(final Story story,final String name,final Story.ExternalFunction<R> function) throws Exception {
        story.bindExternalFunction(name,traced(story,name,function));
    }
    public static <R> Story.ExternalFunction<R> traced(final Story story,final String name,final Story.ExternalFunction<R> function) {
        return new Story.ExternalFunction<R>() {
            public R call(Object... arguments) throws Exception {
                if(!EngineLogger.hasListeners())return function.call(arguments);
                long call=traceIds.incrementAndGet();
                String path=story.getState().getCurrentPathString(),flow=story.getCurrentFlowName();
                EngineLogger.diagnostic("ink.external.call","call_id",call,"name",name,"arguments",java.util.Arrays.asList(arguments.clone()),"path",path,"flow",flow,"scope","actual_engine_binding");
                try {
                    R result=function.call(arguments);
                    EngineLogger.diagnostic("ink.external.result","call_id",call,"name",name,"result",result,"path",path,"flow",flow,"scope","actual_engine_binding");
                    return result;
                } catch(Exception failure) {
                    EngineLogger.diagnostic("ink.external.failed","call_id",call,"name",name,"exception",failure.toString(),"path",path,"flow",flow,"scope","actual_engine_binding");
                    throw failure;
                }
            }
        };
    }


	public static void bindExternalFunctions(final World w, Story story) throws Exception {

		bind(story,"inInventory", new ExternalFunction1<String, Boolean>() {

			@Override
			public Boolean call(String actor) throws Exception {
				if (actor.charAt(0) == '>')
					actor = actor.substring(1);

				return w.getInventory().get(actor) != null;
			}
		});

		bind(story,"getActorState", new ExternalFunction1<String, String>() {

			@Override
			public String call(String act) throws Exception {
				if (act.charAt(0) == '>')
					act = act.substring(1);

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

		bind(story,"getSceneState", new ExternalFunction1<String, String>() {

			@Override
			public String call(String scene) throws Exception {
				if (scene.charAt(0) == '>')
					scene = scene.substring(1);

				final Scene s = w.getScene(scene);

				if (s == null) {
					EngineLogger.error("getSceneState - Scene not found: " + scene);
					return "";
				}

				return s.getState() == null ? "" : s.getState();
			}
		});

		bind(story,"getPlayer", new ExternalFunction0<String>() {

			@Override
			public String call() throws Exception {
				return w.getCurrentScene().getPlayer().getId();
			}
		});

		bind(story,"inInventory2", new ExternalFunction2<String, String, Boolean>() {

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

		bind(story,"getCurrentScene", new ExternalFunction0<String>() {

			@Override
			public String call() throws Exception {
				return w.getCurrentScene().getId();
			}
		});

		bind(story,"isDebug", new ExternalFunction0<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return EngineLogger.debugMode();
			}
		});

		bind(story,"getProperty", new ExternalFunction1<String, String>() {

			@Override
			public String call(String p) throws Exception {
				if (p.charAt(0) == '>')
					p = p.substring(1);

				String v = w.getCustomProperty(p);
				return v == null ? "" : v;
			}
		});

		bind(story,"isVisible", new ExternalFunction1<String, Boolean>() {

			@Override
			public Boolean call(String act) throws Exception {
				if (act.charAt(0) == '>')
					act = act.substring(1);

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
