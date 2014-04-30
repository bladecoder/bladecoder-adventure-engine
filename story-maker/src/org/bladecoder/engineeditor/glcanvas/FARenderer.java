package org.bladecoder.engineeditor.glcanvas;

import org.bladecoder.engine.anim.AtlasFrameAnimation;
import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.model.Sprite3DRenderer;
import org.bladecoder.engine.model.SpriteAtlasRenderer;
import org.bladecoder.engine.model.SpriteRenderer;
import org.bladecoder.engine.model.SpriteSpineRenderer;
import org.bladecoder.engine.util.RectangleRenderer;
import org.bladecoder.engineeditor.model.SceneDocument;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Frame Animation renderer. 
 * 
 * @author rgarcia
 */
public class FARenderer {

	public static final Color BG_COLOR = Color.MAGENTA;
	private static final float HEIGHT = 200;

	AtlasFrameAnimation currentFrameAnimation;
	private SpriteRenderer renderer;

	public void setType(String type) {
		if (renderer != null) {
			renderer.dispose();
			renderer = null;
		}

		if (type.equals(SceneDocument.SPRITE3D_ACTOR_TYPE)) {
			renderer = new Sprite3DRenderer();
		} else if (type.equals(SceneDocument.SPRITE3D_ACTOR_TYPE)) {
			renderer = new SpriteSpineRenderer();
		} else {
			renderer = new SpriteAtlasRenderer();
		}
	}

	public void setFrameAnimation(AtlasFrameAnimation fa) {

		if (renderer != null) {

			renderer.addFrameAnimation(fa);

			renderer.retrieveAssets();

			renderer.startFrameAnimation(fa.id, EngineTween.FROM_FA, 1, null);
		}
	}

	public void draw(SpriteBatch batch) {
		if (currentFrameAnimation == null)
			return;

		if (currentFrameAnimation.regions == null) {
			RectangleRenderer.draw(batch, 0, 0, 100, 100, Color.RED);
			return;
		}

		float screenWidth = Gdx.graphics.getWidth();
		float screenHeight = Gdx.graphics.getHeight();

		float width = HEIGHT / renderer.getHeight() * renderer.getWidth();

		RectangleRenderer.draw(batch, screenWidth - width - 5, screenHeight
				- HEIGHT - 55, width + 10, HEIGHT + 10, Color.BLACK);
		RectangleRenderer.draw(batch, screenWidth - width, screenHeight
				- HEIGHT - 50, width, HEIGHT, BG_COLOR);
		
		float scaleh =   Gdx.graphics.getHeight() /  renderer.getHeight();
		renderer.draw(batch, screenWidth - width, screenHeight - HEIGHT - 50, 0, 0, scaleh);
	}

}
