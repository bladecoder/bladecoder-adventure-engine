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
package com.bladecoder.engineeditor.common;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

public class Message {
	private static final float FADE_DURATION = 0.4f;
	private static Label msg;
	private static Skin skin;
	private static boolean isModal;

	public static void init(Skin skin) {
		msg = new Label("", skin, "message") {

			@Override
			public Actor hit(float x, float y, boolean touchable) {
				if (isModal)
					return this;

				return null;
			}
		};

		Message.skin = skin;
		msg.setWrap(true);
		msg.setAlignment(Align.center, Align.center);
	}

	public static void showMsg(Stage stage, String text) {
		showMsg(stage, text, false);
	}

	public static void showMsg(final Stage stage, final String text, final boolean modal) {

		Timer.post(new Task() {

			@Override
			public void run() {
				isModal = modal;

				if (text == null) {
					hideMsg();
					return;
				}

				add(stage, text);

				if (FADE_DURATION > 0) {
					msg.getColor().a = 0;
					msg.addAction(Actions.fadeIn(FADE_DURATION, Interpolation.fade));
				}

			}
		});
	}

	private static void add(Stage stage, String text) {
		msg.clearActions();

		msg.setText(text);

		GlyphLayout textLayout = new GlyphLayout();

		textLayout.setText(msg.getStyle().font, text, Color.BLACK, stage.getWidth() * .8f, Align.center, true);

		msg.setSize(textLayout.width + textLayout.height, textLayout.height + textLayout.height * 2);

		if (!stage.getActors().contains(msg, true))
			stage.addActor(msg);

		msg.setPosition(Math.round((stage.getWidth() - msg.getWidth()) / 2),
				Math.round((stage.getHeight() - msg.getHeight()) / 2));
		msg.invalidate();
	}

	public static void showMsg(final Stage stage, final String text, final float duration) {

		Timer.post(new Task() {

			@Override
			public void run() {
				isModal = false;

				if (text == null) {
					hideMsg();
					return;
				}

				add(stage, text);

				if (FADE_DURATION > 0) {
					msg.getColor().a = 0;
					msg.addAction(sequence(Actions.fadeIn(FADE_DURATION, Interpolation.fade), Actions.delay(duration,
							sequence(fadeOut(FADE_DURATION, Interpolation.fade), Actions.removeActor()))));
				}

			}
		});
	}

	public static void hideMsg() {
		isModal = false;
		msg.setText("");

		if (FADE_DURATION > 0) {
			msg.addAction(sequence(fadeOut(FADE_DURATION, Interpolation.fade), Actions.removeActor()));
		}
	}

	public static void showMsgDialog(final Stage stage, final String title, final String msg) {
		Timer.post(new Task() {

			@Override
			public void run() {
				new Dialog(title, skin).text(msg).button("Close", true).key(Keys.ENTER, true).key(Keys.ESCAPE, false)
						.show(stage);

			}
		});
	}
}
