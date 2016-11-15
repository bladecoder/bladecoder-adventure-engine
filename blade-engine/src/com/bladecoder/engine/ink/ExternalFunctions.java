package com.bladecoder.engine.ink;

import java.util.HashMap;

import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionFactory;
import com.bladecoder.engine.actions.ActorAnimationRef;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.ink.runtime.Story.ExternalFunction;

public class ExternalFunctions {
	
	private InkManager inkManager;
	
	public ExternalFunctions() {
	}

	public void bindExternalFunctions(InkManager ink) throws Exception {
		
		this.inkManager = ink;
		
		inkManager.getStory().bindExternalFunction("cutMode", new ExternalFunction() {

			@Override
			public Object call(Object[] args) throws Exception {
				String value = inkManager.getStory().tryCoerce(args[0], String.class);
				
				HashMap<String, String> params = new HashMap<String, String>();

				params.put("value", value);

				try {
					Action action = ActionFactory.createByClass("com.bladecoder.engine.actions.SetCutmodeAction", params);
					inkManager.getActions().add(action);
				} catch (ClassNotFoundException | ReflectionException e) {
					EngineLogger.error(e.getMessage(), e);
				}


				return null;
			}
		});
		
		inkManager.getStory().bindExternalFunction("goto", new ExternalFunction() {

			@Override
			public Object call(Object[] args) throws Exception {
				
				HashMap<String, String> params = new HashMap<String, String>();

				params.put("actor", args[0].toString());
				params.put("target", args[1].toString());

				try {
					Action action = ActionFactory.createByClass("com.bladecoder.engine.actions.GotoAction", params);
					inkManager.getActions().add(action);
				} catch (ClassNotFoundException | ReflectionException e) {
					EngineLogger.error(e.getMessage(), e);
				}


				return null;
			}
		});
		
		inkManager.getStory().bindExternalFunction("animation", new ExternalFunction() {

			@Override
			public Object call(Object[] args) throws Exception {
				
				HashMap<String, String> params = new HashMap<String, String>();

				params.put("animation", new ActorAnimationRef(args[0].toString(), args[1].toString()).toString());
				params.put("wait", inkManager.getStory().tryCoerce(args[2], String.class));

				try {
					Action action = ActionFactory.createByClass("com.bladecoder.engine.actions.AnimationAction", params);
					inkManager.getActions().add(action);
				} catch (ClassNotFoundException | ReflectionException e) {
					EngineLogger.error(e.getMessage(), e);
				}


				return null;
			}
		});
		
		inkManager.getStory().bindExternalFunction("setModelProp", new ExternalFunction() {

			@Override
			public Object call(Object[] args) throws Exception {
				
				World.getInstance().setModelProp(args[0].toString(), args[1].toString());

				return null;
			}
		});
		
		inkManager.getStory().bindExternalFunction("getModelProp", new ExternalFunction() {

			@Override
			public Object call(Object[] args) throws Exception {
				
				return World.getInstance().getModelProp(args[0].toString());
			}
		});
	}
}
