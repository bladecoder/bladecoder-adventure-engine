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
package com.bladecoder.engine.model;

import java.util.ArrayList;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.serialization.ActionCallbackSerializer;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.serialization.BladeJson.Mode;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engine.util.EngineLogger;

public class Verb implements VerbRunner, Serializable {
	public static final String LOOKAT_VERB = "lookat";
	public static final String PICKUP_VERB = "pickup";
	public static final String ACTION_VERB = "action";
	public static final String LEAVE_VERB = "leave";
	public static final String TALKTO_VERB = "talkto";
	public static final String USE_VERB = "use";
	public static final String GOTO_VERB = "goto";
	public static final String TEST_VERB = "test";
	public static final String INIT_VERB = "init";
	public static final String INIT_NEW_GAME_VERB = "initNewGame";
	public static final String INIT_SAVED_GAME_VERB = "initSavedGame";

	private String id;
	private String state;
	private String target;
	private String icon;

	private final ArrayList<Action> actions = new ArrayList<>();

	private int ip = -1;
	private String currentTarget;

	private ActionCallback cb;

	public Verb() {
	}

	public Verb(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getHashKey() {
		String key = id;

		if (target != null)
			key = key + "." + target;

		if (state != null)
			key = key + "." + state;

		return key;
	}

	public void add(Action a) {
		actions.add(a);
	}

	@Override
	public ArrayList<Action> getActions() {
		return actions;
	}

	@Override
	public String getCurrentTarget() {
		return currentTarget;
	}

	@Override
	public void run(String currentTarget, ActionCallback cb) {
		this.currentTarget = currentTarget;
		this.cb = cb;

		if (EngineLogger.debugMode()) {
			StringBuilder sb = new StringBuilder(">>> Running verb: ").append(id);

			if (currentTarget != null) {
				sb.append(" currentTarget: " + currentTarget);
			}

			EngineLogger.debug(sb.toString());
		}

		ip = 0;
		nextStep();
	}

	public void nextStep() {

		boolean stop = false;

		while (!isFinished() && !stop) {
			Action a = actions.get(ip);

			if (EngineLogger.debugMode())
				EngineLogger.debug(ip + ". " + a.getClass().getSimpleName());

			try {
				if (a.run(this))
					stop = true;
				else
					ip++;
			} catch (Exception e) {
				EngineLogger.error(
						"EXCEPTION EXECUTING ACTION: " + a.getClass().getSimpleName() + " - " + e.getMessage(), e);
				ip++;
			}
		}

		if (ip == actions.size()) {
			EngineLogger.debug(">>> Verb FINISHED: " + id);

			if (cb != null) {
				ActionCallback cb2 = cb;
				cb = null;

				cb2.resume();
			}
		}
	}

	public boolean isFinished() {
		return ip >= actions.size() || ip < 0;
	}

	@Override
	public void resume() {
		ip++;
		nextStep();
	}

	@Override
	public int getIP() {
		return ip;
	}

	@Override
	public void setIP(int ip) {
		this.ip = ip;
	}

	@Override
	public void cancel() {
		ip = actions.size() + 1;

		for (Action c : actions) {
			if (c instanceof VerbRunner)
				((VerbRunner) c).cancel();
		}

		if (cb != null) {
			ActionCallback cb2 = cb;
			cb = null;

			cb2.resume();
		}

		EngineLogger.debug(">>> Verb CANCELLED: " + id);
	}

	@Override
	public void write(Json json) {

		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {
			json.writeValue("id", id);

			if (target != null)
				json.writeValue("target", target);

			if (state != null)
				json.writeValue("state", state);

			if (icon != null)
				json.writeValue("icon", icon);

			json.writeArrayStart("actions");
			for (Action a : actions) {
				ActionUtils.writeJson(a, json);
			}
			json.writeArrayEnd();
		} else {
			json.writeValue("ip", ip);

			if (cb != null)
				json.writeValue("cb", ActionCallbackSerializer.find(bjson.getWorld(), bjson.getScene(), cb));

			if (currentTarget != null)
				json.writeValue("currentTarget", currentTarget);

			json.writeArrayStart("actions");
			for (Action a : actions) {
				if (a instanceof Serializable) {
					json.writeObjectStart();
					((Serializable) a).write(json);
					json.writeObjectEnd();
				}
			}
			json.writeArrayEnd();
		}
	}

	@Override
	public void read(Json json, JsonValue jsonData) {

		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {
			id = json.readValue("id", String.class, jsonData);
			target = json.readValue("target", String.class, (String) null, jsonData);
			state = json.readValue("state", String.class, (String) null, jsonData);
			icon = json.readValue("icon", String.class, (String) null, jsonData);

			actions.clear();
			JsonValue actionsValue = jsonData.get("actions");
			for (int i = 0; i < actionsValue.size; i++) {
				JsonValue aValue = actionsValue.get(i);
				String clazz = aValue.getString("class");

				try {
					Action a = ActionUtils.readJson(bjson.getWorld(), json, aValue);
					actions.add(a);
				} catch (SerializationException e) {
					EngineLogger.error("Error loading action: " + clazz + " " + aValue.toString());
					throw e;
				}
			}
		} else {
			// MUTABLE
			currentTarget = json.readValue("currentTarget", String.class, (String) null, jsonData);
			ip = json.readValue("ip", Integer.class, jsonData);
			cb = ActionCallbackSerializer.find(bjson.getWorld(), bjson.getScene(),
					json.readValue("cb", String.class, jsonData));

			JsonValue actionsValue = jsonData.get("actions");

			int i = 0;

			for (Action a : actions) {
				if (a instanceof Serializable && i < actionsValue.size) {
					if (actionsValue.get(i) == null)
						break;

					((Serializable) a).read(json, actionsValue.get(i));
					i++;
				}
			}
		}
	}
}
