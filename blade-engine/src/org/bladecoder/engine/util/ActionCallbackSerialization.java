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
package org.bladecoder.engine.util;

import org.bladecoder.engine.actions.Action;
import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.Verb;
import org.bladecoder.engine.model.VerbManager;
import org.bladecoder.engine.model.World;

public class ActionCallbackSerialization {
	public static final String SEPARATION_SYMBOL = "#";

	private static String find(ActionCallback cb, Verb v) {
		String id = v.getId();

		int pos = 0;

		for (Action a : v.getActions()) {
			if (cb == a) {
				StringBuilder stringBuilder = new StringBuilder(id);
				stringBuilder.append(SEPARATION_SYMBOL).append(pos);

				return stringBuilder.toString();
			}

			pos++;
		}

		return null;
	}

	private static String find(ActionCallback cb, Actor a) {
		if(a == null) return null;
		
		String id = a.getId();

		for (Verb v : a.getVerbManager().getVerbs().values()) {
			String result = find(cb, v);

			if (result != null) {
				StringBuilder stringBuilder = new StringBuilder(id);
				stringBuilder.append(SEPARATION_SYMBOL).append(result);

				return stringBuilder.toString();
			}
		}

		return null;
	}
	
	private static String find(ActionCallback cb, Scene s) {
		if(s == null) return null;
		
		String id = s.getId();

		for (Verb v : s.getVerbManager().getVerbs().values()) {
			String result = find(cb, v);

			if (result != null) {
				StringBuilder stringBuilder = new StringBuilder(id);
				stringBuilder.append(SEPARATION_SYMBOL).append(result);

				return stringBuilder.toString();
			}
		}

		return null;
	}

	public static String find(ActionCallback cb) {
		String id = null;

		if (cb == null)
			return null;

		// search in scene verbs
		Scene s = World.getInstance().getCurrentScene();

		id = find(cb, s);

		if (id != null)
			return id;

		id = find(cb, s.getPlayer());
		if (id != null)
			return id;

		// search in actors
		for (Actor a : s.getActors().values()) {
			id = find(cb, a);
			if (id != null)
				return id;
		}

		// search in defaultVerbs
		for (Verb v : VerbManager.getDefaultVerbs().values()) {
			id = find(cb, v);
			if (id != null) {
				StringBuilder stringBuilder = new StringBuilder("DEFAULT_VERB");
				stringBuilder.append(SEPARATION_SYMBOL).append(id);

				return stringBuilder.toString();
			}
		}

		return null;
	}

	public static ActionCallback find(String id) {
		Scene s = World.getInstance().getCurrentScene();

		String[] split = id.split(SEPARATION_SYMBOL);

		if (split.length < 3)
			return null;

		String actorId = split[0];
		String verbId = split[1];
		int actionPos = Integer.parseInt(split[2]);

		
		Verb v = null;
		
		if (actorId.equals("DEFAULT_VERB")) {

			v = VerbManager.getDefaultVerbs().get(verbId);
		} else {

			Actor a;

			if (actorId.equals(s.getId())) {
				v = s.getVerbManager().getVerbs().get(verbId);
			} else {
				a = s.getActor(actorId, true);

				if (a == null)
					return null;
				
				v = a.getVerbManager().getVerbs().get(verbId);
			}
		}

		if (v == null)
			return null;

		Action action = v.getActions().get(actionPos);

		if (action instanceof ActionCallback)
			return (ActionCallback) action;

		return null;
	}
}
