package org.bladecoder.engine.model;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.assets.AssetConsumer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json.Serializable;

public interface SpriteRenderer extends Serializable, AssetConsumer {
	public final static String BACK = "back";
	public final static String FRONT = "front";
	public final static String RIGHT = "right";
	public final static String LEFT = "left";
	public final static String BACKRIGHT = "backright";
	public final static String BACKLEFT = "backleft";
	public final static String FRONTRIGHT = "frontright";
	public final static String FRONTLEFT = "frontleft";
	public final static String STAND_ANIM = "stand";
	public final static String WALK_ANIM = "walk";
	public final static String TALK_ANIM = "talk";
	
	
	public void update(float delta);
	public void draw(SpriteBatch batch, float x, float y, float originX,
			float originY, float scale);
	
	public float getWidth();
	public float getHeight();
	
	public FrameAnimation getCurrentFrameAnimation();
	public void lookat(Vector2 p0, Vector2 pf);
	public void lookat(String direction);
	public void stand();
	public void startWalkFA(Vector2 p0, Vector2 pf);
	public void startFrameAnimation(String id, int repeatType,
			int count, ActionCallback cb);

}

