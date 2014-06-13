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
package org.bladecoder.engineeditor.utils;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Align;

public class Message extends Label {
	static public float fadeDuration = 0.4f;

	public Message(Skin skin) {
		super("", skin, "message");
		
		setWrap(true);
		setAlignment(Align.center, Align.center);
	}
	
	public void show (Stage stage, String text) {
		clearActions();
		
		setText(text);
		
		TextBounds bounds2 = getStyle().font.getMultiLineBounds(text);
		
		setSize(bounds2.width + 20, bounds2.height * 2);

		stage.addActor(this);
		setPosition(Math.round((stage.getWidth() - getWidth()) / 2), Math.round((stage.getHeight() - getHeight()) / 2));
		invalidate();
		
		if (fadeDuration > 0) {
			getColor().a = 0;
			addAction(Actions.fadeIn(fadeDuration, Interpolation.fade));
		}
	}
	
	public void show (Stage stage, String text, float duration) {
		clearActions();
		
		setText(text);
		
		TextBounds bounds2 = getStyle().font.getMultiLineBounds(text);
		
		setSize(bounds2.width + 20, bounds2.height * 2);

		stage.addActor(this);
		setPosition(Math.round((stage.getWidth() - getWidth()) / 2), Math.round((stage.getHeight() - getHeight()) / 2));
		invalidate();
		
		if (fadeDuration > 0) {
			getColor().a = 0;
			addAction(sequence(Actions.fadeIn(fadeDuration, Interpolation.fade), 
					Actions.delay(duration, sequence(fadeOut(fadeDuration, Interpolation.fade), Actions.removeActor()))));
		}
	}

	public void hide () {
		setText("");
		
		if (fadeDuration > 0) {
			addAction(sequence(fadeOut(fadeDuration, Interpolation.fade), Actions.removeActor()));
		} else
			remove();
	}	

}
