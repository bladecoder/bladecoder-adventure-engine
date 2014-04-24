package org.bladecoder.engineeditor.glcanvas;

import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.SpriteActor.DepthType;
import org.bladecoder.engine.util.RectangleRenderer;
import org.bladecoder.engineeditor.model.SceneDocument;
import org.w3c.dom.Element;

import aurelienribon.tweenengine.Tween;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class FARenderer {

	public static final Color BG_COLOR = Color.MAGENTA;
	private static final float HEIGHT = 200;

	SpriteActor actor;
	String fa;

	public void setFrameAnimation(SceneDocument doc, Element a, String fa) {

		if (a == null || fa == null) {
			actor = null;
			this.fa = null;
			return;
		}

		if (this.actor == null || !a.getAttribute("id").equals(actor.getId())) {
			this.actor = (SpriteActor) doc.getEngineActor(a);
			this.actor.setDepthType(DepthType.NONE);
		}

		this.fa = fa;

		play();
	}

	public void update(float delta) {
		if (fa == null)
			return;

		if (actor instanceof SpriteActor)
			actor.update(delta);
	}

	public void draw(SpriteBatch batch) {

		if (fa == null)
			return;

		float screenWidth = Gdx.graphics.getWidth();
		float screenHeight = Gdx.graphics.getHeight();
		
		float actorWidth = actor.getWidth() * actor.getScale();
		float actorHeight = actor.getHeight() * actor.getScale();
		
		float width = HEIGHT / actorHeight * actorWidth;

		RectangleRenderer.draw(batch, screenWidth - width - 5, screenHeight
				- HEIGHT - 55, width + 10, HEIGHT + 10, Color.BLACK);
		RectangleRenderer.draw(batch, screenWidth - width, screenHeight
				- HEIGHT - 50, width, HEIGHT, BG_COLOR);

		actor.getRenderer().draw(batch, screenWidth - width, screenHeight - HEIGHT - 50, 0, 0, width / actorWidth);
	}

	public void play() {
		actor.retrieveAssets();
		actor.startFrameAnimation(fa, EngineTween.REPEAT, Tween.INFINITY, null);
	}
}
