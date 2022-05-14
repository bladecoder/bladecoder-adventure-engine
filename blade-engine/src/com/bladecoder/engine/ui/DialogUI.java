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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.UI.InputMode;
import com.bladecoder.engine.ui.defaults.ScreenControllerHandler;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.EngineLogger;

public class DialogUI extends ScrollPane {
	public static final String DIALOG_END_COMMAND = "dialog_end";

	private DialogUIStyle style;

	private Recorder recorder;

	private Table panel;

	private Button up;
	private Button down;

	private List<String> choices;
	private final World world;

	private Task postponedSelect = null;

	private UI ui;

	public DialogUI(UI ui, Recorder recorder) {
		super(new Table(ui.getSkin()), ui.getSkin());

		this.ui = ui;

		setFadeScrollBars(true);
		setOverscroll(false, false);

		up = new Button(ui.getSkin(), "dialog-up");
		down = new Button(ui.getSkin(), "dialog-down");

		panel = (Table) getActor();
		style = ui.getSkin().get(DialogUIStyle.class);
		this.recorder = recorder;
		this.world = ui.getWorld();

		if (style.background != null)
			getStyle().background = style.background;

		panel.top().left();
		panel.pad(DPIUtils.getMarginSize());

		setVisible(false);
		panel.defaults().expandX().fillX().top().left().padBottom(DPIUtils.getSpacing());

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

	public void setUpDownVisibility() {
		EngineLogger.debug(
				"setUpDownVisibility: " + isScrollY() + " maxY: " + getMaxY() + " Margin: " + DPIUtils.getMarginSize());
		if (isScrollY() && getMaxY() > DPIUtils.getMarginSize()) {

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
		} else {
			up.setVisible(false);
			down.setVisible(false);
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (visible) {
			if (getParent() != null)
				show();
		} else {
			up.setVisible(false);
			down.setVisible(false);
			up.remove();
			down.remove();
			if (getParent() != null)
				getStage().setScrollFocus(null);
		}
	}

	@Override
	public void setScrollY(float pixels) {
		super.setScrollY(pixels);
		setUpDownVisibility();
	}

	private void show() {
		choices = world.getDialogOptions();

		if (choices.size() == 0) {
			setVisible(false);
			return;
		} else if (style.autoselect && choices.size() == 1) {
			// If only has one option, autoselect it

			// To work properly, delay the selection one frame to avoid select it before
			// 'talkto' finished.

			if (postponedSelect != null)
				postponedSelect.cancel();

			postponedSelect = Timer.schedule(new Task() {

				@Override
				public void run() {
					select(0);
				}
			}, 0.01f);

			setVisible(false);
			return;
		}

		panel.clear();
		setScrollPercentY(0);

		for (int i = 0; i < choices.size(); i++) {
			String str = choices.get(i);

			if (str.charAt(0) == I18N.PREFIX)
				str = world.getI18N().getString(str.substring(1));

			TextButton ob = new TextButton(str, style.textButtonStyle);
			ob.setUserObject(i);
			panel.row();
			panel.add(ob);
			ob.getLabel().setWrap(true);
			ob.getLabel().setAlignment(Align.left);

			ob.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					int i = (Integer) event.getListenerActor().getUserObject();

					select(i);
				}
			});
		}

		setWidth(getStage().getViewport().getScreenWidth());
		setHeight(Math.min(panel.getPrefHeight(), getStage().getViewport().getScreenHeight() / 2f));

		float size = DPIUtils.getPrefButtonSize() * .8f;
		float margin = DPIUtils.getSpacing();

		getStage().addActor(up);
		up.setSize(size, size);
		up.setPosition(getX() + getWidth() - size - margin, getY() + getHeight() - margin - size);

		getStage().addActor(down);
		down.setSize(size, size);
		down.setPosition(getX() + getWidth() - size - margin, getY() + margin);

		layout();

		setUpDownVisibility();

		if (down.isVisible()) {
			down.addAction(
					Actions.repeat(3, Actions.sequence(Actions.moveBy(0, 15, .08f), Actions.moveBy(0, -15, .08f))));
		}

		if (ui.getInputMode() == InputMode.GAMEPAD) {
			ScreenControllerHandler.cursorToActor(((Table) (getChildren().get(0))).getChildren().get(0));
		}

		getStage().setScrollFocus(this);
	}

	private void select(int i) {
		// RECORD
		if (recorder.isRecording()) {
			recorder.add(i);
		}

		world.selectDialogOption(i);

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
