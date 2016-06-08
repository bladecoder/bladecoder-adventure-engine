package com.bladecoder.engine.ui.retro;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.bladecoder.engine.model.ActorRenderer;

public class RendererDrawable extends BaseDrawable {

	private ActorRenderer renderer;

	public void setRenderer(ActorRenderer r) {
		renderer = r;
		
		if (r != null) {
			setMinWidth(renderer.getWidth());
			setMinHeight(renderer.getHeight());
		}
	}

	@Override
	public void draw(Batch batch, float x, float y, float width, float height) {
		if (renderer == null)
			return;

		float scale;

		if (renderer.getWidth() > renderer.getHeight())
			scale = width / renderer.getWidth();
		else
			scale = height / renderer.getHeight();

		renderer.draw((SpriteBatch) batch, x + renderer.getWidth() * scale  / 2, y, scale, null);
	}
}
