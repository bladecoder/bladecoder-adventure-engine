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
package com.bladecoder.engine.serialization;

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.ink.InkManager.InkVerbRunner;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Inventory;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.UIActors;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

/**
 * 
 * Helper class to serialize ActionCallbacks. An ActionCallback is an Action or
 * a Verb.
 * 
 * This class can generate an String from an ActionCallback to save to a file
 * and then locate the ActionCallback based in the generated String.
 * 
 * The String generated to locate an ActionCallback is like:
 * 
 * For verbs: actorId#verbId For actions: actorId#verbId#actionPos
 * 
 * If actorId == "DEFAULT_VERB" the ActionCallback is searched in the World
 * default verbs. If actorId == current scene ID the ActionCallback is searched
 * in the current scene verbs.
 * 
 * @author rgarcia
 */
public class ActionCallbackSerializer {

	private static final String SEPARATION_SYMBOL = "#";
	private static final String INK_MANAGER_TAG = "INK_MANAGER";
	private static final String UIACTORS_TAG = "UIACTORS";
	private static final String INVENTORY_TAG = "INVENTORY";
	private static final String DEFAULT_VERB_TAG = "DEFAULT_VERB";

	private static String find(ActionCallback cb, Verb v) {
		String id = v.getHashKey();

		if (cb == v)
			return id;

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

	private static String find(ActionCallback cb, InteractiveActor a) {
		if (a == null)
			return null;

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
		if (s == null)
			return null;

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

	private static String find(ActionCallback cb, InkVerbRunner im) {
		if (im == null)
			return null;

		if (cb instanceof InkVerbRunner) {
			if (cb == im) {
				return INK_MANAGER_TAG;
			} else {
				EngineLogger.debug("CB pointing to an old InkVerbRunner. IGNORING.");
				return null;
			}
		}

		int pos = 0;

		for (Action a : im.getActions()) {
			if (cb == a) {
				StringBuilder stringBuilder = new StringBuilder(INK_MANAGER_TAG);
				stringBuilder.append(SEPARATION_SYMBOL).append(pos);

				return stringBuilder.toString();
			}

			pos++;
		}

		return null;
	}

	private static String find(ActionCallback cb, UIActors uia) {
		if (uia == null)
			return null;

		for (InteractiveActor a : uia.getActors()) {
			String id = find(cb, a);

			if (id != null) {
				StringBuilder stringBuilder = new StringBuilder(UIACTORS_TAG);
				stringBuilder.append(SEPARATION_SYMBOL).append(id);

				return stringBuilder.toString();
			}
		}

		return null;
	}

	private static String find(ActionCallback cb, Inventory inv) {
		for (int i = 0; i < inv.getNumItems(); i++) {
			InteractiveActor a = inv.get(i);
			String id = find(cb, a);

			if (id != null) {
				StringBuilder stringBuilder = new StringBuilder(INVENTORY_TAG);
				stringBuilder.append(SEPARATION_SYMBOL).append(id);

				return stringBuilder.toString();
			}
		}

		return null;
	}

	/**
	 * Generates a String for serialization that allows locate the ActionCallback
	 * 
	 * @param cb The ActionCallback to serialize
	 * @return The generated location string
	 */
	public static String find(World w, Scene s, ActionCallback cb) {
		String id = null;

		if (cb == null)
			return null;

		// search in UIActors
		id = find(cb, w.getUIActors());

		if (id != null)
			return id;

		// search in inventory
		id = find(cb, w.getInventory());

		if (id != null)
			return id;

		// search in inkManager actions
		id = find(cb, w.getInkManager().getVerbRunner());

		if (id != null)
			return id;

		// search in scene verbs
		id = find(cb, s);

		if (id != null)
			return id;

		// search in player
		id = find(cb, s.getPlayer());
		if (id != null)
			return id;

		// search in actors
		for (BaseActor a : s.getActors().values()) {
			if (!(a instanceof InteractiveActor))
				continue;

			id = find(cb, (InteractiveActor) a);
			if (id != null)
				return id;
		}

		// search in worldVerbs
		for (Verb v : w.getVerbManager().getVerbs().values()) {
			id = find(cb, v);
			if (id != null) {
				StringBuilder stringBuilder = new StringBuilder(DEFAULT_VERB_TAG);
				stringBuilder.append(SEPARATION_SYMBOL).append(id);

				return stringBuilder.toString();
			}
		}

		return null;
	}

	/**
	 * Searches for the ActionCallback represented by the id string.
	 * 
	 * @param id
	 */
	public static ActionCallback find(World w, Scene s, String id) {

		if (id == null)
			return null;

		String[] split = id.split(SEPARATION_SYMBOL);

		if (id.startsWith(INK_MANAGER_TAG)) {
			if (split.length == 1)
				return w.getInkManager().getVerbRunner();

			int actionPos = Integer.parseInt(split[1]);
			Action action = w.getInkManager().getVerbRunner().getActions().get(actionPos);

			if (action instanceof ActionCallback)
				return (ActionCallback) action;
		}

		if (split.length < 2)
			return null;

		String actorId;
		String verbId;
		int actionPos = -1;

		if (id.startsWith(UIACTORS_TAG) || id.startsWith(INVENTORY_TAG)) {

			actorId = split[1];
			verbId = split[2];

			if (split.length > 3)
				actionPos = Integer.parseInt(split[3]);
		} else {
			actorId = split[0];
			verbId = split[1];

			if (split.length > 2)
				actionPos = Integer.parseInt(split[2]);
		}

		Verb v = null;

		if (actorId.equals(DEFAULT_VERB_TAG)) {
			v = w.getVerbManager().getVerb(verbId, null, null);
		} else {

			InteractiveActor a;

			if (actorId.equals(s.getId())) {
				v = s.getVerbManager().getVerbs().get(verbId);
			} else {
				a = (InteractiveActor) s.getActor(actorId, true);

				if (a == null) {
					EngineLogger.error("ActionCallbackSerialization - Actor not found: " + actorId + " cb: " + id);
					return null;
				}

				v = a.getVerbManager().getVerbs().get(verbId);
			}
		}

		if (v == null) {
			EngineLogger.error("ActionCallbackSerialization - Verb not found: " + verbId + " cb: " + id);

			return null;
		}

		if (actionPos == -1)
			return v;

		Action action = v.getActions().get(actionPos);

		if (action instanceof ActionCallback)
			return (ActionCallback) action;

		EngineLogger.error("ActionCallbackSerialization - CB not found: " + id);

		return null;
	}
}
