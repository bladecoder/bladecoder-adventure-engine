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

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.SerializationHelper;
import com.bladecoder.engine.util.SerializationHelper.Mode;

/**
 * An InteractiveActor is any object in a scene or in the inventory that has
 * user interaction.
 * 
 * @author rgarcia
 */
public class InteractiveActor extends BaseActor implements AssetConsumer, Comparable<InteractiveActor> {
	protected String desc;
	protected float zIndex;
	protected boolean interaction = true;

	/** internal state. Can be used for actions to maintain a state machine */
	protected String state;

	protected final VerbManager verbs = new VerbManager();
	private HashMap<String, SoundFX> sounds;
	private String playingSound;

	/**
	 * State to know when the player is inside this actor to trigger the
	 * enter/exit verbs
	 */
	private boolean playerInside = false;

	protected String layer;

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

	public HashMap<String, SoundFX> getSounds() {
		return sounds;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public VerbManager getVerbManager() {
		return verbs;
	}

	@Override
	public void update(float delta) {
		InteractiveActor player = scene.getPlayer();
		if (visible && player != null) {
			boolean hit = hit(player.getX(), player.getY());
			if (!hit && playerInside) {
				// the player leaves
				playerInside = false;

				Verb v = getVerb("exit");
				if (v != null)
					v.run();
			} else if (hit && !playerInside) {
				// the player enters
				playerInside = true;

				Verb v = getVerb("enter");
				if (v != null)
					v.run();
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
		verbs.runVerb(id, state, null);
	}

	public void runVerb(String id, String target) {
		verbs.runVerb(id, state, target);
	}

	public void addSound(String id, String filename, boolean loop, float volume) {
		if (sounds == null)
			sounds = new HashMap<String, SoundFX>();

		sounds.put(id, new SoundFX(filename, loop, volume));
	}
	
	public void addSound(SoundFX s) {
		if (sounds == null)
			sounds = new HashMap<String, SoundFX>();

		sounds.put(s.getFilename(), s);
	}

	public void playSound(String id) {
		if (sounds == null)
			return;

		SoundFX s = sounds.get(id);

		if (s != null) {
			if (playingSound != null) {
				SoundFX s2 = sounds.get(playingSound);
				s2.stop();
			}

			s.play();
			playingSound = id;
		} else {
			EngineLogger.debug("Sound Not Found: " + s);
		}
	}

	public void stopCurrentSound() {
		if (playingSound == null)
			return;

		SoundFX s = sounds.get(playingSound);

		if (s != null) {
			s.stop();
		}

		playingSound = null;
	}
	
	public String getPlayingSound() {
		return playingSound;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

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
	public void loadAssets() {
		if (sounds != null) {
			for (SoundFX s : sounds.values()) {
				s.loadAssets();
			}
		}
	}

	@Override
	public void retrieveAssets() {
		if (sounds != null) {
			for (SoundFX s : sounds.values()) {
				s.retrieveAssets();
			}

			if (playingSound != null && sounds.get(playingSound).getLoop() == true) {
				playSound(playingSound);
			} else {
				playingSound = null;
			}
		}
	}

	@Override
	public void dispose() {
		if (sounds != null) {		
			for (SoundFX s : sounds.values()) {
				s.dispose();
			}
		}
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
			json.writeValue("sounds", sounds, sounds == null ? null : sounds.getClass(), SoundFX.class);
		} else {
			json.writeValue("playingSound", playingSound);
			json.writeValue("playerInside", playerInside);
		}
		
		verbs.write(json);
		json.writeValue("interaction", interaction);
		json.writeValue("state", state);
		json.writeValue("zIndex", zIndex);
		json.writeValue("layer", layer);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {
			desc = json.readValue("desc", String.class, jsonData);
			sounds = json.readValue("sounds", HashMap.class, SoundFX.class, jsonData);
			layer = json.readValue("layer", String.class, jsonData);
		} else {
			playingSound = json.readValue("playingSound", String.class, jsonData);
			playerInside = json.readValue("playerInside", boolean.class, false, jsonData);
			String newLayer = json.readValue("layer", String.class, jsonData);
			
			if(newLayer != null && !newLayer.equals(layer)) {
				scene.getLayer(layer).remove(this);
				scene.getLayer(newLayer).add(this);
				layer = newLayer;
			}
		}
		
		verbs.read(json, jsonData);
		interaction = json.readValue("interaction", boolean.class, interaction, jsonData);
		state = json.readValue("state", String.class, jsonData);			
		zIndex = json.readValue("zIndex", float.class, zIndex, jsonData);
	}

}
