package org.bladecoder.engineeditor.glcanvas;

import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.anim.AtlasFrameAnimation;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.RectangleRenderer;

import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenAccessor;
import aurelienribon.tweenengine.TweenManager;
import aurelienribon.tweenengine.equations.Linear;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;

/**
 * Frame Animation renderer. Independent from the EngineTweenManager
 * 
 * @author rgarcia
 */
public class AtlasFARenderer {
	
	static {
		Tween.registerAccessor(FARenderer.class, new Accessor());
	}
	
	public static final Color BG_COLOR = Color.MAGENTA;
	private static final TweenManager tweenManager = new TweenManager();
	private static final float HEIGHT = 200;

	AtlasFrameAnimation currentFrameAnimation;
	int currentFrame = 0;

	public void setFrameAnimation(AtlasFrameAnimation fa) {
		this.currentFrameAnimation = fa;
		
		play();
	}

	public void draw(SpriteBatch batch) {
		tweenManager.update(Gdx.graphics.getDeltaTime());
		
		if(currentFrameAnimation == null) return;
		
		if (currentFrameAnimation.regions == null) {
			RectangleRenderer.draw(batch, 0, 0, 100, 100, Color.RED);
			return;
		}

		if (currentFrame >= currentFrameAnimation.regions.size) {
			EngineLogger.error("Current Frame: " + currentFrame + " in FA: "
					+ currentFrameAnimation.id + " greather than num frames: "
					+ currentFrameAnimation.regions.size);
			currentFrame = currentFrameAnimation.regions.size - 1;
		}

		AtlasRegion r = currentFrameAnimation.regions.get(currentFrame);
		
		float screenWidth = Gdx.graphics.getWidth();
		float screenHeight = Gdx.graphics.getHeight();
		
		float width =  HEIGHT / r.getRegionHeight() * r.getRegionWidth();

		RectangleRenderer.draw(batch, screenWidth - width - 5, screenHeight - HEIGHT - 55, width + 10, HEIGHT + 10, Color.BLACK);
		RectangleRenderer.draw(batch, screenWidth - width, screenHeight - HEIGHT - 50, width, HEIGHT, BG_COLOR);
		
		batch.draw(r, screenWidth - width, screenHeight - HEIGHT - 50, width, HEIGHT);
	}
	
	public void play() {
		tweenManager.killTarget(this, Accessor.FRAME);
		currentFrame = 0;
		
		if(currentFrameAnimation == null || currentFrameAnimation.regions == null || getNumFrames() <= 1) return;

		Tween t = Tween.to(this, Accessor.FRAME, currentFrameAnimation.duration);
		t.target(getNumFrames() - 1);
		
		t.ease(Linear.INOUT); // TODO variable

		switch (currentFrameAnimation.animationType) {
		case EngineTween.REPEAT:
			t.repeat(currentFrameAnimation.count, currentFrameAnimation.delay);
			break;
		case EngineTween.YOYO:
			t.repeatYoyo(currentFrameAnimation.count, currentFrameAnimation.delay);
			break;

		}

		t.start(tweenManager);
	}
	
	public int getNumFrames() {
		return currentFrameAnimation.regions.size;
	}
	
	// -------------------------------------------------------------------------
	// Tween Accessor
	// -------------------------------------------------------------------------

	private static class Accessor implements TweenAccessor<AtlasFARenderer> {
		public static final int FRAME = 1;

		@Override
		public int getValues(AtlasFARenderer target, int tweenType, float[] returnValues) {
			switch (tweenType) {
				case FRAME: returnValues[0] = target.currentFrame; return 1;
				default: assert false; return -1;
			}
		}

		@Override
		public void setValues(AtlasFARenderer target, int tweenType, float[] newValues) {
			switch (tweenType) {
				case FRAME: target.currentFrame = (int)newValues[0]; break;
				default: assert false;
			}
		}
	};	

}
