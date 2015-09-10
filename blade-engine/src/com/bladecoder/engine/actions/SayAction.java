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
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.World;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ActionDescription("Says a text")
public class SayAction extends BaseCallbackAction {
	@JsonProperty("actor")
	@JsonPropertyDescription("The target actor")
	@ActionPropertyType(Type.ACTOR)
	private String actorId;

	@JsonProperty
	@JsonPropertyDescription("The 'text' to show")
	@ActionPropertyType(Type.SMALL_TEXT)
	private String text;

	@JsonProperty("speech")
	@JsonPropertyDescription("The 'soundId' to play if selected")
	@ActionPropertyType(Type.SOUND)
	private String soundId;

	@JsonProperty(required = true, defaultValue = "RECTANGLE")
	@JsonPropertyDescription("The type of the text.")
	@ActionPropertyType(Type.STRING)
	private Text.Type type = Text.Type.RECTANGLE;

	@JsonProperty(defaultValue = "false")
	@JsonPropertyDescription("Queue the text if other text is showing, or show it immediately.")
	@ActionPropertyType(Type.BOOLEAN)
	private boolean queue = false;

	private String previousAnim = null;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");

		soundId = params.get("speech");
		text = params.get("text");

		if (params.get("wait") != null) {
			setWait(Boolean.parseBoolean(params.get("wait")));
		}

		final String strType = params.get("type");
		if (strType != null) {
			this.type = Text.Type.valueOf(strType.trim().toUpperCase());
		}

		if (params.get("quee") != null) {
			queue = Boolean.parseBoolean(params.get("quee"));
		}
	}

	@Override
	public boolean run(ActionCallback cb) {
		float x, y;
		Color color = null;

		setVerbCb(cb);
		InteractiveActor actor = (InteractiveActor)World.getInstance().getCurrentScene().getActor(actorId, false);

		if (soundId != null)
			actor.playSound(soundId);

		if (text != null) {
			if (type != Text.Type.TALK) {
				x = y = TextManager.POS_SUBTITLE;
			} else {
				x = actor.getX();
				y = actor.getY() + actor.getBBox().getBoundingRectangle().getHeight();
				
				color = ((CharacterActor)actor).getTextColor();
				
				restoreStandPose((CharacterActor)actor);
				startTalkAnim((CharacterActor)actor);
			}

			World.getInstance().getTextManager().addText(text, x, y, queue, type, color, null,
					this);
		}

		return getWait();
	}

	@Override
	public void resume() {
		if (type == Text.Type.TALK) {
			CharacterActor actor = (CharacterActor) World.getInstance().getCurrentScene().getActor(actorId, false);
			actor.startAnimation(previousAnim, Tween.Type.SPRITE_DEFINED, 0, null);
		}

		super.resume();
	}

	private void restoreStandPose(CharacterActor a) {
		if (a == null)
			return;

		String fa = a.getRenderer().getCurrentAnimationId();

		// If the actor was already talking we restore the actor to the 'stand'
		// pose
		if (fa.startsWith(a.getTalkAnim())) {
			a.stand();
		}
	}

	private void startTalkAnim(CharacterActor a) {
		previousAnim = a.getRenderer().getCurrentAnimationId();

		a.talk();
	}

	@Override
	public void write(Json json) {

		json.writeValue("soundId", soundId);
		json.writeValue("text", text);
		json.writeValue("actorId", actorId);
		json.writeValue("previousAnim", previousAnim);
		json.writeValue("type", type);
		json.writeValue("quee", queue);
		super.write(json);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		soundId = json.readValue("soundId", String.class, jsonData);
		text = json.readValue("text", String.class, jsonData);
		actorId = json.readValue("actorId", String.class, jsonData);
		previousAnim = json.readValue("previousAnim", String.class, jsonData);
		type = json.readValue("type", Text.Type.class, jsonData);
		queue = json.readValue("quee", Boolean.class, jsonData);
		super.read(json, jsonData);
	}

}
