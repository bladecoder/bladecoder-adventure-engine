package org.bladecoder.engine.model;

import java.util.HashMap;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.assets.AssetConsumer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json.Serializable;

public interface SpriteRenderer extends Serializable, AssetConsumer {

	public void update(float delta);
	public void draw(SpriteBatch batch, float x, float y, float originX,
			float originY, float scale);
	
	public float getWidth();
	public float getHeight();
	
	public FrameAnimation getCurrentFrameAnimation();
	public String getCurrentFrameAnimationId();
	
	public void lookat(Vector2 p0, Vector2 pf);
	public void lookat(String direction);
	public void stand();
	public void startWalkFA(Vector2 p0, Vector2 pf);
	public void startFrameAnimation(String id, int repeatType,
			int count, ActionCallback cb);
	
	
	public void addFrameAnimation(FrameAnimation fa);
	public void setInitFrameAnimation(String fa);
	public String getInitFrameAnimation();
	
	public String[] getInternalAnimations(String source);
	public HashMap<String, FrameAnimation> getFrameAnimations();
}

