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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ModelDescription;
import com.bladecoder.engine.actions.ModelPropertyType;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.assets.AssetConsumer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * An InteractiveActor is any object in a scene or in the inventory that has user interaction.
 * 
 * @author rgarcia
 */
@JsonTypeName("background")
@ModelDescription("Background actors don't have sprites or animations. They are used to make objects drawn in the background interactive")
public class InteractiveActor extends BaseActor implements AssetConsumer, VerbContainer {
	@JsonProperty
	@JsonPropertyDescription("True when the actor reacts to the user input")
	protected boolean interaction = true;

	@JsonProperty
	@JsonPropertyDescription("The text shown when the cursor is over the actor")
	@ModelPropertyType(Param.Type.STRING)
	protected String desc;

	/** internal state. Can be used for actions to maintain a state machine */
	@JsonProperty
	@JsonPropertyDescription("Initial state of the actor. Actors can be in different states during the game")
	@ModelPropertyType(Param.Type.STRING)
	protected String state;

	@JsonProperty
	@JsonPropertyDescription("The order to draw")
	protected float zIndex;

	protected VerbManager verbManager = new VerbManager();

	@JsonProperty
	private Collection<Verb> getVerbs() {
		return verbManager.getVerbs().values();
	}

	private void setVerbs(Collection<Verb> verbs) {
		for (Verb verb : verbs) {
			this.verbManager.addVerb(verb);
		}
	}

	protected Map<String, SoundFX> sounds = new HashMap<>();

	@JsonProperty
	private Collection<SoundFX> getSounds() {
		return sounds.values();
	}

	private void setSounds(Collection<SoundFX> sounds) {
		this.sounds = new HashMap<>();

		for (SoundFX sound : sounds) {
			this.sounds.put(sound.getId(), sound);
		}
	}

	private String playingSound;
	
	/** State to know when the player is inside this actor to trigger the enter/exit verbs */ 
	private boolean playerInside = false;


	public boolean hasInteraction() {
		return interaction && visible;
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

	@Override
	public VerbManager getVerbManager() {
		return verbManager;
	}
	
	@Override
	public void update(float delta) {
		InteractiveActor player = scene.getPlayer();
		if(visible && player != null) {
			boolean hit = hit(player.getX(), player.getY());
			if(!hit && playerInside) {
				// the player leaves
				playerInside = false;
				
				Verb v = getVerb(Verb.EXIT_VERB);
				if(v!=null)
					v.run();
			} else if(hit && !playerInside){
				// the player enters
				playerInside = true;
				
				Verb v = getVerb(Verb.ENTER_VERB);
				if(v!=null)
					v.run();				
			}
		}
	}

	public Verb getVerb(String id) {
		return verbManager.getVerb(id, state, null);
	}

	public Verb getVerb(String id, String target) {
		return verbManager.getVerb(id, state, target);
	}
	
	public void runVerb(String id) {
		verbManager.runVerb(id, state, null);
	}
	
	public void runVerb(String id, String target) {
		verbManager.runVerb(id, state, target);
	}

	public void addSound(String id, String filename, boolean loop, float volume) {
		if (sounds == null)
			sounds = new HashMap<String, SoundFX>();

		sounds.put(id, new SoundFX(filename, loop, volume));
	}

	public void playSound(String id) {
		if(sounds == null) return;
		
		SoundFX s = sounds.get(id);

		if (s != null) {
			if(playingSound != null) {
				SoundFX s2 = sounds.get(playingSound);
				s2.stop();
			}
			
			s.play();
			playingSound = id;
		}
	}
	
	public void stopSound(String id) {
		if(sounds == null) return;
		
		SoundFX s = sounds.get(id);

		if (s != null) {
			s.stop();
		}
		
		playingSound = null;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append(super.toString());
		sb.append("  State: ").append(state);
		sb.append("\n  Desc: ").append(desc);
		sb.append("\n  Verbs:");

		for (String v : verbManager.getVerbs().keySet()) {
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
			
			if(playingSound != null && sounds.get(playingSound).isLooping() == true) {
				playSound(playingSound);
			}
		}
	}	

	@Override
	public void dispose() {
		if (sounds != null) {
			for (SoundFX s : sounds.values()) {
				s.dispose();
			}

			sounds.clear();
			sounds = null;
			playingSound = null;
		}
	}

	@Override
	public void write(Json json) {
		super.write(json);
		
		json.writeValue("interaction", interaction);
		json.writeValue("desc", desc);
		json.writeValue("verbs", verbManager);

		json.writeValue("state", state);
		json.writeValue("sounds", sounds, sounds == null ? null : sounds.getClass(), SoundFX.class);
		json.writeValue("playingSound", playingSound, playingSound == null ? null : playingSound.getClass());
		
		json.writeValue("playerInside", playerInside);
		json.writeValue("zIndex", zIndex);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read (Json json, JsonValue jsonData) {
		super.read(json, jsonData);
		
		interaction = json.readValue("interaction", Boolean.class, jsonData);
		desc = json.readValue("desc", String.class, jsonData);
		verbManager = json.readValue("verbs", VerbManager.class, jsonData);
		
		state = json.readValue("state", String.class, jsonData);
		sounds = json.readValue("sounds", HashMap.class, SoundFX.class, jsonData);
		playingSound = json.readValue("playingSound", String.class, jsonData);

		playerInside = json.readValue("playerInside", Boolean.class, jsonData);
		zIndex = json.readValue("zIndex", Float.class, jsonData);
	}

}
