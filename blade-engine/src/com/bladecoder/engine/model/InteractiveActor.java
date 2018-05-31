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

import java.util.HashMap;
import java.util.Map.Entry;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.util.SerializationHelper;
import com.bladecoder.engine.util.SerializationHelper.Mode;

/**
 * An InteractiveActor is any object in a scene or in the inventory that has
 * user interaction.
 * 
 * @author rgarcia
 */
public class InteractiveActor extends BaseActor implements Comparable<InteractiveActor> {
	protected String desc;
	protected float zIndex;
	protected boolean interaction = true;

	/** internal state. Can be used for actions to maintain a state machine */
	protected String state;

	protected final VerbManager verbs = new VerbManager();

	/**
	 * State to know when the player is inside this actor to trigger the enter/exit
	 * verbs
	 */
	private boolean playerInside = false;

	protected String layer;

	/**
	 * Characters use this point to walk to the actor.
	 */
	private final Vector2 refPoint = new Vector2();

	public void setLayer(String layer) {
		this.layer = layer;
	}

	public String getLayer() {
		return layer;
	}

	/**
	 * @return Is visible and has interaction
	 */
	public boolean canInteract() {
		return interaction && visible;
	}

	public boolean getInteraction() {
		return interaction;
	}

	public void setInteraction(boolean interaction) {
		this.interaction = interaction;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public Vector2 getRefPoint() {
		return refPoint;
	}

	public void setRefPoint(float x, float y) {
		refPoint.set(x, y);
	}

	public VerbManager getVerbManager() {
		return verbs;
	}

	@Override
	public void update(float delta) {
		InteractiveActor player = null;

		if (scene != null)
			player = scene.getPlayer();

		if (visible && player != null) {
			boolean hit = hit(player.getX(), player.getY());
			if (!hit && playerInside) {
				// the player leaves
				playerInside = false;

				Verb v = getVerb("exit");
				if (v != null)
					v.run(null, null);
			} else if (hit && !playerInside) {
				// the player enters
				playerInside = true;

				Verb v = getVerb("enter");
				if (v != null)
					v.run(null, null);
			}
		}
	}

	public Verb getVerb(String id) {
		return verbs.getVerb(id, state, null);
	}

	public Verb getVerb(String id, String target) {
		return verbs.getVerb(id, state, target);
	}

	public void runVerb(String id) {
		verbs.runVerb(id, state, null, null);
	}

	public void runVerb(String id, String target) {
		verbs.runVerb(id, state, target, null);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(super.toString());
		sb.append("  State: ").append(state);
		sb.append("\n  Desc: ").append(desc);
		sb.append("\n  Verbs:");

		for (String v : verbs.getVerbs().keySet()) {
			sb.append(" ").append(v);
		}

		sb.append("\n");

		return sb.toString();
	}

	public float getZIndex() {
		return zIndex;
	}

	public void setZIndex(float z) {
		zIndex = z;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Override
	public int compareTo(InteractiveActor o) {
		return (int) (o.getBBox().getY() - this.getBBox().getY());
	}

	@Override
	public void write(Json json) {
		super.write(json);

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {
			json.writeValue("desc", desc);

			float worldScale = EngineAssetManager.getInstance().getScale();
			json.writeValue("refPoint", new Vector2(getRefPoint().x / worldScale, getRefPoint().y / worldScale));
		} else {
			json.writeValue("playerInside", playerInside);
		}

		verbs.write(json);
		json.writeValue("interaction", interaction);
		json.writeValue("state", state);
		json.writeValue("zIndex", zIndex);
		json.writeValue("layer", layer);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {
			desc = json.readValue("desc", String.class, jsonData);
			layer = json.readValue("layer", String.class, jsonData);

			Vector2 r = json.readValue("refPoint", Vector2.class, jsonData);

			if (r != null) {
				float worldScale = EngineAssetManager.getInstance().getScale();
				getRefPoint().set(r.x * worldScale, r.y * worldScale);
			}

			// Load actor sounds for backwards compatibility. 
			@SuppressWarnings("unchecked")
			HashMap<String, SoundDesc> sounds = json.readValue("sounds", HashMap.class, SoundDesc.class, jsonData);

			if (sounds != null) {
				for (Entry<String, SoundDesc> e : sounds.entrySet()) {
					e.getValue().setId(id + "_" + e.getKey());
					World.getInstance().getSounds().put(id + "_" + e.getKey(), e.getValue());
				}
			}

		} else {

			playerInside = json.readValue("playerInside", boolean.class, false, jsonData);
			String newLayer = json.readValue("layer", String.class, jsonData);

			if (newLayer != null && !newLayer.equals(layer)) {
				if (scene != null) {
					if (scene.getLayer(layer).remove(this))
						scene.getLayer(newLayer).add(this);
				}

				layer = newLayer;
			}
		}

		verbs.read(json, jsonData);
		interaction = json.readValue("interaction", boolean.class, interaction, jsonData);
		state = json.readValue("state", String.class, jsonData);
		zIndex = json.readValue("zIndex", float.class, zIndex, jsonData);
	}

}
