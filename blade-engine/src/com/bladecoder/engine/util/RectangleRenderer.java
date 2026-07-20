package com.bladecoder.engine.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;

public final class RectangleRenderer {
	private static Texture texture;

	private static Texture makePixel() {
		Texture _temp;
		Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		p.setColor(Color.WHITE);
		p.fillRectangle(0, 0, 1, 1);
		_temp = new Texture(p, true);
		p.dispose();
		return _temp;
	}

	public static void draw(Batch batch, float posX, float posY, float width, float height, Color color) {

		if (texture == null)
			texture = makePixel();

		float tmp = batch.getPackedColor();
		batch.setColor(color);
		batch.draw(texture, posX, posY, 0, 0, width, height, 1, 1, 0, 0, 0, 1, 1, false, false);

		batch.setPackedColor(tmp);
	}

	public static void dispose() {
		if (texture != null)
			texture.dispose();
		texture = null;
	}

}
