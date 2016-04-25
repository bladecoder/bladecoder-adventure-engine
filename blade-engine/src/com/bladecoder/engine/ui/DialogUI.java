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

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.DPIUtils;

public class DialogUI extends ScrollPane {
	public static final String DIALOG_END_COMMAND = "dialog_end";

	private DialogUIStyle style;

	private Recorder recorder;

	private Table panel;

	public DialogUI(UI ui) {
		super(new Table(ui.getSkin()));
		panel = (Table) getWidget();
		style = ui.getSkin().get(DialogUIStyle.class);
		this.recorder = ui.getRecorder();

		if (style.background != null)
			panel.setBackground(style.background);

		panel.top().left();
		panel.pad(DPIUtils.getMarginSize());

		setVisible(false);
		panel.defaults().expandX().fillX().top().left().padBottom(DPIUtils.getSpacing());
	}

	public void show() {
		ArrayList<DialogOption> options = World.getInstance().getCurrentDialog().getVisibleOptions();

		if (options.size() == 0)
			return;

		else if (options.size() == 1) { // If only has one option,
										// autoselect it
			select(0);
			return;
		}

		panel.clear();
		setVisible(true);

		for (DialogOption o : options) {
			String str = o.getText();

			if (str.charAt(0) == I18N.PREFIX)
				str = I18N.getString(str.substring(1));

			TextButton ob = new TextButton(str, style.textButtonStyle);
			ob.setUserObject(o);
			panel.row();
			panel.add(ob);
			ob.getLabel().setWrap(true);
			ob.getLabel().setAlignment(Align.left);

			ob.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					DialogOption o = (DialogOption) event.getListenerActor().getUserObject();

					ArrayList<DialogOption> options = World.getInstance().getCurrentDialog().getVisibleOptions();

					for (int i = 0; i < options.size(); i++) {
						if (options.get(i) == o) {
							select(i);
							break;
						}
					}
				}
			});
		}
		
		panel.pack();
		setWidth(getStage().getViewport().getScreenWidth());
		setHeight(Math.min(panel.getHeight(), getStage().getHeight()/2));
	}

	public void hide() {
		setVisible(false);
	}

	private void select(int i) {
		// RECORD
		if (recorder.isRecording()) {
			recorder.add(i);
		}

		World.getInstance().selectVisibleDialogOption(i);
		
		hide();
	}

	/** The style for the DialogUI */
	static public class DialogUIStyle {
		/** Optional. */
		public Drawable background;

		public TextButtonStyle textButtonStyle;

		public DialogUIStyle() {
		}

		public DialogUIStyle(DialogUIStyle style) {
			background = style.background;
			textButtonStyle = style.textButtonStyle;
		}
	}
}
