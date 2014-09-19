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
package org.bladecoder.engine.ui;

import java.util.ArrayList;

import org.bladecoder.engine.i18n.I18N;
import org.bladecoder.engine.model.Dialog;
import org.bladecoder.engine.model.DialogOption;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.DPIUtils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class DialogUI extends Actor {
	public static final String DIALOG_END_COMMAND = "dialog_end";
	
	private DialogUIStyle style;

	private Recorder recorder;

	private int selected = -1;

	public DialogUI(SceneScreen scr) {
		style = scr.getUI().getSkin().get(DialogUIStyle.class);		
		this.recorder = scr.getRecorder();

		addListener(new InputListener() {
			public void touchUp(InputEvent event, float x, float y,
					int pointer, int button) {
				int i = getOption(x, y);

				if (i >= 0) {
					select(i);
				}
			}

			public boolean mouseMoved(InputEvent event, float x, float y) {
				selected = getOption(x, y);

				return true;
			}

			public void touchDragged(InputEvent event, float x, float y,
					int pointer) {
				selected = getOption(x, y);
			}
			
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				return true;
			}
		});

		setPosition(0, 0);
	}
	
	@Override
	public void act(float delta) {
		super.act(delta);

		if(isVisible() && (World.getInstance().getCurrentDialog() == null || World.getInstance().inCutMode())) {
			setVisible(false);
			selected = -1;
		} else if( !isVisible() &&  World.getInstance().getCurrentDialog() != null && !World.getInstance().inCutMode()) {
			setVisible(true);
		}
	}

	@Override
	public void draw(Batch batch, float alpha) {
		
		ArrayList<DialogOption> options = World.getInstance()
				.getCurrentDialog().getVisibleOptions();

		if (options.size() == 0)
			return;
		
		else if (options.size() == 1) { // If only has one option,
										// autoselect it
			select(0);
			return;
		}

		float lineHeight = style.font.getLineHeight();
		float y = lineHeight * options.size();
		setWidth(getStage().getViewport().getScreenWidth());
		float margin = DPIUtils.getMarginSize(getStage().getViewport().getScreenWidth(), getStage().getViewport().getScreenWidth());
		setHeight( margin + lineHeight * options.size());

		if (style.background != null) {
			style.background.draw(batch, getX(), getY(), getWidth(), getHeight());
		}

		for (int i = 0; i < options.size(); i++) {
			DialogOption o = options.get(i);
			String str = o.getText();

			if (str.charAt(0) == '@')
				str = I18N.getString(str.substring(1));

			if (i == selected) {
				style.font.setColor(style.overFontColor);
				style.font.draw(batch, str, margin, y);
			} else {
				style.font.setColor(style.fontColor);
				style.font.draw(batch, str, margin, y);
			}

			y -= lineHeight;
		}
	}

	private int getOption(float x, float y) {
		float lineHeight = style.font.getLineHeight();

		int selectedLine = (int) (y / lineHeight);

		return World.getInstance().getCurrentDialog().getNumVisibleOptions()
				- selectedLine - 1;
	}

	private void select(int i) {
		Dialog d = World.getInstance().getCurrentDialog();

		// RECORD
		if (recorder.isRecording()) {
			recorder.add(i);
		}

		d.selectOption(i);

		if (World.getInstance().getCurrentDialog().ended()) {
			World.getInstance().setCurrentDialog(null);
			selected = -1;
			setVisible(false);
		}
	}
	
	/** The style for the DialogUI */
	static public class DialogUIStyle {
		/** Optional. */
		public Drawable background;
		public BitmapFont font;
		public Color fontColor;
		public Color overFontColor;

		public DialogUIStyle () {
		}

		public DialogUIStyle (DialogUIStyle style) {
			background = style.background;
			font = style.font;
			fontColor = style.fontColor;
			overFontColor = style.overFontColor;
		}
	}
}
