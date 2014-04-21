package org.bladecoder.engine.model;

import java.text.MessageFormat;
import java.util.HashMap;

import org.bladecoder.engine.assets.AssetConsumer;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public class BaseActor implements Comparable<BaseActor>, Serializable, AssetConsumer {

	protected String id;
	protected String desc;
	/** visibility and interaction activation */
	private boolean interaction = true;
	private boolean visible = true;

	/** internal state. Can be used for actions to maintain a state machine */
	protected String state;

	protected static HashMap<String, Verb> defaultVerbs = new HashMap<String, Verb>();

	protected HashMap<String, Verb> verbs = new HashMap<String, Verb>();
	protected HashMap<String, SoundFX> sounds;
	protected HashMap<String, String> customProperties;
	
	private String playingSound;

	protected Rectangle bbox;
	
	private HashMap<String, Dialog> dialogs;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Rectangle getBBox() {
		return bbox;
	}

	public boolean isActive() {
		return (interaction && visible);
	}

	public void setActive(boolean active) {
		interaction = active;
		visible = active;
	}

	public boolean hasInteraction() {
		return interaction;
	}

	public void setInteraction(boolean interaction) {
		this.interaction = interaction;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public void setBbox(Rectangle bbox) {
		this.bbox = bbox;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public void addVerb(String id, Verb v) {
		verbs.put(id, v);
	}

	public static void addDefaultVerb(String id, Verb v) {
		defaultVerbs.put(id, v);
	}

	public Verb getVerb(String id) {
		return getVerb(id, null);
	}

	/**
	 * Returns an actor Verb.
	 * 
	 * Search order:
	 *   - id.target.state
	 *   - id.target
	 *   - id.state
	 *   - id
	 * 
	 * @param id Verb id
	 * @param target When an object is used by other object.
	 * @return
	 */
	public Verb getVerb(String id, String target) {
		StringBuilder sb = new StringBuilder();
		Verb v = null;
		
		if(target != null) {
			if(state != null) {
				sb.append(id).append(".").append(target).append(".").append(state);
				v = verbs.get(sb.toString()); // id.target.state
			}

			if (v == null) {
				sb.setLength(0);
				sb.append(id).append(".").append(target);
				v = verbs.get(sb.toString()); // id.target
			}
		}
		
		if (v == null && state != null) {
			sb.setLength(0);
			sb.append(id).append(".").append(state);
			
			v = verbs.get(sb.toString()); // id.state
		}

		if (v == null)
			v = verbs.get(id); // id

		return v;
	}
	
	public static HashMap<String, Verb> getDefaultVerbs() {
		return defaultVerbs;
	}

	public HashMap<String, Verb> getVerbs() {
		return verbs;
	}

	public void runVerb(String id) {
		runVerb(id, null);
	}

	/**
	 * Run Verb
	 * 
	 * @param verb Verb
	 * @param target When one object is used with another object.
	 */
	public void runVerb(String verb, String target) {

		Verb v = null;
			
		v = getVerb(verb, target);

		if (v == null) {
			v = defaultVerbs.get(verb);
		}

		if (v != null) {
			v.run();
		} else {
			EngineLogger.error(MessageFormat.format("Verb ''{0}'' not found for actor ''{1}'' and target ''{2}''",
					verb, id, target) );
		}
	}
	
	/**
	 * Cancels the execution of a running verb
	 * 
	 * @param verb
	 * @param target
	 */
	public void cancelVerb(String verb, String target) {
		Verb v = null;
		
		v = getVerb(verb, target);

		if (v == null) {
			v = defaultVerbs.get(verb);
		}

		if (v != null)
			v.cancel();
		else {
			EngineLogger.error(MessageFormat.format("Verb ''{0}'' not found for actor ''{1}'' and target ''{2}''",
					verb, id, target) );
		}	
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
	
	public void setCustomProperty(String name, String value) {
		if(customProperties == null)
			customProperties = new HashMap<String, String>();
		
		customProperties.put(name, value);
	}
	
	public String getCustomProperty(String name) {
		return customProperties.get(name);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("\nObject: ").append(id).append(", state: ").append(state);
		sb.append("\n  Desc: ").append(desc);
		sb.append("\n  BBox: ").append(getBBox().toString());
		sb.append("\n  Verbs:");

		for (String v : verbs.keySet()) {
			sb.append(" ").append(v);
		}

		sb.append("\n");

		return sb.toString();
	}

	@Override
	public int compareTo(BaseActor o) {
		return (int) (o.getBBox().getY() - this.getBBox().getY());
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
	
	public Dialog getDialog(String dialog) {
		return dialogs.get(dialog);
	}
	
	public void addDialog(String id, Dialog d) {
		if(dialogs == null)
			dialogs = new HashMap<String, Dialog> ();
		
		dialogs.put(id, d);
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
		json.writeValue("id", id);
		json.writeValue("interaction", interaction);
		json.writeValue("visible", visible);
		json.writeValue("desc", desc, desc == null ? null : desc.getClass());
		json.writeValue("verbs", verbs, verbs == null ? null : verbs.getClass());
		
		Rectangle bboxScaled = null;
		
		if(bbox != null) {
			float scale = EngineAssetManager.getInstance().getScale();
			
			bboxScaled = new Rectangle(bbox);
			bboxScaled.x /= scale;
			bboxScaled.y /= scale;
			bboxScaled.width /= scale;
			bboxScaled.height /= scale;
		}
		
		json.writeValue("bbox", bboxScaled, bboxScaled == null ? null : bboxScaled.getClass());
		json.writeValue("state", state, state == null ? null : desc.getClass());
		json.writeValue("sounds", sounds, sounds == null ? null : sounds.getClass());
		json.writeValue("playingSound", playingSound, playingSound == null ? null : playingSound.getClass());
		
		json.writeValue("customProperties", customProperties, customProperties == null ? null : customProperties.getClass());
		json.writeValue("dialogs", dialogs, dialogs == null ? null : dialogs.getClass());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read (Json json, JsonValue jsonData) {
		id = json.readValue("id", String.class, jsonData);
		interaction = json.readValue("interaction", Boolean.class, jsonData);
		visible = json.readValue("visible", Boolean.class, jsonData);
		desc = json.readValue("desc", String.class, jsonData);
		verbs = json.readValue("verbs", HashMap.class, Verb.class, jsonData);
		
		bbox = json.readValue("bbox", Rectangle.class, jsonData);
		if(bbox != null) {
			float scale = EngineAssetManager.getInstance().getScale();
			bbox.x *= scale;
			bbox.y *= scale;
			bbox.width *= scale;
			bbox.height *= scale;
		}
		
		state = json.readValue("state", String.class, jsonData);
		sounds = json.readValue("sounds", HashMap.class, SoundFX.class, jsonData);
		playingSound = json.readValue("playingSound", String.class, jsonData);
		customProperties = json.readValue("customProperties", HashMap.class, String.class, jsonData);
		dialogs = json.readValue("dialogs", HashMap.class, Dialog.class, jsonData);
	}

}
