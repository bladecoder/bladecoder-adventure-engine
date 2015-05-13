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
package com.bladecoder.engine.actions;

import java.util.HashMap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.World;

public class SayAction extends BaseCallbackAction {
	public static final String INFO = "Says a text";
	public static final Param[] PARAMS = {
			new Param("actor", "The target actor", Type.ACTOR, false),
			new Param("text", "The 'text' to show", Type.STRING),
			new Param(
					"pos",
					"The position of the text. If null, the position will be calc based in actor",
					Type.VECTOR2),
			new Param("speech", "The 'soundId' to play if selected",
					Type.STRING),
			new Param(
					"wait",
					"If this param is 'false' the text is showed and the action continues inmediatly",
					Type.BOOLEAN, true),
			new Param(
					"type",
					"The type of the text: 'talk', 'rectangle' (default) and 'plain'",
					Type.STRING, true, "rectangle", new String[] { "rectangle",
							"talk", "plain" }), 
			new Param("animation",	"The animation to put when talking instead the default talk animation.",
									Type.STRING),
			new Param("quee", "Quee the text if other text is showing or show it inmediatly.",Type.BOOLEAN, false, "false")
	};
	
	private String soundId;
	private String text;

	private String actorId;

	private Text.Type type = Text.Type.RECTANGLE;

	private String previousAnim = null;
	private String talkAnim = null;
	private Vector2 pos = null;
	private boolean quee = false;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");

		soundId = params.get("speech");
		text = params.get("text");
		talkAnim = params.get("animation");
		
		if(talkAnim == null)
			talkAnim = AnimationDesc.TALK_ANIM;

		if (params.get("wait") != null) {
			setWait(Boolean.parseBoolean(params.get("wait")));
		}

		if (params.get("type") != null) {
			if (params.get("type").equals("talk")) {
				type = Text.Type.TALK;
			} else if (params.get("type").equals("rectangle")) {
				type = Text.Type.RECTANGLE;
			} else if (params.get("type").equals("plain")) {
				type = Text.Type.PLAIN;
			}
		}

		if (params.get("pos") != null) {
			pos = Param.parseVector2(params.get("pos"));
		}
		
		if (params.get("quee") != null) {
			quee = Boolean.parseBoolean(params.get("quee"));
		}
	}

	@Override
	public boolean run(ActionCallback cb) {
		setVerbCb(cb);	
		BaseActor actor = World.getInstance().getCurrentScene()
				.getActor(actorId, false);

		if (type == Text.Type.TALK)
			restoreStandPose((SpriteActor) actor);

		if (soundId != null)
			actor.playSound(soundId);

		if (text != null) {
			float x, y;

			if (pos != null) {
				x = pos.x;
				y = pos.y;
			} else {

				if (type == Text.Type.RECTANGLE) {
					x = y = TextManager.POS_SUBTITLE;
				} else {
					// WorldCamera c = World.getInstance().getCamera();
					// Vector3 p = c.scene2screen(pos.x, pos.y +
					// ((SpriteActor)actor).getHeight());

					x = actor.getX();
					y = actor.getY()
							+ actor.getBBox().getBoundingRectangle()
									.getHeight();
					// quee = true;
				}
			}

			if (type == Text.Type.TALK) {
				startTalkAnim((SpriteActor)actor, talkAnim);
			}

			World.getInstance().getTextManager()
						.addSubtitle(text, x, y, quee, type, Color.BLACK, getWait()?this:null);
		}
		
		return getWait();
	}

	@Override
	public void resume() {
		if (this.type == Text.Type.TALK) {
			SpriteActor actor = (SpriteActor) World.getInstance()
					.getCurrentScene().getActor(actorId, false);
			actor.startAnimation(previousAnim, Tween.FROM_FA, 0, null);
		}

		super.resume();
	}

	private void restoreStandPose(SpriteActor a) {
		if (a == null)
			return;

		String fa = a.getRenderer().getCurrentAnimationId();

		if (fa.startsWith(talkAnim)) { // If the actor was already talking we
										// restore the actor to the 'stand' pose
			int idx = fa.indexOf('.');
			String prevFA = AnimationDesc.STAND_ANIM + fa.substring(idx);
			a.startAnimation(prevFA, null);
		}
	}

	private void startTalkAnim(SpriteActor a, String talkAnim) {
		previousAnim = a.getRenderer().getCurrentAnimationId();
		
		String s = talkAnim;
		
		String l = s + "." + AnimationDesc.LEFT;
		String r = s + "." + AnimationDesc.RIGHT;
		
		if (previousAnim.endsWith(AnimationDesc.LEFT)) {
			if(a.getRenderer().getAnimations().get(l) != null || a.getRenderer().getAnimations().get(r) != null)
				s = l;
		} else if (previousAnim.endsWith(AnimationDesc.RIGHT)) {
			if(a.getRenderer().getAnimations().get(l) != null || a.getRenderer().getAnimations().get(r) != null)
				s = r;
		}
		
		a.startAnimation(s, null);
	}

	@Override
	public void write(Json json) {

		json.writeValue("soundId", soundId);
		json.writeValue("text", text);
		json.writeValue("actorId", actorId);
		json.writeValue("previousAnim", previousAnim);
		json.writeValue("type", type);
		json.writeValue("talkAnim", talkAnim);
		json.writeValue("pos", pos);
		json.writeValue("quee", quee);
		super.write(json);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		soundId = json.readValue("soundId", String.class, jsonData);
		text = json.readValue("text", String.class, jsonData);
		actorId = json.readValue("actorId", String.class, jsonData);
		previousAnim = json.readValue("previousAnim", String.class, jsonData);
		type = json.readValue("type", Text.Type.class, jsonData);
		talkAnim = json.readValue("talkAnim", String.class, jsonData);
		pos = json.readValue("pos", Vector2.class, jsonData);
		quee = json.readValue("quee", Boolean.class, jsonData);
		super.read(json, jsonData);
	}

	@Override
	public String getInfo() {
		return INFO;
	}

	@Override
	public Param[] getParams() {
		return PARAMS;
	}
}
