package org.bladecoder.engine.model;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.assets.EngineAssetManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonBounds;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.esotericsoftware.spine.SkeletonRenderer;

public class SpriteSpineRenderer implements SpriteRenderer {
	
	private String source;
	private TextureAtlas atlas;
	private Skeleton skeleton;
	private AnimationState state;
	
	private SkeletonRenderer renderer;
	private SkeletonBounds bounds = new SkeletonBounds();
	
	public void setSource(String s) {
		source = s;
	}

	@Override
	public void update(float delta) {
		state.update(Gdx.graphics.getDeltaTime()); // Update the animation time.

		state.apply(skeleton); // Poses skeleton using current animations. This sets the bones' local SRT.
		skeleton.updateWorldTransform(); // Uses the bones' local SRT to compute their world SRT.
		
		bounds.update(skeleton, true);
	}

	@Override
	public void draw(SpriteBatch batch, float x, float y, float originX,
			float originY, float scale) {
		
		skeleton.setX(x);
		skeleton.setY(y);
		
		renderer.draw(batch, skeleton); // Draw the skeleton images.
	}

	@Override
	public float getWidth() {
		return bounds.getWidth();
	}

	@Override
	public float getHeight() {
		return bounds.getHeight();
	}

	@Override
	public FrameAnimation getCurrentFrameAnimation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void lookat(Vector2 p0, Vector2 pf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void lookat(String direction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stand() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startWalkFA(Vector2 p0, Vector2 pf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startFrameAnimation(String id, int repeatType, int count,
			ActionCallback cb) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void loadAssets() {
		EngineAssetManager.getInstance().loadAtlas(source);
	}

	@Override
	public void retrieveAssets() {
		atlas = EngineAssetManager.getInstance().getTextureAtlas(source);
		
		SkeletonJson json = new SkeletonJson(atlas);      
        SkeletonData skeletonData = json.readSkeletonData(EngineAssetManager.getInstance().getSpine(source));
        skeleton = new Skeleton(skeletonData); // Skeleton holds skeleton state (bone positions, slot attachments, etc).
		skeleton.setX(250);
		skeleton.setY(20);

		AnimationStateData stateData = new AnimationStateData(skeletonData); // Defines mixing (crossfading) between animations.
		stateData.setMix("walk", "jump", 0.2f);
		stateData.setMix("jump", "walk", 0.4f);

		state = new AnimationState(stateData); // Holds the animation state for a skeleton (current animation, time, etc).
		state.setAnimation(0, "jump", false);
		state.addAnimation(0, "walk", true, 0);
		
		renderer = new SkeletonRenderer();
		renderer.setPremultipliedAlpha(true);
	}

	@Override
	public void dispose() {
		atlas.dispose();
	}	

	@Override
	public void write(Json json) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void read(Json json, JsonValue jsonData) {
		// TODO Auto-generated method stub
		
	}	
}