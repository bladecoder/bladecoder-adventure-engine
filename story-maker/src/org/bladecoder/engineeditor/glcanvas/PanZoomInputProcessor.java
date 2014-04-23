package org.bladecoder.engineeditor.glcanvas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;

public class PanZoomInputProcessor extends InputAdapter {
	private final ScnCanvas canvas;
	private final Vector2 lastTouch = new Vector2();

	public PanZoomInputProcessor(ScnCanvas canvas) {
		this.canvas = canvas;
	}

	@Override
	public boolean touchDown(int x, int y, int pointer, int button) {
		if (button != Buttons.RIGHT && !Gdx.input.isButtonPressed(Buttons.MIDDLE)) return false;

		Vector2 p = canvas.screenToWorld(x, y);
		lastTouch.set(p);
		return false;
	}

	@Override
	public boolean touchDragged(int x, int y, int pointer) {
		if (!Gdx.input.isButtonPressed(Buttons.RIGHT) && !Gdx.input.isButtonPressed(Buttons.MIDDLE)) return false;

		Vector2 p = canvas.screenToWorld(x, y);
		Vector2 delta = new Vector2(p).sub(lastTouch);
		
		canvas.translate(delta);
		
		lastTouch.set(canvas.screenToWorld(x, y));
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		canvas.zoom(amount);
		return false;
	}
}
