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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.ui.UI.InputMode;
import com.bladecoder.engine.ui.defaults.ScreenControllerHandler;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.RectangleRenderer;

public class PieMenu extends com.badlogic.gdx.scenes.scene2d.Group {

	private BitmapFont font;

	private Button lookatButton;
	private Button talktoButton;
	private Button pickupButton;

	private float x = 0, y = 0;

	private InteractiveActor iActor = null;

	private final SceneScreen sceneScreen;

	private int viewportWidth, viewportHeight;

	private final GlyphLayout layout = new GlyphLayout();

	private String desc = null;

	public PieMenu(SceneScreen scr) {
		sceneScreen = scr;
		font = scr.getUI().getSkin().getFont("desc");

		lookatButton = new AnimButton(scr.getUI().getSkin(), "pie_lookat");
		addActor(lookatButton);
		lookatButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				if (iActor != null && iActor.canInteract()) {
					sceneScreen.runVerb(iActor, "lookat", null);
				}

				hide();
			}
		});

		talktoButton = new AnimButton(scr.getUI().getSkin(), "pie_talkto");
		addActor(talktoButton);
		talktoButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				if (iActor != null && iActor.canInteract()) {
					sceneScreen.runVerb(iActor, "talkto", null);
				}

				hide();
			}
		});

		pickupButton = new AnimButton(scr.getUI().getSkin(), "pie_pickup");
		addActor(pickupButton);
		pickupButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				if (iActor != null) {
					sceneScreen.runVerb(iActor, "pickup", null);
				}

				hide();
			}
		});
	}

	@Override
	public void draw(Batch batch, float alpha) {

		// check if the interactive or visible property has change and hide it.
		if (!iActor.canInteract()) {
			hide();
			return;
		}

		super.draw(batch, alpha);

		// DRAW TARGET DESCRIPTION
		String desc = iActor.getDesc();

		if (desc != null) {
			float margin = DPIUtils.UI_SPACE;

			float textX = x - layout.width / 2;
			float textY = y - layout.height - DPIUtils.UI_SPACE;

			if (textX < 0)
				textX = 0;

			RectangleRenderer.draw(batch, textX - margin, textY - layout.height - margin, layout.width + margin * 2,
					layout.height + margin * 2, Color.BLACK);
			font.draw(batch, layout, textX, textY);
		}
	}

	public void hide() {
		setVisible(false);
		iActor = null;
	}

	public void show(InteractiveActor a, float x, float y) {
		setVisible(true);
		this.x = x;
		this.y = y;
		iActor = a;

		// DRAW TARGET DESCRIPTION
		desc = iActor.getDesc();

		if (desc != null) {

			if (desc.charAt(0) == I18N.PREFIX)
				desc = sceneScreen.getWorld().getI18N().getString(desc.substring(1));

			layout.setText(font, desc);
		}

		Actor rightButton;

		if (a.getVerb("talkto") != null) {
			talktoButton.setVisible(true);
			pickupButton.setVisible(false);
			rightButton = talktoButton;
		} else {
			talktoButton.setVisible(false);
			pickupButton.setVisible(true);
			rightButton = pickupButton;
		}

		float margin = DPIUtils.getMarginSize();

		// FITS TO SCREEN
		if (x < lookatButton.getWidth() + margin)
			this.x = lookatButton.getWidth() + margin;
		else if (x > viewportWidth - lookatButton.getWidth() - margin)
			this.x = viewportWidth - lookatButton.getWidth() - margin;

		if (y < margin)
			this.y = margin;
		else if (y > viewportHeight - lookatButton.getHeight() - margin)
			this.y = viewportHeight - lookatButton.getHeight() - margin;

		// lookatButton.setPosition(this.x - lookatButton.getWidth() - margin / 2,
		// this.y + margin);
		lookatButton.setPosition(this.x - lookatButton.getWidth() / 2, this.y - lookatButton.getHeight() / 2);
		lookatButton.addAction(
				Actions.sequence(Actions.moveTo(this.x - lookatButton.getWidth() - margin / 2, this.y + margin, .1f),
						Actions.run(new Runnable() {

							@Override
							public void run() {
								if (sceneScreen.getUI().getInputMode() == InputMode.GAMEPAD) {
									ScreenControllerHandler.cursorToActor(lookatButton);
								}
							}

						})));

		// rightButton.setPosition(this.x + margin / 2, this.y + margin);
		rightButton.setPosition(this.x - lookatButton.getWidth() / 2, this.y - lookatButton.getHeight() / 2);
		rightButton.addAction(Actions.moveTo(this.x + margin / 2, this.y + margin, .1f));

	}

	public void resize(int width, int height) {
		viewportWidth = width;
		viewportHeight = height;

		setBounds(0, 0, width, height);

		float size = DPIUtils.getPrefButtonSize();
		lookatButton.setSize(size, size);
		talktoButton.setSize(size, size);
		pickupButton.setSize(size, size);
	}
}
