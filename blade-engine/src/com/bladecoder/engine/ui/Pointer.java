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
		if (ui.getInputMode() == InputMode.TOUCHPANEL) {
			setVisible(false);
		} else {
			setVisible(true);
		}
	}

	public void hide() {
		setVisible(false);
	}
}
