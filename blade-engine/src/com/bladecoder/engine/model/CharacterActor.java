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
import com.bladecoder.engine.anim.SpritePosTween;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.anim.WalkTween;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.SerializationHelper;
import com.bladecoder.engine.util.SerializationHelper.Mode;

public class CharacterActor extends SpriteActor {
	public final static float DEFAULT_WALKING_SPEED = 1000f; // Speed units:
																// pix/sec.

	public final static String DEFAULT_STAND_ANIM = "stand";
	public final static String DEFAULT_WALK_ANIM = "walk";
	public final static String DEFAULT_TALK_ANIM = "talk";

	private float walkingSpeed = DEFAULT_WALKING_SPEED;
	private Color textColor;

	private String standAnim = DEFAULT_STAND_ANIM;
	private String walkAnim = DEFAULT_WALK_ANIM;
	private String talkAnim = DEFAULT_TALK_ANIM;

	private HashMap<String, Dialog> dialogs;

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

	public Dialog getDialog(String dialog) {
		return dialogs.get(dialog);
	}

	public void addDialog(Dialog d) {
		if (dialogs == null)
			dialogs = new HashMap<String, Dialog>();

		dialogs.put(d.getId(), d);
	}

	public void setWalkingSpeed(float s) {
		walkingSpeed = s;
	}

	public float getWalkingSpeed() {
		return walkingSpeed;
	}

	public void lookat(Vector2 p) {
		if(!(renderer instanceof AnimationRenderer)) 
			return;
		
		inAnim();
		removeTween(SpritePosTween.class);
		((AnimationRenderer)renderer).startAnimation(standAnim, Tween.Type.SPRITE_DEFINED, -1, null, new Vector2(bbox.getX(), bbox.getY()),
				p);
		outAnim(Tween.Type.SPRITE_DEFINED);
	}

	public void lookat(String direction) {
		if(!(renderer instanceof AnimationRenderer)) 
			return;
		
		inAnim();
		removeTween(SpritePosTween.class);
		((AnimationRenderer)renderer).startAnimation(standAnim, Tween.Type.SPRITE_DEFINED, -1, null, direction);
		outAnim(Tween.Type.SPRITE_DEFINED);
	}

	public void stand() {
		if(!(renderer instanceof AnimationRenderer)) 
			return;
		
		inAnim();
		removeTween(SpritePosTween.class);
		((AnimationRenderer)renderer).startAnimation(standAnim, Tween.Type.SPRITE_DEFINED, -1, null, null);
		outAnim(Tween.Type.SPRITE_DEFINED);
	}

	public void talk() {
		if(!(renderer instanceof AnimationRenderer)) 
			return;
		
		inAnim();
		removeTween(SpritePosTween.class);
		((AnimationRenderer)renderer).startAnimation(talkAnim, Tween.Type.SPRITE_DEFINED, -1, null, null);
		outAnim(Tween.Type.SPRITE_DEFINED);
	}

	public void startWalkAnim(Vector2 p0, Vector2 pf) {
		if(!(renderer instanceof AnimationRenderer)) 
			return;
		
		inAnim();
		((AnimationRenderer)renderer).startAnimation(walkAnim, Tween.Type.SPRITE_DEFINED, -1, null, p0, pf);
		outAnim(Tween.Type.SPRITE_DEFINED);
	}

	/**
	 * Walking Support
	 * 
	 * @param pf
	 *            Final position to walk
	 * @param cb
	 *            The action callback
	 */
	public void goTo(Vector2 pf, ActionCallback cb, boolean ignoreWalkZone) {
		EngineLogger.debug(MessageFormat.format("GOTO {0},{1}", pf.x, pf.y));

		Vector2 p0 = new Vector2(bbox.getX(), bbox.getY());

		// stop previous movement
		if (tweens.size() > 0) {
			removeTween(SpritePosTween.class);
			stand();
		}

		ArrayList<Vector2> walkingPath = null;

		//
		if (p0.dst(pf) < 2.0f) {
			setPosition(pf.x, pf.y);

			// call the callback
			if (cb != null)
				ActionCallbackQueue.add(cb);

			return;
		}

		if (scene.getPolygonalNavGraph() != null && !ignoreWalkZone) {
			walkingPath = scene.getPolygonalNavGraph().findPath(p0.x, p0.y, pf.x, pf.y);
		} else {
			walkingPath = new ArrayList<Vector2>(2);
			walkingPath.add(p0);
			walkingPath.add(new Vector2(pf));
		}

		if (walkingPath == null || walkingPath.size() == 0) {
			// call the callback even when the path is empty
			if (cb != null)
				ActionCallbackQueue.add(cb);

			return;
		}

		WalkTween t = new WalkTween();

		t.start(this, walkingPath, walkingSpeed, cb);
		addTween(t);
	}

	/**
	 * If the character is walking, the character position is set to the final
	 * position and the walk is finish.
	 * 
	 * This is used to fast walk between scenes. Used when double clicking.
	 */
	public void fastWalk() {	
		for(Tween<SpriteActor> t: tweens) {	
			if(t instanceof WalkTween) {
				WalkTween wt = (WalkTween) t;
				wt.completeNow(this);
				wt = null;
				break;
			}
		}
	}

	public HashMap<String, Dialog> getDialogs() {
		return dialogs;
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
		json.writeValue("dialogs", dialogs, HashMap.class, Dialog.class);

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {
		} else {
			json.writeValue("standAnim", standAnim);
			json.writeValue("walkAnim", walkAnim);
			json.writeValue("talkAnim", talkAnim);
		}

		json.writeValue("walkingSpeed", walkingSpeed);
		json.writeValue("textColor", textColor);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {
			dialogs = json.readValue("dialogs", HashMap.class, Dialog.class, jsonData);

			if (dialogs != null) {
				for (Dialog d : dialogs.values())
					d.setActor(id);
			}

		} else {
			if (dialogs != null) {
				JsonValue dialogsValue = jsonData.get("dialogs");

				for (Dialog d : dialogs.values()) {
					String id = d.getId();
					JsonValue dValue = dialogsValue.get(id);

					if (dValue != null)
						d.read(json, dValue);
				}
			}

			standAnim = json.readValue("standAnim", String.class, jsonData);
			walkAnim = json.readValue("walkAnim", String.class, jsonData);
			talkAnim = json.readValue("talkAnim", String.class, jsonData);
		}

		walkingSpeed = json.readValue("walkingSpeed", float.class, walkingSpeed, jsonData);
		textColor = json.readValue("textColor", Color.class, jsonData);
	}

}