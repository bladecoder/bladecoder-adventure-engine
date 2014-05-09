package org.bladecoder.engineeditor.glcanvas;

import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.anim.Tween;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Sprite3DRenderer;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.AtlasRenderer;
import org.bladecoder.engine.model.SpriteRenderer;
import org.bladecoder.engine.model.SpineRenderer;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

/**
 * Frame Animation renderer.
 * 
 * @author rgarcia
 */
public class FARenderer {

	public static final Color BG_COLOR = Color.MAGENTA;
	private static final float HEIGHT = 200;

	FrameAnimation currentFrameAnimation;
	private SpriteRenderer renderer;

	public void setActor(Actor a) {
		if (renderer != null) {
			renderer.dispose();
			renderer = null;
		}

		if (a instanceof SpriteActor) {
			SpriteRenderer r = ((SpriteActor) a).getRenderer();

			if (r instanceof Sprite3DRenderer) {
				renderer = new Sprite3DRenderer();
				((Sprite3DRenderer)renderer).setSpriteSize(new Vector2( r.getWidth(), r.getHeight()));
			} else if (r instanceof SpineRenderer) {
				renderer = new SpineRenderer();
			} else {
				renderer = new AtlasRenderer();
			}
		}
	}

	public void setFrameAnimation(FrameAnimation fa) {
		currentFrameAnimation = fa;

		if (renderer != null && fa != null) {

			renderer.getFrameAnimations().clear();
			
			renderer.addFrameAnimation(fa);

			renderer.retrieveAssets();

			renderer.startFrameAnimation(fa.id, Tween.FROM_FA, 1, null);
		}
	}

	public void draw(SpriteBatch batch) {
		if (renderer != null && currentFrameAnimation != null) {

			float screenWidth = Gdx.graphics.getWidth();
			float screenHeight = Gdx.graphics.getHeight();

			float width = HEIGHT / renderer.getHeight() * renderer.getWidth();

			RectangleRenderer.draw(batch, screenWidth - width - 5, screenHeight
					- HEIGHT - 55, width + 10, HEIGHT + 10, Color.BLACK);
			RectangleRenderer.draw(batch, screenWidth - width, screenHeight
					- HEIGHT - 50, width, HEIGHT, BG_COLOR);

			float scaleh = width / renderer.getWidth();
			renderer.draw(batch, screenWidth - width/2, screenHeight - HEIGHT
					- 50, 0, 0, scaleh);
		}
	}

	public void update(float delta) {
		if (renderer != null && currentFrameAnimation != null) {
			renderer.update(delta);
		}
	}

}
