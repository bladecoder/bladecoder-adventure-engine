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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.anim.WalkTween;
import com.bladecoder.engine.util.EngineLogger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class CharacterActor extends SpriteActor {
	private final static float DEFAULT_WALKING_SPEED = 700f; // Speed units:
																// pix/sec.
	
	public final static String DEFAULT_STAND_ANIM = "stand";
	public final static String DEFAULT_WALK_ANIM = "walk";
	public final static String DEFAULT_TALK_ANIM = "talk";

	@JsonProperty(defaultValue = "700")
	@JsonPropertyDescription("The walking speed in pix/sec")
	private float walkingSpeed = DEFAULT_WALKING_SPEED;

	private Color textColor;
	
	private String standAnim = DEFAULT_STAND_ANIM;
	public Color getTextColor() {
		return textColor;
	}

	public void setTextColor(Color textColor) {
		this.textColor = textColor;
	}

	public String getStandAnim() {
		return standAnim;
	}

	public void setStandAnim(String standAnim) {
		this.standAnim = standAnim;
	}

	public String getWalkAnim() {
		return walkAnim;
	}

	public void setWalkAnim(String walkAnim) {
		this.walkAnim = walkAnim;
	}

	public String getTalkAnim() {
		return talkAnim;
	}

	public void setTalkAnim(String talkAnim) {
		this.talkAnim = talkAnim;
	}

	private String walkAnim = DEFAULT_WALK_ANIM;
	private String talkAnim = DEFAULT_TALK_ANIM;
	
	private HashMap<String, Dialog> dialogs;
	
	public Dialog getDialog(String dialog) {
		return dialogs.get(dialog);
	}
	
	public void addDialog(String id, Dialog d) {
		if(dialogs == null)
			dialogs = new HashMap<String, Dialog> ();
		
		dialogs.put(id, d);
	}

	public void setWalkingSpeed(float s) {
		walkingSpeed = s;
	}

	public void lookat(Vector2 p) {		
		renderer.startAnimation(standAnim, Tween.Type.SPRITE_DEFINED,-1, null, new Vector2(bbox.getX(), bbox.getY()), p);
		
		posTween = null;
	}

	public void lookat(String direction) {
		renderer.startAnimation(standAnim, Tween.Type.SPRITE_DEFINED,-1, null, direction);
		posTween = null;
	}

	public void stand() {
		renderer.startAnimation(standAnim, Tween.Type.SPRITE_DEFINED,-1, null, null);
		posTween = null;
	}
	
	public void talk() {
		renderer.startAnimation(talkAnim, Tween.Type.SPRITE_DEFINED,-1, null, null);
		posTween = null;
	}

	public void startWalkAnim(Vector2 p0, Vector2 pf) {
		renderer.startAnimation(walkAnim, Tween.Type.SPRITE_DEFINED,-1, null, p0, pf);
	}

	/**
	 * Walking Support
	 * 
	 * @param pf
	 *            Final position to walk
	 * @param cb
	 *            The action callback
	 */
	public void goTo(Vector2 pf, ActionCallback cb) {
		EngineLogger.debug(MessageFormat.format("GOTO {0},{1}", pf.x, pf.y));

		Vector2 p0 = new Vector2(bbox.getX(), bbox.getY());

		ArrayList<Vector2> walkingPath = null;

		//
		if (p0.dst(pf) < 2.0f) {
			setPosition(pf.x, pf.y);

			// call the callback
			if (cb != null)
				ActionCallbackQueue.add(cb);

			return;
		}

		if (scene.getPolygonalNavGraph() != null) {
			walkingPath = scene.getPolygonalNavGraph().findPath(p0.x, p0.y, pf.x, pf.y);
		}

		if (walkingPath == null || walkingPath.size() == 0) {
			// call the callback even when the path is empty
			if (cb != null)
				ActionCallbackQueue.add(cb);

			return;
		}

		posTween = new WalkTween();

		((WalkTween) posTween).start(this, walkingPath, walkingSpeed, cb);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());

		sb.append("  Walking Speed: ").append(walkingSpeed);
		sb.append("\nText Color: ").append(textColor);

		return sb.toString();
	}

	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("walkingSpeed", walkingSpeed);
		json.writeValue("textColor", textColor);
		json.writeValue("dialogs", dialogs);
		json.writeValue("standAnim", standAnim);
		json.writeValue("walkAnim", walkAnim);
		json.writeValue("talkAnim", talkAnim);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		walkingSpeed = json.readValue("walkingSpeed", Float.class, jsonData);
		textColor = json.readValue("textColor", Color.class, jsonData);
		dialogs = json.readValue("dialogs", HashMap.class, Dialog.class, jsonData);
		standAnim = json.readValue("standAnim", String.class, jsonData);
		walkAnim = json.readValue("walkAnim", String.class, jsonData);
		talkAnim = json.readValue("talkAnim", String.class, jsonData);
	}

}