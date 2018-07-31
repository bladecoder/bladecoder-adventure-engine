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

import java.util.List;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.util.DPIUtils;

public class DialogUI extends ScrollPane {
	public static final String DIALOG_END_COMMAND = "dialog_end";

	private DialogUIStyle style;

	private Recorder recorder;

	private Table panel;

	private Button up;
	private Button down;

	private List<String> choices;

	private final UI ui;

	public DialogUI(UI ui) {
		super(new Table(ui.getSkin()), ui.getSkin());

		this.ui = ui;

		setFadeScrollBars(true);
		setOverscroll(false, false);

		up = new Button(ui.getSkin(), "dialog-up");
		down = new Button(ui.getSkin(), "dialog-down");

		panel = (Table) getActor();
		style = ui.getSkin().get(DialogUIStyle.class);
		this.recorder = ui.getRecorder();

		if (style.background != null)
			panel.setBackground(style.background);

		panel.top().left();
		panel.pad(DPIUtils.getMarginSize());

		setVisible(false);
		panel.defaults().expandX().fillX().top().left().padBottom(DPIUtils.getSpacing());

		addListener(new EventListener() {

			@Override
			public boolean handle(Event event) {
				if (isScrollY()) {

					if (getScrollPercentY() > 0f && up.isVisible() == false) {
						up.setVisible(true);
					} else if (getScrollPercentY() == 0f && up.isVisible() == true) {
						up.setVisible(false);
					}

					if (getScrollPercentY() < 1f && down.isVisible() == false) {
						down.setVisible(true);
					} else if (getScrollPercentY() == 1f && down.isVisible() == true) {
						down.setVisible(false);
					}
				}

				return false;
			}
		});

		up.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setScrollY(getScrollY() - DPIUtils.getPrefButtonSize());
			}
		});

		down.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setScrollY(getScrollY() + DPIUtils.getPrefButtonSize());
			}
		});
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (visible) {
			if (getParent() != null)
				show();
		} else {
			up.remove();
			down.remove();
		}
	}

	private void show() {
		choices = ui.getWorld().getDialogOptions();

		if (choices.size() == 0)
			return;

		else if (style.autoselect && choices.size() == 1) {
			// If only has one option, autoselect it

			// To work properly, delay the selection one frame to avoid select it before
			// 'talkto' finished.
			Timer.post(new Task() {

				@Override
				public void run() {
					select(0);

					return;
				}
			});
		}

		panel.clear();

		for (int i = 0; i < choices.size(); i++) {
			String str = choices.get(i);

			if (str.charAt(0) == I18N.PREFIX)
				str = I18N.getString(str.substring(1));

			TextButton ob = new TextButton(str, style.textButtonStyle);
			ob.setUserObject(i);
			panel.row();
			panel.add(ob);
			ob.getLabel().setWrap(true);
			ob.getLabel().setAlignment(Align.left);

			ob.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					int i = (Integer) event.getListenerActor().getUserObject();

					select(i);
				}
			});
		}

		panel.pack();
		setWidth(getStage().getViewport().getScreenWidth());
		setHeight(Math.min(panel.getHeight(), getStage().getViewport().getScreenHeight() / 2));

		float size = DPIUtils.getPrefButtonSize() * .8f;
		float margin = DPIUtils.getSpacing();

		getStage().addActor(up);
		up.setSize(size, size);
		up.setPosition(getX() + getWidth() - size - margin, getY() + getHeight() - margin - size);
		up.setVisible(false);

		getStage().addActor(down);
		down.setSize(size, size);
		down.setPosition(getX() + getWidth() - size - margin, getY() + margin);
		down.setVisible(false);
	}

	private void select(int i) {
		// RECORD
		if (recorder.isRecording()) {
			recorder.add(i);
		}

		ui.getWorld().selectDialogOption(i);

		setVisible(false);
	}

	/** The style for the DialogUI */
	static public class DialogUIStyle {
		/** Optional. */
		public Drawable background;

		public TextButtonStyle textButtonStyle;

		// If only one option is visible, auto select it.
		public boolean autoselect = true;

		public DialogUIStyle() {
		}

		public DialogUIStyle(DialogUIStyle style) {
			background = style.background;
			textButtonStyle = style.textButtonStyle;
			autoselect = style.autoselect;
		}
	}
}
