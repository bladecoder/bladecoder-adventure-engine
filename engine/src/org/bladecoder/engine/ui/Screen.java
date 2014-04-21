package org.bladecoder.engine.ui;

import org.bladecoder.engine.assets.UIAssetConsumer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

interface Screen extends TouchEventListener, UIAssetConsumer {
	public void draw(SpriteBatch batch);
	public void resize(Rectangle viewPort);
}
