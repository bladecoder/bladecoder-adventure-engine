package com.bladecoder.engine.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable;

public class AnimationDrawable extends BaseDrawable implements TransformDrawable {
	public final Animation<AtlasRegion> anim;
	private float stateTime = 0;
	private Color tint;

	public AnimationDrawable(Animation<AtlasRegion> anim) {
		this.anim = anim;
		setMinWidth(anim.getKeyFrames()[0].getRegionWidth());
		setMinHeight(anim.getKeyFrames()[0].getRegionHeight());
	}

	public AnimationDrawable(AnimationDrawable ad) {
		super(ad);
		anim = new Animation<AtlasRegion>(ad.getAnimation().getFrameDuration(), ad.getAnimation().getKeyFrames());
		anim.setPlayMode(PlayMode.LOOP);
	}

	public void act(float delta) {
		stateTime += delta;
	}

	public void reset() {
		stateTime = 0;
	}

	@Override
	public void draw(Batch batch, float x, float y, float width, float height) {
		if (tint != null)
			batch.setColor(tint);

		batch.draw(anim.getKeyFrame(stateTime), x, y, width, height);

		if (tint != null)
			batch.setColor(Color.WHITE);
	}

	@Override
	public void draw(Batch batch, float x, float y, float originX, float originY, float width, float height,
			float scaleX, float scaleY, float rotation) {

		if (tint != null)
			batch.setColor(tint);

		batch.draw(anim.getKeyFrame(stateTime), x, y, originX, originY, width, height, scaleX, scaleY, rotation);

		if (tint != null)
			batch.setColor(Color.WHITE);
	}

	public Animation<AtlasRegion> getAnimation() {
		return anim;
	}

	public Drawable tint(Color tint) {
		AnimationDrawable d = new AnimationDrawable(this);
		d.setTint(tint);

		return d;
	}

	public void setTint(Color t) {
		this.tint = t;
	}
}
