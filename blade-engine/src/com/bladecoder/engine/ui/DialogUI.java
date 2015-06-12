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
package com.bladecoder.engine.ui;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.DPIUtils;

public class DialogUI extends Actor {
	public static final String DIALOG_END_COMMAND = "dialog_end";
	
	private DialogUIStyle style;

	private Recorder recorder;

	private int selected = -1;
	
	private final GlyphLayout layout = new GlyphLayout();

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
		
		if(isVisible()) {
			setWidth(getStage().getViewport().getScreenWidth());
			setHeight(calcHeight());		
		}
	}

	@Override
	public void draw(Batch batch, float alpha) {
		if(World.getInstance().getCurrentDialog() == null || World.getInstance().inCutMode())
			return;
		
		ArrayList<DialogOption> options = World.getInstance()
				.getCurrentDialog().getVisibleOptions();

		if (options.size() == 0)
			return;
		
		else if (options.size() == 1) { // If only has one option,
										// autoselect it
			select(0);
			return;
		}

		float margin = DPIUtils.getMarginSize();
		float y = margin;		

		if (style.background != null) {
			style.background.draw(batch, getX(), getY(), getWidth(), getHeight());
		}

		for (int i = options.size() - 1; i >= 0; i--) {
			DialogOption o = options.get(i);
			String str = o.getText();

			if (str.charAt(0) == '@')
				str = I18N.getString(str.substring(1));

			if (i == selected) {
				layout.setText(style.font, str, style.overFontColor, getWidth() - margin * 2, Align.left, true);
			} else {
				layout.setText(style.font, str, style.fontColor, getWidth() - margin * 2, Align.left, true);			
			}

			y += layout.height - style.font.getDescent() + style.font.getAscent();
			style.font.draw(batch, layout, margin, y);
		}
	}
	
	private float calcHeight() {
		float height = 0;
		float margin = DPIUtils.getMarginSize();
		
		ArrayList<DialogOption> options = World.getInstance()
				.getCurrentDialog().getVisibleOptions();
		
		for (int i = 0; i < options.size(); i++) {
			DialogOption o = options.get(i);
			String str = o.getText();

			if (str.charAt(0) == '@')
				str = I18N.getString(str.substring(1));
			layout.setText(style.font, str, style.overFontColor, getStage().getViewport().getScreenWidth() - margin * 2, Align.left, true);
			height += layout.height - style.font.getDescent() + style.font.getAscent();
		}
		
		return height + margin * 2;
	}

	private int getOption(float x, float y) {
		if(World.getInstance().getCurrentDialog() == null)
			return -1;
		
		ArrayList<DialogOption> options = World.getInstance()
				.getCurrentDialog().getVisibleOptions();
		
		float margin = DPIUtils.getMarginSize();
		float oy = margin;
		
		int selectedLine = 0;
		
		for (int i = options.size() - 1; i >= 0; i--) {
			DialogOption o = options.get(i);
			String str = o.getText();

			if (str.charAt(0) == '@')
				str = I18N.getString(str.substring(1));
			
			layout.setText(style.font, str, style.overFontColor, getStage().getViewport().getScreenWidth() - margin * 2, Align.left, true);
			oy += layout.height - style.font.getDescent() + style.font.getAscent();
			
			if(oy > y) {
				selectedLine = i;
				break;
			}
		}

		return selectedLine;
	}

	private void select(int i) {
		// RECORD
		if (recorder.isRecording()) {
			recorder.add(i);
		}

		World.getInstance().selectDialogOption(i);
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
