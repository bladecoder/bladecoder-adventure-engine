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

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.serialization.BladeJson.Mode;

public class DialogOption implements Serializable {
	private String text;
	private String responseText;
	private String verbId;
	private String next;
	private boolean visible = true;
	private boolean once = false;
	private String voiceId;
	private String responseVoiceId;

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public String getVerbId() {
		return verbId;
	}

	public void setVerbId(String verbId) {
		this.verbId = verbId;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getVoiceId() {
		return voiceId;
	}

	public void setVoiceId(String voiceId) {
		this.voiceId = voiceId;
	}

	public String getResponseVoiceId() {
		return responseVoiceId;
	}

	public void setResponseVoiceId(String soundId) {
		this.responseVoiceId = soundId;
	}

	public void setResponseText(String responseText) {
		this.responseText = responseText;
	}

	public String getResponseText() {
		return responseText;
	}

	public String getNext() {
		return next;
	}

	public void setNext(String next) {
		this.next = next;
	}

	public boolean isOnce() {
		return once;
	}

	public void setOnce(boolean once) {
		this.once = once;
	}

	@Override
	public void write(Json json) {

		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {
			json.writeValue("text", text);
			json.writeValue("responseText", responseText);
			json.writeValue("verbId", verbId);
			json.writeValue("next", next);
			json.writeValue("once", once);
			json.writeValue("soundId", voiceId);
			json.writeValue("responseSoundId", responseVoiceId);
		} else {

		}

		json.writeValue("visible", visible);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {

		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {
			text = json.readValue("text", String.class, jsonData);
			responseText = json.readValue("responseText", String.class, jsonData);
			verbId = json.readValue("verbId", String.class, jsonData);
			next = json.readValue("next", String.class, jsonData);
			once = json.readValue("once", Boolean.class, jsonData);
			voiceId = json.readValue("soundId", String.class, jsonData);
			responseVoiceId = json.readValue("responseSoundId", String.class, jsonData);
		} else {

		}

		visible = json.readValue("visible", boolean.class, false, jsonData);
	}
}
