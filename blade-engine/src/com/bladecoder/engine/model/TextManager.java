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
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.Text.Type;
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

	public static final boolean AUTO_HIDE_TEXTS = Config.getInstance().getProperty(Config.AUTO_HIDE_TEXTS, true);

	private float inScreenTime;
	private Text currentText = null;
	private final VoiceManager voiceManager = new VoiceManager(this);

	private Queue<Text> fifo;
	private Scene scene;

	/**
	 * Stores the previous animation to restore it when the character ends his talk.
	 */
	private String previousCharacterAnim = null;

	public TextManager(Scene s) {
		fifo = new LinkedList<>();
		this.scene = s;
	}

	public void addText(String str, float x, float y, boolean queue, Text.Type type, Color color, String font,
			String actorId, String voiceId, String talkAnimation, ActionCallback cb) {

		if (str.charAt(0) == I18N.PREFIX)
			str = scene.getWorld().getI18N().getString(str.substring(1));

		String s = str.replace("\\n", "\n");

		if (type == Text.Type.UI && scene.getWorld().getListener() != null) {

			Text t = new Text(s, x, y, 0, type, color, font, actorId, voiceId, talkAnimation, null);

			scene.getWorld().getListener().text(t);

			if (cb != null) {
				ActionCallback tmpcb = cb;
				cb = null;
				tmpcb.resume();
			}

			return;
		}

		String[] text = s.split("\n\n");

		int nQueued = fifo.size();

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

			sub = new Text(finalStr, x, y, duration, type, color, font, actorId, lineVoiceId, talkAnimation,
					i == text.length - 1 ? cb : null);

			// resets voice id for the next line
			lineVoiceId = null;

			fifo.add(sub);
		}

		if (!queue)
			clear(nQueued);

		if (!queue || currentText == null) {
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
		if (currentText != null && (currentText.type == Type.TALK || currentText.animation != null)
				&& currentText.actorId != null) {
			CharacterActor a = (CharacterActor) scene.getActor(currentText.actorId, false);

			// restore previous stand animation
			if (a != null && previousCharacterAnim != null)
				a.startAnimation(previousCharacterAnim, Tween.Type.SPRITE_DEFINED, 0, null);

			previousCharacterAnim = null;
		}

		inScreenTime = 0f;
		currentText = t;

		if (t != null) {
			voiceManager.play(t.voiceId);

			// Start talk animation
			if ((t.type == Type.TALK || t.animation != null) && t.actorId != null) {
				CharacterActor a = (CharacterActor) scene.getActor(t.actorId, false);

				previousCharacterAnim = ((AnimationRenderer) a.getRenderer()).getCurrentAnimationId();

				if (t.animation != null)
					a.startAnimation(t.animation, Tween.Type.SPRITE_DEFINED, 0, null);
				else
					a.talk();
			}
		} else {
			voiceManager.stop();
		}

		if (scene.getWorld().getListener() != null)
			scene.getWorld().getListener().text(t);
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
			Text t = currentText;

			setCurrentText(fifo.poll());

			t.callCb();
		}
	}

	private void clear(int n) {
		// CLEAR
		for (int i = 0; i < n; i++)
			next();

		inScreenTime = 0;
		voiceManager.stop();
	}

	/**
	 * Put manager in the init state. Used when changing current scene
	 */
	public void reset() {
		fifo.clear();

		setCurrentText(null);
	}

	@Override
	public void write(Json json) {
		json.writeValue("inScreenTime", inScreenTime);

		if (currentText != null)
			json.writeValue("currentText", currentText);

		json.writeValue("fifo", new ArrayList<>(fifo), ArrayList.class, Text.class);
		json.writeValue("voiceManager", voiceManager);

		if (previousCharacterAnim != null)
			json.writeValue("previousAnim", previousCharacterAnim);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		inScreenTime = json.readValue("inScreenTime", Float.class, jsonData);
		currentText = json.readValue("currentText", Text.class, jsonData);
		fifo = new LinkedList<>(json.readValue("fifo", ArrayList.class, Text.class, jsonData));
		previousCharacterAnim = json.readValue("previousAnim", String.class, jsonData);

		JsonValue jsonValue = jsonData.get("voiceManager");

		if (jsonValue != null)
			voiceManager.read(json, jsonValue);
	}
}
