package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

public class ActionFactory {

	private static final HashMap<String, String> actions;

	static {
		actions = new HashMap<String, String>();
		
		actions.put("lookat",
				"org.bladecoder.engine.actions.LookAtAction");
		actions.put("pickup",
				"org.bladecoder.engine.actions.PickUpAction");
		actions.put("goto",
				"org.bladecoder.engine.actions.GotoAction");
		actions.put("leave",
				"org.bladecoder.engine.actions.LeaveAction");	

		actions.put("set_state",
				"org.bladecoder.engine.actions.SetStateAction");
		actions.put("set_active",
				"org.bladecoder.engine.actions.SetActiveAction");
		actions.put("set_cutmode",
				"org.bladecoder.engine.actions.SetCutmodeAction");			
		actions.put("set_frame_animation",
				"org.bladecoder.engine.actions.FrameAnimationAction");
		actions.put("pos_animation",
				"org.bladecoder.engine.actions.PosAnimationAction");
		actions.put("remove_inventory_item",
				"org.bladecoder.engine.actions.RemoveInventoryItemAction");	
		actions.put("say",
				"org.bladecoder.engine.actions.SayAction");	
		actions.put("drop_item",
				"org.bladecoder.engine.actions.DropItemAction");
		actions.put("wait",
				"org.bladecoder.engine.actions.WaitAction");
		actions.put("talkto",
				"org.bladecoder.engine.actions.TalktoAction");
		actions.put("dialog_option",
				"org.bladecoder.engine.actions.DialogOptionAction");
		actions.put("say_dialog",
				"org.bladecoder.engine.actions.SayDialogAction");
		actions.put("run_verb",
				"org.bladecoder.engine.actions.RunVerbAction");
		actions.put("cancel_verb",
				"org.bladecoder.engine.actions.CancelVerbAction");
		actions.put("sound",
				"org.bladecoder.engine.actions.SoundAction");
		actions.put("music",
				"org.bladecoder.engine.actions.MusicAction");
		actions.put("overlay",
				"org.bladecoder.engine.actions.OverlayAction");
		actions.put("camera",
				"org.bladecoder.engine.actions.CameraAction");
		actions.put("transition",
				"org.bladecoder.engine.actions.TransitionAction");
	}
	
	public static String []getActionList() {
		return  actions.keySet().toArray(new String[actions.size()]);
	}

	public static Action create(String name,
			HashMap<String, String> params) {
		String className = actions.get(name);

		if (className == null) {
			EngineLogger.error( "Action with name '" + name
					+ "' not found.");

			return null;
		}

		return ActionFactory.createByClass(className, params);
	}	
	
	
	
	public static Action createByClass(String className,
			HashMap<String, String> params) {

		Action a = null;

		try {
			Class<?> c = ClassReflection.forName(className);
			a = (Action) ClassReflection.newInstance(c);
			
			if(params != null)
				a.setParams(params);
		} catch (ReflectionException e) {
			EngineLogger.error(e.getMessage());
		}

		return a;
	}
}
