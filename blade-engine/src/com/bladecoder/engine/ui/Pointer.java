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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.ui.UI.InputMode;
import com.bladecoder.engine.util.DPIUtils;

/**
 * WARNING!!! This is a *struct* in C# so we have to use assign instead of '='
 * operator. And always create the object, null value is not allowed.
 * 
 */
public class Pointer extends Actor {
	private static final String POINTER_ICON = "pointer";

	private Drawable pointerIcon;

	private final Vector2 mousepos = new Vector2();

	private float pointerScale;

	private UI ui;

	public Pointer(UI ui) {
		this.ui = ui;
		pointerIcon = ui.getSkin().getDrawable(POINTER_ICON);
		setTouchable(Touchable.disabled);

		resize();
		show();
	}

	private void getInputUnproject(Viewport v, Vector2 out) {
		out.set(Gdx.input.getX(), Gdx.input.getY());

		v.unproject(out);
	}

	@Override
	public void act(float delta) {

		if (pointerIcon instanceof AnimationDrawable)
			((AnimationDrawable) pointerIcon).act(delta);

		super.act(delta);

		if (getStage().getActors().get(getStage().getActors().size - 1) != this)
			toFront();
	}

	@Override
	public void draw(Batch batch, float alpha) {

		getInputUnproject(getStage().getViewport(), mousepos);

		setPosition(mousepos.x - getWidth() / 2, mousepos.y - getHeight() / 2);

		pointerIcon.draw(batch, getX(), getY(), getWidth(), getHeight());
	}

	public void resize() {
		pointerScale = DPIUtils.getTouchMinSize() / pointerIcon.getMinHeight() * .8f;
		setSize(pointerIcon.getMinWidth() * pointerScale, pointerIcon.getMinHeight() * pointerScale);
	}

	public void show() {
		if (!(ui.getInputMode() == InputMode.TOUCHPANEL)) {
			setVisible(true);
		} else {
			setVisible(false);
		}
	}

	public void hide() {
		setVisible(false);
	}
}
