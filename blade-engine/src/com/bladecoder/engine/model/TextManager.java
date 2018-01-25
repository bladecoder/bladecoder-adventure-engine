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
import java.util.LinkedList;
import java.util.Queue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.util.Config;

/**
 * TextManager maintains a fifo for the character subtitles.
 * 
 * For now, only one subtitle is displayed in the screen.
 * 
 * A subtitle is cut in pieces and quee. Each piece has is own time in screen.
 * 
 * 
 * @author rgarcia
 * 
 */
public class TextManager implements Serializable {
	public static final float POS_CENTER = -1f;
	public static final float POS_SUBTITLE = -2f;
	public static final float RECT_MARGIN = 18f;
	public static final float RECT_BORDER = 2f;

	public static final boolean AUTO_HIDE_TEXTS = Config.getProperty(Config.AUTO_HIDE_TEXTS, true);

	private float inScreenTime;
	private Text currentText = null;
	private final VoiceManager voiceManager = new VoiceManager(this);

	private Queue<Text> fifo;

	public TextManager() {
		fifo = new LinkedList<Text>();
	}

	public void addText(String str, float x, float y, boolean quee, Text.Type type, Color color, String font,
			String actorId, String voiceId, ActionCallback cb) {

		if (str.charAt(0) == I18N.PREFIX)
			str = I18N.getString(str.substring(1));

		String s = str.replace("\\n", "\n");
		String[] text = s.split("\n\n");

		if (!quee)
			clear();
		
		String lineVoiceId = voiceId;

		for (int i = 0; i < text.length; i++) {
			String cutStr = text[i];

			// search for embedded duration in the string ex:
			// "2#two seconds subtitle"
			float duration = 0;
			String finalStr = cutStr;

			int idx = cutStr.indexOf('#');
			if (idx != -1) {
				String prefix = cutStr.substring(0, idx);

				if (prefix.charAt(0) == 'v') {
					lineVoiceId = prefix.substring(1).trim();
				} else {
					duration = Float.parseFloat(prefix);
				}
				
				finalStr = cutStr.substring(idx + 1);
			}

			Text sub;

			sub = new Text(finalStr, x, y, duration, type, color, font, actorId, lineVoiceId, i == text.length - 1?cb:null);
			
			// resets voice id for the next line
			lineVoiceId = null;

			fifo.add(sub);
		}

		if (!quee || currentText == null) {
			if (currentText != null) {
				next();
			} else {
				setCurrentText(fifo.poll());
			}
		}

	}

	public VoiceManager getVoiceManager() {
		return voiceManager;
	}

	public Text getCurrentText() {
		return currentText;
	}

	private void setCurrentText(Text t) {
		inScreenTime = 0f;
		currentText = t;

		if (t != null)
			voiceManager.play(t.voiceId);
		else
			voiceManager.stop();
	}

	public void update(float delta) {

		if (currentText == null) {
			return;
		}

		inScreenTime += delta;

		if (inScreenTime > currentText.time && AUTO_HIDE_TEXTS) {
			next();
		}
	}

	public void next() {
		if (currentText != null) {

			currentText.callCb();

			setCurrentText(fifo.poll());
		}
	}

	private void clear() {
		// CLEAR FIFO
		while (currentText != null)
			next();

		inScreenTime = 0;
		voiceManager.stop();
	}

	/**
	 * Put manager in the init state. Use it when changing current scene
	 */
	public void reset() {
		inScreenTime = 0;
		fifo.clear();
		currentText = null;
		voiceManager.stop();
	}

	@Override
	public void write(Json json) {
		json.writeValue("inScreenTime", inScreenTime);

		if (currentText != null)
			json.writeValue("currentText", currentText);

		json.writeValue("fifo", new ArrayList<Text>(fifo), ArrayList.class, Text.class);
		json.writeValue("voiceManager", voiceManager);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		inScreenTime = json.readValue("inScreenTime", Float.class, jsonData);
		currentText = json.readValue("currentText", Text.class, jsonData);
		fifo = new LinkedList<Text>(json.readValue("fifo", ArrayList.class, Text.class, jsonData));

		JsonValue jsonValue = jsonData.get("voiceManager");

		if (jsonValue != null)
			voiceManager.read(json, jsonValue);
	}
}
