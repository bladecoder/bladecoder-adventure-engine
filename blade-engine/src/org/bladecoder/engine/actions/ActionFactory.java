/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

public class ActionFactory {

	private static final HashMap<String, String> actions = new HashMap<String, String>();

	static {
		
		actions.put("Lookat",
				"org.bladecoder.engine.actions.LookAtAction");
		actions.put("Pickup",
				"org.bladecoder.engine.actions.PickUpAction");
		actions.put("Goto",
				"org.bladecoder.engine.actions.GotoAction");
		actions.put("Leave",
				"org.bladecoder.engine.actions.LeaveAction");	

		actions.put("SetState",
				"org.bladecoder.engine.actions.SetStateAction");
		actions.put("SetActive",
				"org.bladecoder.engine.actions.SetActiveAction");
		actions.put("Cutmode",
				"org.bladecoder.engine.actions.SetCutmodeAction");
		actions.put("ShowInventory",
				"org.bladecoder.engine.actions.ShowInventoryAction");			
		actions.put("Animation",
				"org.bladecoder.engine.actions.FrameAnimationAction");
		actions.put("Position",
				"org.bladecoder.engine.actions.PosAnimationAction");
		actions.put("RemoveInventoryItem",
				"org.bladecoder.engine.actions.RemoveInventoryItemAction");	
		actions.put("Say",
				"org.bladecoder.engine.actions.SayAction");	
		actions.put("DropItem",
				"org.bladecoder.engine.actions.DropItemAction");
		actions.put("Wait",
				"org.bladecoder.engine.actions.WaitAction");
		actions.put("Talkto",
				"org.bladecoder.engine.actions.TalktoAction");
		actions.put("DialogOption",
				"org.bladecoder.engine.actions.DialogOptionAction");
		actions.put("SayDialog",
				"org.bladecoder.engine.actions.SayDialogAction");
		actions.put("RunVerb",
				"org.bladecoder.engine.actions.RunVerbAction");
		actions.put("CancelVerb",
				"org.bladecoder.engine.actions.CancelVerbAction");
		actions.put("Sound",
				"org.bladecoder.engine.actions.SoundAction");
		actions.put("Music",
				"org.bladecoder.engine.actions.MusicAction");
		actions.put("Camera",
				"org.bladecoder.engine.actions.CameraAction");
		actions.put("Transition",
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
